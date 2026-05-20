package com.emalter.creatediagram.item;

import com.emalter.creatediagram.CreateDiagram;
// Puoi rimuovere anche le importazioni di DiagramData e ModDataComponents se non usate qui
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateDiagram.MODID);

    public static final DeferredItem<Item> DIAGRAM = ITEMS.registerItem(
            "diagram",
            BlueprintItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}