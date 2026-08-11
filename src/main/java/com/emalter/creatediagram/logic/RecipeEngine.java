package com.emalter.creatediagram.logic;

import com.emalter.creatediagram.component.OutputPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class RecipeEngine {

    public List<OutputPort> getOutputs(String machineId, String property, Map<String, Integer> inputs) {
        if (inputs.isEmpty()) return List.of();

        List<EmiRecipeCategory> validCategories = EmiHelper.getEmiCategories(new ArrayList<>(), machineId);
        List<EmiRecipe> validRecipes = new ArrayList<>();

        for (EmiRecipeCategory category : validCategories) {
            for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                if (calculateBatches(recipe, inputs).batches() > 0 && matchesHeat(recipe, property)) {
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

        // CORRETTO: Estraiamo sia batch che type in un'unica chiamata passando 'selectedRecipe'
        BatchResult result = calculateBatches(selectedRecipe, inputs);
        int batches = result.batches();
        RecipeType recipeType = result.type();

        List<OutputPort> results = new ArrayList<>();
        List<EmiStack> emiOutputs = selectedRecipe.getOutputs();

        String processName = "Unknown Process";
        try {
            processName = selectedRecipe.getCategory().getName().getString();
        } catch (Exception e) {
            processName = selectedRecipe.getCategory().getId().getPath();
        }

        for (int i = 0; i < emiOutputs.size(); i++) {
            EmiStack output = emiOutputs.get(i);
            int finalAmount = (int) (output.getAmount() * batches);
            float rawChance = output.getChance();

            try {
                Object backing = selectedRecipe.getBackingRecipe();
                if (backing != null) {
                    Object recipeVal = backing;
                    if (backing instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
                        recipeVal = holder.value();
                    }

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
                            float realChance = getRealChance(rollableResults, i);
                            if (realChance >= 0f) rawChance = realChance;
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (rawChance > 1.0f) rawChance = rawChance / 100.0f;
            if (rawChance < 0f) rawChance = 0f;
            if (rawChance > 1f) rawChance = 1f;

            results.add(new OutputPort(output.getId().toString(), rawChance, finalAmount, processName, recipeType));
        }
        return results;
    }

    private static float getRealChance(List<?> rollableResults, int i) throws IllegalAccessException, InvocationTargetException {
        Object rollable = rollableResults.get(i);
        float realChance = -1f;

        try {
            java.lang.reflect.Method getChanceMethod = rollable.getClass().getMethod("getChance");
            realChance = (float) getChanceMethod.invoke(rollable);
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Field chanceField = rollable.getClass().getDeclaredField("chance");
                chanceField.setAccessible(true);
                realChance = chanceField.getFloat(rollable);
            } catch (Exception ignored) {}
        }
        return realChance;
    }

    private int calculateRecipeScore(List<EmiIngredient> inputs) {
        int score = 0;
        for (EmiIngredient req : inputs) {
            if (!req.isEmpty()) score += (int) req.getAmount();
        }
        return score;
    }

    public String getNextAlternativeTarget(String machineId, String property, Map<String, Integer> inputs) {
        List<EmiRecipeCategory> validCategories = EmiHelper.getEmiCategories(new ArrayList<>(), machineId);
        List<EmiRecipe> validRecipes = new ArrayList<>();

        for (EmiRecipeCategory category : validCategories) {
            for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                if (calculateBatches(recipe, inputs).batches() > 0 && matchesHeat(recipe, property)) {
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

    private boolean matchesHeat(EmiRecipe recipe, String nodeProperty) {
        String catalyst = "";
        if (nodeProperty != null) {
            for (String part : nodeProperty.split(";")) {
                if (!part.startsWith("target:")) catalyst = part;
            }
        }
        String safeProp = catalyst;

        int userHeat = 0;
        if (safeProp.equals("create:blaze_burner")) userHeat = 1;
        else if (safeProp.equals("create:blaze_cake")) userHeat = 2;

        EmiHelper.HeatLevel recipeHeat = EmiHelper.getRecipeHeatLevel(recipe);
        if (recipeHeat != EmiHelper.HeatLevel.NONE) {
            return userHeat >= recipeHeat.ordinal();
        }

        return true;
    }

    private BatchResult calculateBatches(EmiRecipe recipe, Map<String, Integer> userInputs) {
        List<EmiIngredient> recipeInputs = new ArrayList<>(recipe.getInputs());
        recipeInputs.addAll(recipe.getCatalysts());

        if (recipeInputs.isEmpty()) {
            return new BatchResult(userInputs.isEmpty() ? 1 : 0, RecipeType.INFINITE, List.of());
        }

        String categoryId = recipe.getCategory().getId().toString();
        boolean isForcedInfinite = categoryId.contains("extruding") || categoryId.contains("create_mechanical_extruder");

        Map<String, Long> aggregatedRequirements = new HashMap<>();
        Set<String> usedUserInputs = new HashSet<>();
        List<String> infiniteInputs = new ArrayList<>();

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

            if (matchedId == null) return new BatchResult(0, RecipeType.STANDARD, List.of());

            usedUserInputs.add(matchedId);

            boolean isCatalyst = recipe.getCatalysts().contains(req) || isForcedInfinite;
            boolean isInherentlyInfinite = (req.getAmount() == 0 || isCatalyst);

            if (isInherentlyInfinite) {
                aggregatedRequirements.putIfAbsent(matchedId, 0L);
                if (!infiniteInputs.contains(matchedId)) infiniteInputs.add(matchedId);
            } else {
                aggregatedRequirements.put(matchedId, aggregatedRequirements.getOrDefault(matchedId, 0L) + req.getAmount());
            }
        }

        if (usedUserInputs.size() < userInputs.size()) return new BatchResult(0, RecipeType.STANDARD, List.of());

        int maxBatches = Integer.MAX_VALUE;
        boolean allReqsInfinite = true;
        boolean someReqsInfinite = false;

        for (Map.Entry<String, Long> entry : aggregatedRequirements.entrySet()) {
            String id = entry.getKey();
            long requiredTotal = entry.getValue();
            int provided = userInputs.getOrDefault(id, 0);

            // Check if the requirement is satisfied infinitely (either inherently infinite or catalyst)
            boolean isSatisfiedInfinitely = (requiredTotal <= 0) || (provided == Integer.MAX_VALUE);

            if (isSatisfiedInfinitely) {
                someReqsInfinite = true;
                if (!infiniteInputs.contains(id)) infiniteInputs.add(id);
            } else {
                allReqsInfinite = false;
            }

            if (requiredTotal <= 0) {
                if (provided < 1) return new BatchResult(0, RecipeType.STANDARD, List.of());
                continue; // It is a catalyst or inherently infinite requirement, so we don't limit the batches based on this
            }

            if (provided == Integer.MAX_VALUE) {
                continue; // The provided amount is infinite, so we can produce unlimited batches for this item
            }

            int batchesForThisItem = (int) (provided / requiredTotal);
            if (batchesForThisItem == 0) return new BatchResult(0, RecipeType.STANDARD, List.of());

            maxBatches = Math.min(maxBatches, batchesForThisItem);
        }
        
        RecipeType finalType;
        if (allReqsInfinite) finalType = RecipeType.INFINITE;
        else if (someReqsInfinite) finalType = RecipeType.MIXED;
        else finalType = RecipeType.STANDARD;

        int finalBatches = maxBatches == Integer.MAX_VALUE ? 1 : maxBatches;
        return new BatchResult(finalBatches, finalType, infiniteInputs);
    }

    public List<String> getInfiniteInputs(String machineId, String property, Map<String, Integer> inputs) {
        if (inputs.isEmpty()) return List.of();

        List<EmiRecipeCategory> validCategories = EmiHelper.getEmiCategories(new ArrayList<>(), machineId);
        List<EmiRecipe> validRecipes = new ArrayList<>();

        for (EmiRecipeCategory category : validCategories) {
            for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                if (calculateBatches(recipe, inputs).batches() > 0 && matchesHeat(recipe, property)) {
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

        // Restituisce la lista degli ID estratti dal record BatchResult
        return calculateBatches(selectedRecipe, inputs).infiniteInputs();
    }
}