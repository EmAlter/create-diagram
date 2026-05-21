package com.emalter.creatediagram.item;

import com.emalter.creatediagram.client.ClientBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Represents a specialized item that, when used, opens a client-side diagram UI.
 * This item is primarily intended to interact with the {@code ClientBridge} to render
 * a custom UI for creating or managing diagrams.
 *
 * This class extends the base {@code Item} class, inheriting its core properties 
 * and behaviors, and overrides its {@code use} method to provide custom functionality.
 *
 * Key Features:
 * - Custom UI Trigger: Uses {@code ClientBridge.openDiagramScreen} to open
 *   a client-side diagram management UI when the item is used in a supported hand.
 * - Client-Side Handling: Includes logic to ensure the custom behavior is executed
 *   client-side, preventing unnecessary server-side processing.
 * - Hand Animation: Notifies the game that the operation succeeded to trigger 
 *   the hand usage animation.
 */
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