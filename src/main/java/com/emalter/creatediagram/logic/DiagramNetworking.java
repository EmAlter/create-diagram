package com.emalter.creatediagram.logic;

import com.emalter.creatediagram.component.DiagramData;
import com.emalter.creatediagram.component.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public class DiagramNetworking {

    // 1. Define the packet payload record
    public record SaveDiagramPayload(DiagramData data) implements CustomPacketPayload {
        public static final Type<SaveDiagramPayload> ID = new Type<>(ResourceLocation.parse("creatediagram:save_diagram"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveDiagramPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.fromCodecWithRegistries(DiagramData.CODEC), SaveDiagramPayload::data,
                SaveDiagramPayload::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    // 2. Register the payload handlers during mod initialization.
    // Ensure this method is called from your mod event bus during setup.
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("creatediagram");

        registrar.playToServer(
                SaveDiagramPayload.ID,
                SaveDiagramPayload.CODEC,
                (payload, context) -> {
                    // This runs on the server thread
                    context.enqueueWork(() -> {
                        // Read the player's main-hand item
                        ItemStack mainHandItem = context.player().getMainHandItem();

                        // Optional safety check: ensure the item exists
                        if (!mainHandItem.isEmpty()) {
                            // Write the DiagramData component into the held item
                            mainHandItem.set(ModDataComponents.DIAGRAM_DATA, payload.data());
                        }
                    });
                }
        );
    }

    // 3. Client helper to send diagram data to the server
    public static void sendSavePacket(DiagramData data) {
        PacketDistributor.sendToServer(new SaveDiagramPayload(data));
    }
}