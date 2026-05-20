package com.emalter.creatediagram.logic;

import com.emalter.creatediagram.view.widget.ConnectionManager.RecipeOutput;
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

    /**
     * Returns the list of recipe outputs for the best matching recipe given a machine ID, node property and available inputs.
     * @param machineId identifier of the machine/workstation
     * @param property  node-specific property string used to influence recipe selection (e.g. catalysts or target output)
     * @param inputs    map of available input item IDs to their quantities
     * @return list of RecipeOutput objects representing resulting item ids, chances and amounts
     */
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

        EmiRecipe selectedRecipe = validRecipes.get(0);
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

        for (EmiStack output : selectedRecipe.getOutputs()) {
            int finalAmount = (int) (output.getAmount() * batches);
            results.add(new RecipeOutput(output.getId().toString(), output.getChance(), finalAmount));
        }
        return results;
    }

    /**
     * Computes a simple score for a recipe by summing the required amounts of non-empty ingredients.
     * Higher score means a heavier/rarer recipe and is used for sorting.
     */
    private int calculateRecipeScore(List<EmiIngredient> inputs) {
        int score = 0;
        for (EmiIngredient req : inputs) {
            if (!req.isEmpty()) score += req.getAmount();
        }
        return score;
    }

    /**
     * Cycles to the next available target output for the given machine/property and inputs.
     * Returns an updated property string containing the new target id or null if no alternative exists.
     */
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
        String nextTargetId = nextRecipe.getOutputs().isEmpty() ? "" : nextRecipe.getOutputs().get(0).getId().toString();

        StringBuilder baseProperty = new StringBuilder();
        if (property != null) {
            for (String part : property.split(";")) {
                if (!part.startsWith("target:") && !part.isEmpty()) baseProperty.append(part).append(";");
            }
        }
        return baseProperty.toString() + "target:" + nextTargetId;
    }

    private boolean hasOutput(EmiRecipe recipe, String itemId) {
        for (EmiStack out : recipe.getOutputs()) {
            if (out.getId().toString().equals(itemId)) return true;
        }
        return false;
    }

    /**
     * Checks whether the recipe produces an output with the specified item id.
     */

    private String getTargetFromProperty(String property) {
        if (property != null && property.contains("target:")) {
            for (String part : property.split(";")) {
                if (part.startsWith("target:")) return part.substring(7);
            }
        }
        return null;
    }

    /**
     * Extracts a 'target:' value from a semicolon-delimited property string if present.
     */

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

    /**
     * Finds recipe categories associated with the provided machine/workstation id.
     */

    /**
     * Checks whether the provided recipe is compatible with the catalyst/property encoded in the nodeProperty string.
     * Supports special cases for washing/blasting/smoking/haunting categories and recipes that require heat levels.
     * @param recipe the EMI recipe to validate
     * @param nodeProperty semicolon-delimited property string that may contain a catalyst or a 'target:' entry
     * @return true if the recipe can run with the given catalyst/property
     */
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

        int recipeHeat = 0;
        boolean hasHeatRequirement = false;
        Object rawRecipe = null;

        try {
            for (java.lang.reflect.Field f : recipe.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(recipe);
                if (val != null) {
                    String className = val.getClass().getName().toLowerCase();
                    if (className.contains("recipe") && !className.contains("emi")) {
                        rawRecipe = val;
                        break;
                    }
                }
            }
            if (rawRecipe == null) {
                for (java.lang.reflect.Method m : recipe.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType().getName().toLowerCase().contains("recipe") && !m.getReturnType().getName().toLowerCase().contains("emi")) {
                        m.setAccessible(true);
                        rawRecipe = m.invoke(recipe);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (rawRecipe == null && recipe.getId() != null) {
            try {
                var level = net.minecraft.client.Minecraft.getInstance().level;
                if (level != null) {
                    var recipeOpt = level.getRecipeManager().byKey(recipe.getId());
                    if (recipeOpt.isPresent()) rawRecipe = recipeOpt.get();
                }
            } catch (Exception ignored) {}
        }

        if (rawRecipe != null) {
            try {
                if (rawRecipe.getClass().getName().contains("RecipeHolder")) {
                    try {
                        rawRecipe = rawRecipe.getClass().getMethod("value").invoke(rawRecipe);
                    } catch (Exception e) {
                        java.lang.reflect.Field vField = rawRecipe.getClass().getDeclaredField("value");
                        vField.setAccessible(true);
                        rawRecipe = vField.get(rawRecipe);
                    }
                }
                for (java.lang.reflect.Method m : rawRecipe.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType().isEnum()) {
                        Object res = m.invoke(rawRecipe);
                        if (res != null) {
                            String name = res.toString();
                            if (name.equals("NONE") || name.equals("HEATED") || name.equals("SUPERHEATED")) {
                                hasHeatRequirement = true;
                                if (name.equals("HEATED")) recipeHeat = 1;
                                else if (name.equals("SUPERHEATED")) recipeHeat = 2;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (hasHeatRequirement) return userHeat >= recipeHeat;
        if (recipe.getCatalysts().isEmpty()) return true;

        for (EmiIngredient cat : recipe.getCatalysts()) {
            for (EmiStack stack : cat.getEmiStacks()) {
                if (stack.getId().toString().equals(safeProp)) return true;
            }
        }
        return false;
    }

    /**
     * Calculates how many full batches of the recipe can be performed with the provided user inputs.
     * The method requires that every unique input type present in userInputs is consumed by the recipe. If some
     * user input types are not used by the recipe the recipe is considered partial and returns 0 batches.
     * @param recipeInputs list of recipe ingredient requirements
     * @param userInputs map of available item ids to quantities
     * @return number of full batches that can be executed (0 if not possible)
     */
    private int calculateBatches(List<EmiIngredient> recipeInputs, Map<String, Integer> userInputs) {
        // If recipe requires no inputs, it only matches when the user provided no inputs.
        if (recipeInputs.isEmpty()) {
            return userInputs.isEmpty() ? 1 : 0;
        }

        Map<String, Long> aggregatedRequirements = new HashMap<>();
        Set<String> usedUserInputs = new HashSet<>(); // track which user input types were consumed

        for (EmiIngredient req : recipeInputs) {
            if (req.isEmpty()) continue;

            String matchedId = null;
            // Exact match: look for a user-provided id that matches any valid stack for the ingredient
            for (EmiStack validStack : req.getEmiStacks()) {
                String validId = validStack.getId().toString();
                if (userInputs.containsKey(validId)) {
                    matchedId = validId;
                    break;
                }
            }

            if (matchedId == null) return 0; // missing required ingredient

            usedUserInputs.add(matchedId); // mark that this input type was used
            aggregatedRequirements.put(matchedId, aggregatedRequirements.getOrDefault(matchedId, 0L) + req.getAmount());
        }

        // If the recipe did not consume all unique user input types, treat it as a partial recipe and fail.
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