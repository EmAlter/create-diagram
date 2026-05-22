package com.emalter.creatediagram.logic;

import com.emalter.creatediagram.component.RecipeOutput;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine responsible for selecting and scoring EMI recipes based on provided inputs and machine categories.
 * It exposes methods to compute possible outputs, select alternative targets, and evaluate catalysts and batches.
 */
public class RecipeEngine {

    public List<RecipeOutput> getOutputs(String machineId, String property, Map<String, Integer> inputs) {
        if (inputs.isEmpty()) return List.of();

        List<EmiRecipeCategory> validCategories = getCategoriesForMachine(machineId);
        List<EmiRecipe> validRecipes = new ArrayList<>();

        for (EmiRecipeCategory category : validCategories) {
            for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                if (calculateBatches(recipe.getInputs(), inputs) > 0 && matchesCatalysts(recipe, property)) {
                    validRecipes.add(recipe);
                }
            }
        }

        if (validRecipes.isEmpty()) return List.of();

        validRecipes.sort((r1, r2) -> Integer.compare(calculateRecipeScore(r2.getInputs()), calculateRecipeScore(r1.getInputs())));

        EmiRecipe selectedRecipe = validRecipes.getFirst();
        String targetId = getTargetFromProperty(property);

        if (targetId != null) {
            for (EmiRecipe r : validRecipes) {
                if (hasOutput(r, targetId)) {
                    selectedRecipe = r;
                    break;
                }
            }
        }

        int batches = calculateBatches(selectedRecipe.getInputs(), inputs);
        List<RecipeOutput> results = new ArrayList<>();

