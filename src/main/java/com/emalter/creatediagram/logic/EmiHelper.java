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
    private static boolean isInitialized = false;

    public static void initCache() {
        if (isInitialized) return;

        // FASE DI SICUREZZA: Se EMI non ha ancora caricato le ricette, interrompiamo
        // per evitare di bloccare la cache su una lista vuota!
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

        // Scansioniamo TUTTO: Input, Output e Catalizzatori
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

        // FAILSAFE ESTREMO: Se la cache è misteriosamente vuota,
        // non nascondiamo il menu, mostriamo tutto!
        if (VALID_INPUTS.isEmpty() && VALID_MACHINES.isEmpty()) {
            return true;
        }

        return VALID_INPUTS.contains(id) || VALID_MACHINES.contains(id);
    }

    public static List<String> getValidCatalystsForMachine(String machineId) {
        if (!isInitialized) initCache();
        Set<String> catalysts = new LinkedHashSet<>();

        if (machineId.equals("create:encased_fan")) {
            catalysts.addAll(List.of("minecraft:water", "minecraft:lava", "minecraft:campfire", "minecraft:soul_campfire"));
        } else if (machineId.equals("create:mechanical_mixer") || machineId.equals("create:basin")) {
            catalysts.addAll(List.of("create:empty_blaze_burner", "create:blaze_burner", "create:blaze_cake"));
        }

        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            boolean isMyWorkstation = false;
            for (EmiIngredient workstation : EmiApi.getRecipeManager().getWorkstations(category)) {
                for (EmiStack stack : workstation.getEmiStacks()) {
                    if (stack.getId().toString().equals(machineId)) {
                        isMyWorkstation = true;
                        break;
                    }
                }
            }

            if (isMyWorkstation) {
                for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
                    for (EmiIngredient cat : recipe.getCatalysts()) {
                        for (EmiStack stack : cat.getEmiStacks()) {
                            String id = stack.getId().toString();
                            if (!id.equals(machineId)) catalysts.add(id);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(catalysts);
    }
}