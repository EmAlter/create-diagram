package com.emalter.creatediagram.item;

import com.emalter.creatediagram.CreateDiagram;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Registry holder for mod items.
 * This class creates and registers item entries used by the mod.
 */
public class ModItems {

    /**
     * Deferred register used to register items under the mod ID.
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateDiagram.MODID);

    /**
     * Deferred item entry for the diagram item. The item instance is created by the BlueprintItem constructor.
     */
    public static final DeferredItem<Item> DIAGRAM = ITEMS.registerItem(
            "diagram",
            BlueprintItem::new,
            new Item.Properties().stacksTo(1)
    );

    /**
     * Registers the deferred item registry on the provided event bus so entries are created at the correct time.
     * @param eventBus Event bus used by the mod loading system
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}