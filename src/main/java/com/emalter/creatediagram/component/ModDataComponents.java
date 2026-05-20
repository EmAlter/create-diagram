package com.emalter.creatediagram.component;

import com.emalter.creatediagram.CreateDiagram;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    /**
     * Deferred register used to register custom data components for the mod.
     */
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateDiagram.MODID);

    // Register our custom data component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiagramData>> DIAGRAM_DATA =
            DATA_COMPONENTS.register("diagram_data", () ->
                    DataComponentType.<DiagramData>builder()
                            .persistent(DiagramData.CODEC)       // Use codec for disk persistence
                            .networkSynchronized(DiagramData.STREAM_CODEC) // Use stream codec for network sync
                            .build()
            );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}