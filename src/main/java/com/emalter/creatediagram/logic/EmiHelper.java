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
        NONE, HEATED, SUPERHEATED;
        public static HeatLevel max(HeatLevel first, HeatLevel second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    public static void initCache() {
        if (isInitialized) return;
        if (EmiApi.getRecipeManager().getRecipes().isEmpty()) return;

        for (EmiStack stack : EmiApi.getIndexStacks()) {
            STACK_CACHE.putIfAbsent(stack.getId().toString(), stack);
        }

        // QUI AVVIENE IL POPOLAMENTO DI VALID_MACHINES (Dove la neve viene erroneamente aggiunta)
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
        if (HIDDEN_MENU_ITEMS.contains(id)) return false;
        if (VALID_INPUTS.isEmpty() && VALID_MACHINES.isEmpty()) return true;
        return VALID_INPUTS.contains(id) || VALID_MACHINES.contains(id);
    }

    // === METODO DI DEBUG AGGIUNTO ===
    private static void debugCatalystPaths(String machineId) {
        System.out.println("====== EMI DEBUGGING per " + machineId + " ======");
        System.out.println("[DEBUG] Categorie associate alla macchina tramite getEmiCategories: ");
        for(EmiRecipeCategory cat : getEmiCategories(new LinkedHashSet<>(), machineId)) {
            System.out.println("  -> " + cat.getId());
        }

        System.out.println("\n[DEBUG] Scansione Workstation per Neve, Acqua e Fan (Path A):");
        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            for (EmiIngredient ws : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : ws.getEmiStacks()) {
                    String id = stack.getId().toString();
                    if (id.contains("snow") || id.contains("industrial_fan") || id.contains("water") || id.contains("fire")) {
                        System.out.println("[WS] Categoria=" + category.getId() + " | Stack=" + id + " | isMachine() dice: " + isMachine(id));
                    }
                }
            }
        }
        System.out.println("==================================================");
    }

    public static List<String> getValidCatalystsForMachine(String machineId) {
        if (!isInitialized) initCache();

        return catalystCache.computeIfAbsent(machineId, id -> {
            Set<String> catalysts = new LinkedHashSet<>();
            HeatLevel highestHeat = HeatLevel.NONE;

            // Cerca esclusivamente il livello di calore massimo richiesto dalle ricette
            for (EmiRecipe recipe : getRecipesForMachine(id)) {
                highestHeat = HeatLevel.max(highestHeat, getRecipeHeatLevel(recipe));
            }

            // Aggiunge al menù solo gli item per il calore
            if (highestHeat != HeatLevel.NONE) {
                catalysts.add("create:empty_blaze_burner");
                catalysts.add("create:blaze_burner");
                if (highestHeat == HeatLevel.SUPERHEATED) {
                    catalysts.add("create:blaze_cake");
                }
            }

            return new ArrayList<>(catalysts);
        });
    }

    public static List<EmiRecipe> getRecipesForMachine(String machineId) {
        Set<EmiRecipeCategory> categories = getEmiCategories(new LinkedHashSet<>(), machineId);
        List<EmiRecipe> recipes = new ArrayList<>();
        for (EmiRecipeCategory category : categories) {
            recipes.addAll(EmiApi.getRecipeManager().getRecipes(category));
        }
        return recipes;
    }

    public static HeatLevel getRecipeHeatLevel(EmiRecipe recipe) {
        try {
            Object backing = recipe.getBackingRecipe();
            if (backing != null) {
                Object recipeVal = backing;
                if (backing instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
                    recipeVal = holder.value();
                }

                for (java.lang.reflect.Method m : recipeVal.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType().isEnum()) {
                        Object res = m.invoke(recipeVal);
                        if (res != null) {
                            String name = res.toString();
                            if (name.equals("HEATED")) return HeatLevel.HEATED;
                            if (name.equals("SUPERHEATED")) return HeatLevel.SUPERHEATED;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return HeatLevel.NONE;
    }

    public static <C extends Collection<EmiRecipeCategory>> C getEmiCategories(C categories, String machineId) {
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
        return categories;
    }
}