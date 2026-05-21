package com.emalter.creatediagram.component;

/**
 * Represents a recipe output with item ID, chance, and amount.
 * This is a domain object independent of the UI layer.
 */
public record RecipeOutput(String itemId, float chance, int amount) {}

