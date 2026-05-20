package com.emalter.creatediagram.logic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class RecipeAnalyzer {

    private final RecipeManager recipeManager;

    public RecipeAnalyzer() {
        // Obtain the client's recipe manager if available
        ClientLevel level = Minecraft.getInstance().level;
        this.recipeManager = (level != null) ? level.getRecipeManager() : null;
    }

    // Example helper method to print the number of smelting recipes available
    public void printSmeltingRecipes() {
        if (recipeManager == null) return;

        List<RecipeHolder<?>> smeltingRecipes = new ArrayList<>(recipeManager.getAllRecipesFor(RecipeType.SMELTING));
        System.out.println("Found " + smeltingRecipes.size() + " smelting recipes");
    }

    // In later stages we will use this manager to extract Create-specific recipe types such as
    // splashing, pressing and mixing
}