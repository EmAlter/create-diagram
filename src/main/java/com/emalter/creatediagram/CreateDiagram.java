package com.emalter.creatediagram;

import com.emalter.creatediagram.component.ModDataComponents;
import com.emalter.creatediagram.item.ModItems;
import com.emalter.creatediagram.logic.DiagramNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(CreateDiagram.MODID)
public class CreateDiagram {
    public static final String MODID = "creatediagram";

    public CreateDiagram(IEventBus modEventBus) {
        modEventBus.addListener(DiagramNetworking::register);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        
    }
}