package com.emalter.creatediagram.client.menu;

import com.emalter.creatediagram.logic.integration.ModIntegration;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MenuModel {
    private final int width = 180;
    private final int height;
    private final Font font;

    private int scrollY = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;

    private boolean isOpen = true;
    private float currentX = 0;

    private final Map<String, List<EmiStack>> modRegistry = new HashMap<>();
    private final List<String> availableMods = new ArrayList<>();
    private String selectedMod = "create";
    private String searchBoxValue = "";

    private EmiStack draggingStack = null;
    private EmiStack hoveredStack = null;
    private String hoveredCategory = null;

    private double categoryScrollOffset = 0;
    private final Map<String, ItemStack> modIconCache = new HashMap<>();

    public record Hitbox(EmiStack stack, int x, int y) {}
    private final List<Hitbox> activeHitboxes = new ArrayList<>();

    private boolean isLoaded = false;
    
    private List<EmiStack> cachedItemsToRender = new ArrayList<>();

    public MenuModel(int height, Font font) {
        this.height = height;
        this.font = font;
    }

    public void init() {
        if (isLoaded) return;

        CompletableFuture.runAsync(() -> {
            Map<String, List<EmiStack>> tempReg = new HashMap<>();

            for (EmiStack emiStack : EmiApi.getIndexStacks()) {
                ResourceLocation id = emiStack.getId();
                String modid = id.getNamespace();

                if (ModIntegration.get().isValidInput(id.toString())) {
                    tempReg.computeIfAbsent(modid, k -> new ArrayList<>()).add(emiStack);
                }
            }

            List<String> tempMods = new ArrayList<>(tempReg.keySet());
            Collections.sort(tempMods);

            Minecraft.getInstance().execute(() -> {
                this.modRegistry.clear();
                this.modRegistry.putAll(tempReg);
                this.availableMods.clear();
                this.availableMods.addAll(tempMods);
                this.isLoaded = true;
                this.updateCachedItems();
            });
        });
    }
    
    private void updateCachedItems() {
        if (!isLoaded) {
            cachedItemsToRender = List.of();
            return;
        }

        List<EmiStack> items = new ArrayList<>();
        String query = searchBoxValue.toLowerCase();

        if (!query.isEmpty()) {
            for (List<EmiStack> list : modRegistry.values()) {
                for (EmiStack stack : list) {
                    if (stack.getName().getString().toLowerCase().contains(query)) items.add(stack);
                }
            }
        } else {
            items.addAll(modRegistry.getOrDefault(selectedMod, List.of()).stream()
                    .filter(s -> s.getName().getString().toLowerCase().contains(query))
                    .toList());
        }
        this.cachedItemsToRender = items;
    }
    
    public List<EmiStack> getItemsToRender() {
        return cachedItemsToRender;
    }

    public void selectModCategory(String modid) {
        this.selectedMod = modid;
        this.scrollY = 0;
        this.searchBoxValue = "";
        this.updateCachedItems(); // Aggiorna la cache
    }

    public void setSearchBoxValue(String value) {
        this.searchBoxValue = value;
        this.updateCachedItems(); // Aggiorna la cache
    }

    public ItemStack getModIcon(String modid) {
        return modIconCache.computeIfAbsent(modid, id -> {
            ResourceLocation res = switch (id) {
                case "create" -> ResourceLocation.parse("create:wrench");
                case "minecraft" -> ResourceLocation.parse("minecraft:grass_block");
                case "creatediagram" -> ResourceLocation.parse("creatediagram:diagram");
                default -> null;
            };
            if (res != null) return new ItemStack(BuiltInRegistries.ITEM.get(res));

            for (CreativeModeTab tab : net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                ResourceLocation tabId = net.neoforged.neoforge.common.CreativeModeTabRegistry.getName(tab);
                if (tabId != null && tabId.getNamespace().equals(id)) return tab.getIconItem();
            }
            List<EmiStack> items = modRegistry.get(id);
            if (items != null && !items.isEmpty()) return items.get(new Random().nextInt(items.size())).getItemStack();
            return new ItemStack(Items.CHEST);
        });
    }

    public void updateActiveHitboxes(List<EmiStack> items, int cols, int iconSize, int yOffset, int listTop) {
        activeHitboxes.clear();
        for (int i = 0; i < items.size(); i++) {
            int r = i / cols, c = i % cols;
            int x = 10 + c * iconSize, y = yOffset + r * iconSize;
            if (y + iconSize > listTop && y < height) activeHitboxes.add(new Hitbox(items.get(i), x, y));
        }
    }

    public EmiStack checkHitboxes(int localX, int mouseY, int cols, int iconSize, int yOffset, int listTop) {
        for (Hitbox box : activeHitboxes) {
            if (localX >= box.x && localX < box.x + 16 && mouseY >= box.y && mouseY < box.y + 16) return box.stack;
        }
        return null;
    }

    public boolean isLoaded() { return isLoaded; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getScrollY() { return scrollY; }
    public void setScrollY(int scrollY) { this.scrollY = scrollY; }
    public int getMaxScroll() { return maxScroll; }
    public void setMaxScroll(int maxScroll) { this.maxScroll = maxScroll; }
    public boolean isScrolling() { return isScrolling; }
    public void setScrolling(boolean scrolling) { this.isScrolling = scrolling; }
    public boolean getIsOpen() { return isOpen; }
    public void setIsOpen(boolean isOpen) { this.isOpen = isOpen; }
    public float getCurrentX() { return currentX; }
    public void setCurrentX(float currentX) { this.currentX = currentX; }
    public String getSelectedMod() { return selectedMod; }
    public String getSearchBoxValue() { return searchBoxValue; }
    public double getCategoryScrollOffset() { return categoryScrollOffset; }
    public void setCategoryScrollOffset(double offset) { this.categoryScrollOffset = offset; }
    public List<String> getAvailableMods() { return availableMods; }
    public EmiStack getDraggingStack() { return draggingStack; }
    public void setDraggingStack(EmiStack stack) { this.draggingStack = stack; }
    public EmiStack getHoveredStack() { return hoveredStack; }
    public void setHoveredStack(EmiStack stack) { this.hoveredStack = stack; }
    public String getHoveredCategory() { return hoveredCategory; }
    public void setHoveredCategory(String category) { this.hoveredCategory = category; }
    public Font getFont() { return font; }
}