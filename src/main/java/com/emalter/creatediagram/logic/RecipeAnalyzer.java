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
        // Recuperiamo il manager delle ricette dal mondo lato client
        ClientLevel level = Minecraft.getInstance().level;
        this.recipeManager = (level != null) ? level.getRecipeManager() : null;
    }

    // Esempio di metodo per testare se riusciamo a leggere le ricette base
    public void printSmeltingRecipes() {
        if (recipeManager == null) return;

        List<RecipeHolder<?>> smeltingRecipes = new ArrayList<>(recipeManager.getAllRecipesFor(RecipeType.SMELTING));
        System.out.println("Trovate " + smeltingRecipes.size() + " ricette di fornace!");
    }

    // Nelle prossime fasi useremo questo manager per estrarre:
    // AllRecipeTypes.SPLASHING, AllRecipeTypes.PRESSING, AllRecipeTypes.MIXING (che sono i tipi specifici di Create)
}