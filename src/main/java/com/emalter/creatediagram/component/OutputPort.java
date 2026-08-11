package com.emalter.creatediagram.component;

import com.emalter.creatediagram.logic.RecipeType;

/**
 * Represents a recipe output with item ID, chance, amount, and the applied process name.
 * This is a domain object independent of the UI layer.
 */
public record OutputPort(String itemId, float chance, int amount, String processName, RecipeType recipeType) {}