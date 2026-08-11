package com.emalter.creatediagram.logic;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmiHelper {
    // 1. Thread-Safe Collections
    private static final Map<String, EmiStack> STACK_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> VALID_MACHINES = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> VALID_INPUTS = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> HIDDEN_MENU_ITEMS = Set.of("create:basin");
    private static final Map<String, List<String>> catalystCache = new ConcurrentHashMap<>();

    // Volatile assicura che il cambio di stato sia visibile istantaneamente a tutti i thread
    private static volatile boolean isInitialized = false;

    public enum HeatLevel {
        NONE, HEATED, SUPERHEATED;
        public static HeatLevel max(HeatLevel first, HeatLevel second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    // 2. Metodo Synchronized per impedire esecuzioni simultanee
    public static synchronized void initCache() {
        if (isInitialized) return;
        if (EmiApi.getRecipeManager().getRecipes().isEmpty()) return;

        // Estrazione base
        for (EmiStack stack : EmiApi.getIndexStacks()) {
            STACK_CACHE.putIfAbsent(stack.getId().toString(), stack);
        }

        // Estrazione macchinari
        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            for (EmiIngredient workstation : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : workstation.getEmiStacks()) {
                    String id = stack.getId().toString();
                    if (!id.equals("create:basin")) {
                        VALID_MACHINES.add(id);
                    }
                    STACK_CACHE.putIfAbsent(id, stack);
                }
            }
        }

        // 3. FIX LIQUIDI: Itera sulle ricette per forzare il caching di fluidi e item nascosti
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes()) {
            for (EmiIngredient req : recipe.getInputs()) {
                for (EmiStack stack : req.getEmiStacks()) {
                    String id = stack.getId().toString();
                    VALID_INPUTS.add(id);
                    STACK_CACHE.putIfAbsent(id, stack); // Fondamentale per salvare la lava!
                }
            }
            for (EmiIngredient cat : recipe.getCatalysts()) {
                for (EmiStack stack : cat.getEmiStacks()) {
                    String id = stack.getId().toString();
                    VALID_INPUTS.add(id);
                    STACK_CACHE.putIfAbsent(id, stack);
                }
            }
            for (EmiStack out : recipe.getOutputs()) {
                String id = out.getId().toString();
                VALID_INPUTS.add(id);
                STACK_CACHE.putIfAbsent(id, out);
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

    public static List<String> getValidCatalystsForMachine(String machineId) {
        if (!isInitialized) initCache();

        return catalystCache.computeIfAbsent(machineId, id -> {
            Set<String> catalysts = new LinkedHashSet<>();
            HeatLevel highestHeat = HeatLevel.NONE;

            for (EmiRecipe recipe : getRecipesForMachine(id)) {
                highestHeat = HeatLevel.max(highestHeat, getRecipeHeatLevel(recipe));
            }

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
        if (!isInitialized) initCache(); // Sicurezza extra
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
        if (!isInitialized) initCache();
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