        List<EmiStack> emiOutputs = selectedRecipe.getOutputs();
        for (int i = 0; i < emiOutputs.size(); i++) {
            EmiStack output = emiOutputs.get(i);
            int finalAmount = (int) (output.getAmount() * batches);
            float rawChance = output.getChance();

            // --- INIZIO FIX: RECUPERO PERCENTUALI ORIGINALI DINAMICO ---
            try {
                Object backing = selectedRecipe.getBackingRecipe();
                if (backing != null) {
                    Object recipeVal = backing;
                    if (backing instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
                        recipeVal = holder.value();
                    }

                    // Cerca il metodo getRollableResults ciclando tutti i metodi, aggirando il nome della classe
                    java.lang.reflect.Method getRollableResults = null;
                    for (java.lang.reflect.Method m : recipeVal.getClass().getMethods()) {
                        if (m.getName().equals("getRollableResults")) {
                            getRollableResults = m;
                            break;
                        }
                    }

                    if (getRollableResults != null) {
                        java.util.List<?> rollableResults = (java.util.List<?>) getRollableResults.invoke(recipeVal);

                        if (i < rollableResults.size()) {
                            Object rollable = rollableResults.get(i);
                            float realChance = -1f;

                            // Prova con il metodo getter (vecchie versioni)
                            try {
                                java.lang.reflect.Method getChanceMethod = rollable.getClass().getMethod("getChance");
                                realChance = (float) getChanceMethod.invoke(rollable);
                            } catch (NoSuchMethodException e) {
                                // Prova leggendo direttamente il campo (nuove versioni NeoForge)
                                try {
                                    java.lang.reflect.Field chanceField = rollable.getClass().getDeclaredField("chance");
                                    chanceField.setAccessible(true);
                                    realChance = chanceField.getFloat(rollable);
                                } catch (Exception ex) {
                                    // Ignora silenziosamente
                                }
                            }

                            if (realChance >= 0f) {
                                rawChance = realChance;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback pulito a EMI se la reflection fallisce
            }
            // --- FINE FIX ---

            if (rawChance > 1.0f) rawChance = rawChance / 100.0f;
            if (rawChance < 0f) rawChance = 0f;
            if (rawChance > 1f) rawChance = 1f;

            results.add(new RecipeOutput(output.getId().toString(), rawChance, finalAmount));
        }
        return results;
    }

    private int calculateRecipeScore(List<EmiIngredient> inputs) {
        int score = 0;
        for (EmiIngredient req : inputs) {
            if (!req.isEmpty()) score += (int) req.getAmount();
        }
        return score;
    }

    public String getNextAlternativeTarget(String machineId, String property, Map<String, Integer> inputs) {
        List<EmiRecipeCategory> validCategories = getCategoriesForMachine(machineId);
        List<EmiRecipe> validRecipes = new ArrayList<>();

        for (EmiRecipeCategory category : validCategories) {
            for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                if (calculateBatches(recipe.getInputs(), inputs) > 0 && matchesCatalysts(recipe, property)) {
                    validRecipes.add(recipe);
                }
            }
        }

        if (validRecipes.size() <= 1) return null;
        validRecipes.sort((r1, r2) -> Integer.compare(calculateRecipeScore(r2.getInputs()), calculateRecipeScore(r1.getInputs())));

        int currentIndex = 0;
        String currentTarget = getTargetFromProperty(property);

        if (currentTarget != null) {
            for (int i = 0; i < validRecipes.size(); i++) {
                if (hasOutput(validRecipes.get(i), currentTarget)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = (currentIndex + 1) % validRecipes.size();
        EmiRecipe nextRecipe = validRecipes.get(nextIndex);
        String nextTargetId = nextRecipe.getOutputs().isEmpty() ? "" : nextRecipe.getOutputs().getFirst().getId().toString();

        StringBuilder baseProperty = new StringBuilder();
        if (property != null) {
            for (String part : property.split(";")) {
                if (!part.startsWith("target:") && !part.isEmpty()) baseProperty.append(part).append(";");
            }
        }
        return baseProperty.append("target:").append(nextTargetId).toString();
    }

    private boolean hasOutput(EmiRecipe recipe, String itemId) {
        for (EmiStack out : recipe.getOutputs()) {
            if (out.getId().toString().equals(itemId)) return true;
        }
        return false;
    }

    private String getTargetFromProperty(String property) {
        if (property != null && property.contains("target:")) {
            for (String part : property.split(";")) {
                if (part.startsWith("target:")) return part.substring(7);
            }
        }
        return null;
    }

    private List<EmiRecipeCategory> getCategoriesForMachine(String machineId) {
        List<EmiRecipeCategory> cats = new ArrayList<>();
        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            for (EmiIngredient workstation : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : workstation.getEmiStacks()) {
                    if (stack.getId().toString().equals(machineId)) {
                        cats.add(category);
                        break;
                    }
                }
            }
        }
        return cats;
    }

    private boolean matchesCatalysts(EmiRecipe recipe, String nodeProperty) {
        String catalyst = "";
        if (nodeProperty != null) {
            for (String part : nodeProperty.split(";")) {
                if (!part.startsWith("target:")) catalyst = part;
            }
        }
        String safeProp = catalyst;
        String categoryPath = recipe.getCategory().getId().getPath().toLowerCase();

        if (categoryPath.contains("fan_washing") || categoryPath.contains("splashing")) return safeProp.equals("minecraft:water");
        if (categoryPath.contains("fan_blasting") || categoryPath.contains("blasting")) return safeProp.equals("minecraft:lava");
        if (categoryPath.contains("fan_smoking") || categoryPath.contains("smoking")) return safeProp.equals("minecraft:campfire");
        if (categoryPath.contains("fan_haunting") || categoryPath.contains("haunting")) return safeProp.equals("minecraft:soul_campfire");

        int userHeat = 0;
        if (safeProp.equals("create:blaze_burner")) userHeat = 1;
        else if (safeProp.equals("create:blaze_cake")) userHeat = 2;

        EmiHelper.HeatLevel recipeHeat = EmiHelper.getRecipeHeatLevel(recipe);
        if (recipeHeat != EmiHelper.HeatLevel.NONE) return userHeat >= recipeHeat.ordinal();
        if (recipe.getCatalysts().isEmpty()) return true;

        for (EmiIngredient cat : recipe.getCatalysts()) {
            for (EmiStack stack : cat.getEmiStacks()) {
                if (stack.getId().toString().equals(safeProp)) return true;
            }
        }
        return false;
    }

    private int calculateBatches(List<EmiIngredient> recipeInputs, Map<String, Integer> userInputs) {
        if (recipeInputs.isEmpty()) {
            return userInputs.isEmpty() ? 1 : 0;
        }

        Map<String, Long> aggregatedRequirements = new HashMap<>();
        Set<String> usedUserInputs = new HashSet<>();

        for (EmiIngredient req : recipeInputs) {
            if (req.isEmpty()) continue;

            String matchedId = null;
            for (EmiStack validStack : req.getEmiStacks()) {
                String validId = validStack.getId().toString();
                if (userInputs.containsKey(validId)) {
                    matchedId = validId;
                    break;
                }
            }

            if (matchedId == null) return 0;

            usedUserInputs.add(matchedId);
            aggregatedRequirements.put(matchedId, aggregatedRequirements.getOrDefault(matchedId, 0L) + req.getAmount());
        }

        if (usedUserInputs.size() < userInputs.size()) {
            return 0;
        }

        int maxBatches = Integer.MAX_VALUE;
        for (Map.Entry<String, Long> entry : aggregatedRequirements.entrySet()) {
            String id = entry.getKey();
            long requiredTotal = entry.getValue();
            int provided = userInputs.getOrDefault(id, 0);

            int batchesForThisItem = (int) (provided / requiredTotal);
            if (batchesForThisItem == 0) return 0;

            maxBatches = Math.min(maxBatches, batchesForThisItem);
        }

        return maxBatches == Integer.MAX_VALUE ? 1 : maxBatches;
    }
}