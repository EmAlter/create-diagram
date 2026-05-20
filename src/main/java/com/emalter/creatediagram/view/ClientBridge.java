package com.emalter.creatediagram.view;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ClientBridge {
    // Questo metodo sarà eseguito SOLO se ci troviamo sul client
    public static void openDiagramScreen(ItemStack stack, InteractionHand hand) {
        // Impostiamo la schermata attiva passando l'oggetto che stiamo tenendo in mano
        Minecraft.getInstance().setScreen(new DiagramScreen(stack, hand));
    }
}