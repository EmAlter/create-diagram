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
        // Only open the UI on the client side
        if (level.isClientSide()) {
            // Use the client bridge to safely open the diagram screen
            ClientBridge.openDiagramScreen(stack, hand);
        }

        // Notify the game the action succeeded to trigger the hand animation
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}