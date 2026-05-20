package com.emalter.creatediagram.item;

import com.emalter.creatediagram.view.ClientBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlueprintItem extends Item {

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Controlliamo se stiamo eseguendo il codice sul Client (il gioco visivo)
        if (level.isClientSide()) {
            // Chiamiamo il ponte client-side in modo sicuro
            ClientBridge.openDiagramScreen(stack, hand);
        }

        // Diciamo al gioco che l'azione ha avuto successo, attivando l'animazione della mano
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}