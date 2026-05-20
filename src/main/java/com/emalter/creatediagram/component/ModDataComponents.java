package com.emalter.creatediagram.component;

import com.emalter.creatediagram.CreateDiagram;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    // Crea il registro specifico per i Data Component
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateDiagram.MODID);

    // Registra il nostro componente personalizzato
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiagramData>> DIAGRAM_DATA =
            DATA_COMPONENTS.register("diagram_data", () ->
                    DataComponentType.<DiagramData>builder()
                            .persistent(DiagramData.CODEC)       // Usa il codec per salvare su disco
                            .networkSynchronized(DiagramData.STREAM_CODEC) // Usa lo stream codec per inviare al server
                            .build()
            );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}