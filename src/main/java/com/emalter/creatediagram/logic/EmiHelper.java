package com.emalter.creatediagram.logic;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.util.*;

public class EmiHelper {
    private static final Map<String, EmiStack> STACK_CACHE = new HashMap<>();
    private static final Set<String> VALID_MACHINES = new HashSet<>();
    private static final Set<String> VALID_INPUTS = new HashSet<>();
    private static final Set<String> HIDDEN_MENU_ITEMS = Set.of("create:basin");
    private static final Map<String, List<String>> catalystCache = new HashMap<>();
    private static boolean isInitialized = false;

    public enum HeatLevel {
        NONE,
        HEATED,
        SUPERHEATED;

        public static HeatLevel max(HeatLevel first, HeatLevel second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    public static void initCache() {
        if (isInitialized) return;

        // Safety: if EMI hasn't loaded recipes yet, abort to avoid caching an empty list
        if (EmiApi.getRecipeManager().getRecipes().isEmpty()) {
            return;
        }

        for (EmiStack stack : EmiApi.getIndexStacks()) {
            STACK_CACHE.putIfAbsent(stack.getId().toString(), stack);
        }

        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            for (EmiIngredient workstation : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : workstation.getEmiStacks()) {
                    String id = stack.getId().toString();
                    if (!id.equals("create:basin")) {
                        VALID_MACHINES.add(id);
                    }
                }
            }
        }

        // Scan all recipes: inputs, outputs and catalysts
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes()) {
            for (EmiIngredient req : recipe.getInputs()) {
                for (EmiStack stack : req.getEmiStacks()) VALID_INPUTS.add(stack.getId().toString());
            }
            for (EmiIngredient cat : recipe.getCatalysts()) {
                for (EmiStack stack : cat.getEmiStacks()) VALID_INPUTS.add(stack.getId().toString());
            }
            for (EmiStack out : recipe.getOutputs()) {
                VALID_INPUTS.add(out.getId().toString());
            }
        }

        VALID_INPUTS.addAll(List.of("minecraft:water", "minecraft:lava", "minecraft:campfire", "minecraft:soul_campfire"));

        isInitialized = true;
    }

    public static EmiStack getStack(String id) {
        if (!isInitialized) initCache();
        return STACK_CACHE.getOrDefault(id, EmiStack.EMPTY);
    }

    public static boolean isMachine(String id) {
        if (!isInitialized) initCache();
        return VALID_MACHINES.contains(id);
    }

    public static boolean isValidInput(String id) {
        if (!isInitialized) initCache();

        if (HIDDEN_MENU_ITEMS.contains(id)) {
            return false;
        }

        // Extreme failsafe: if the cache is unexpectedly empty, expose everything instead of hiding the menu
        if (VALID_INPUTS.isEmpty() && VALID_MACHINES.isEmpty()) {
            return true;
        }

        return VALID_INPUTS.contains(id) || VALID_MACHINES.contains(id);
    }

    public static List<String> getValidCatalystsForMachine(String machineId) {
        if (!isInitialized) initCache();

        // computeIfAbsent controlla se la macchina è già in cache.
        // Se c'è, restituisce subito la lista. Altrimenti, esegue il calcolo.
        return catalystCache.computeIfAbsent(machineId, id -> {
            Set<String> catalysts = new LinkedHashSet<>();
            HeatLevel highestHeat = HeatLevel.NONE;

            // 1. Hardcode per la Ventola (Encased Fan)
            if (id.equals("create:encased_fan")) {
                catalysts.addAll(List.of("minecraft:water", "minecraft:lava", "minecraft:campfire", "minecraft:soul_campfire"));
            }

            // 2. Iterazione sulle ricette di EMI
            for (EmiRecipe recipe : getRecipesForMachine(id)) {
                highestHeat = HeatLevel.max(highestHeat, getRecipeHeatLevel(recipe));

                for (EmiIngredient cat : recipe.getCatalysts()) {
                    for (EmiStack stack : cat.getEmiStacks()) {
                        String stackId = stack.getId().toString();
                        if (!stackId.equals(id)) catalysts.add(stackId);
                    }
                }
            }

            // 3. Gestione del Calore (Blaze Burner)
            if (highestHeat != HeatLevel.NONE) {
                catalysts.add("create:empty_blaze_burner");
                catalysts.add("create:blaze_burner");
                if (highestHeat == HeatLevel.SUPERHEATED) {
                    catalysts.add("create:blaze_cake");
                }
            }

            // Restituisce la lista, che verrà automaticamente salvata nella mappa catalystCache
            return new ArrayList<>(catalysts);
        });
    }

    private static List<EmiRecipe> getRecipesForMachine(String machineId) {
        Set<EmiRecipeCategory> categories = new LinkedHashSet<>();

        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            for (EmiIngredient workstation : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : workstation.getEmiStacks()) {
                    if (stack.getId().toString().equals(machineId)) {
                        categories.add(category);
                        break;
                    }
                }
            }
        }

        List<EmiRecipe> recipes = new ArrayList<>();
        for (EmiRecipeCategory category : categories) {
            recipes.addAll(EmiApi.getRecipeManager().getRecipes(category));
        }
        return recipes;
    }

    public static HeatLevel getRecipeHeatLevel(EmiRecipe recipe) {
        Object rawRecipe = extractRawRecipe(recipe);

        if (rawRecipe == null) {
            return HeatLevel.NONE;
        }

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
                        if (name.equals("HEATED")) return HeatLevel.HEATED;
                        if (name.equals("SUPERHEATED")) return HeatLevel.SUPERHEATED;
                    }
                }
            }
        } catch (Exception ignored) {}

        return HeatLevel.NONE;
    }

    private static Object extractRawRecipe(EmiRecipe recipe) {
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

        return rawRecipe;
    }
}
