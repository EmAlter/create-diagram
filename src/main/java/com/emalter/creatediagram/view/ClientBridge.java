package com.emalter.creatediagram.view;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ClientBridge {
    /**
     * Client-only bridge utilities.
     * This class contains methods that must be executed on the client side only.
     */
    /**
     * Opens the diagram UI for the given item stack and hand.
     * This is executed on the client and sets the active screen to a new DiagramScreen.
     * @param stack The item stack used to populate the diagram screen
     * @param hand  The hand the player is holding the item with
     */
    public static void openDiagramScreen(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new DiagramScreen(stack, hand));
    }
}