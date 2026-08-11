package com.emalter.creatediagram.logic.integration;

import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.logic.RecipeEngine;
import dev.emi.emi.api.stack.EmiStack;

import java.util.List;
import java.util.Map;

/**
 * Lightweight compatibility wrapper that exposes helper methods used across
 * the UI. It delegates to EmiHelper implementation 
 * so the rest of the codebase can call a consistent API.
 */
public final class ModIntegration {
    private static final ModIntegration INSTANCE = new ModIntegration();
    private final RecipeEngine recipeEngine = new RecipeEngine();

    private ModIntegration() {
    }

    //EmiHelper
    public static ModIntegration get() {
        return INSTANCE;
    }

    public EmiStack getStack(String id) {
        return EmiHelper.getStack(id);
    }

    public boolean isMachine(String id) {
        return EmiHelper.isMachine(id);
    }

    public boolean isValidInput(String id) {
        return EmiHelper.isValidInput(id);
    }

    public List<String> getValidCatalystsForMachine(String machineId) {
        return EmiHelper.getValidCatalystsForMachine(machineId);
    }
    
    public List<OutputPort> getOutputs(String machineId, String property, Map<String, Integer> inputs) {
        return recipeEngine.getOutputs(machineId, property, inputs);
    }

    public String getNextAlternativeTarget(String machineId, String property, Map<String, Integer> inputs) {
        return recipeEngine.getNextAlternativeTarget(machineId, property, inputs);
    }

    public List<String> getInfiniteInputs(String machineId, String property, Map<String, Integer> inputs) {
        return recipeEngine.getInfiniteInputs(machineId, property, inputs);
    }
    
}


