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

public class DiagramNetworking {

    // 1. Definiamo il Record del Pacchetto
    public record SaveDiagramPayload(DiagramData data) implements CustomPacketPayload {
        public static final Type<SaveDiagramPayload> ID = new Type<>(ResourceLocation.parse("creatediagram:save_diagram"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveDiagramPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.fromCodecWithRegistries(DiagramData.CODEC), SaveDiagramPayload::data,
                SaveDiagramPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    // 2. Metodo per registrare il payload all'avvio della Mod
    // DEVI REGISTRARE QUESTO EVENTO NEL TUO MOD EVENT BUS!
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("creatediagram");

        registrar.playToServer(
                SaveDiagramPayload.ID,
                SaveDiagramPayload.CODEC,
                (payload, context) -> {
                    // Viene eseguito sul Server Thread
                    context.enqueueWork(() -> {
                        // Prende l'oggetto dalla mano principale del giocatore
                        ItemStack mainHandItem = context.player().getMainHandItem();

                        // Opzionale ma consigliato: verifica l'oggetto
                        if (!mainHandItem.isEmpty()) {
                            // Scrive il nuovo Data Component!
                            mainHandItem.set(ModDataComponents.DIAGRAM_DATA, payload.data());
                        }
                    });
                }
        );
    }

    // 3. Metodo invocato dal Client per inviare i dati al Server
    public static void sendSavePacket(DiagramData data) {
        PacketDistributor.sendToServer(new SaveDiagramPayload(data));
    }
}