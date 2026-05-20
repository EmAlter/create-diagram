package com.emalter.creatediagram.view.widget;

import com.emalter.creatediagram.logic.EmiHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

import java.util.*;

public class PalettePanel {
    private final int width = 180;
    private final int height;

    private int scrollY = 0;
    private int maxScroll = 0;
    private boolean isScrolling = false;
    private boolean isOpen = true;
    private float currentX = 0;

    // NUOVO: Struttura dati semplificata (Mod -> Lista di Oggetti)
    private final Map<String, List<EmiStack>> modRegistry = new HashMap<>();
    private final List<String> availableMods = new ArrayList<>();
    private String selectedMod = "create";

    private EditBox searchBox;
    private final Font font;

    private record Hitbox(EmiStack stack, int x, int y) {}
    private final List<Hitbox> activeHitboxes = new ArrayList<>();

    private EmiStack draggingStack = null;
    private EmiStack hoveredStack = null;

    public PalettePanel(int height, Font font) {
        this.height = height;
        this.font = font;
    }

    public void init() {
        this.searchBox = new EditBox(this.font, 10, 45, width - 25, 16, Component.literal("Cerca..."));
        this.searchBox.setResponder(text -> this.scrollY = 0);
        buildRegistryCache();
    }
    
    public boolean getIsOpen() { return isOpen; }
    public void setIsOpen(boolean isOpen) { this.isOpen = isOpen; }

    private void buildRegistryCache() {
        modRegistry.clear();
        availableMods.clear();

        // Iterazione Master API EMI
        for (EmiStack emiStack : EmiApi.getIndexStacks()) {
            ResourceLocation id = emiStack.getId();
            String modid = id.getNamespace();

            if (!EmiHelper.isValidInput(id.toString())) continue;

            modRegistry.computeIfAbsent(modid, k -> new ArrayList<>()).add(emiStack);
        }

        availableMods.addAll(modRegistry.keySet());
        Collections.sort(availableMods);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float targetX = isOpen ? 0 : -width;
        currentX += (targetX - currentX) * 0.3f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(currentX, 0, 0);

        int localMouseX = (int) (mouseX - currentX);
        hoveredStack = null;
        activeHitboxes.clear();

        guiGraphics.fill(0, 0, width, height, 0xFF222222);
        guiGraphics.fill(width, 0, width + 1, height, 0xFF000000);

        int iconX = 10;
        for (String mod : availableMods) {
            if (mod.equals(selectedMod)) guiGraphics.fill(iconX - 2, 23, iconX + 18, 43, 0xFF555555);
            guiGraphics.renderItem(getModIcon(mod), iconX, 25);
            iconX += 24;
        }

        this.searchBox.render(guiGraphics, localMouseX, mouseY, partialTick);
        renderItemsAndScrollbar(guiGraphics, localMouseX, mouseY, partialTick);

        int btnY = height / 2 - 20;
        guiGraphics.fill(width, btnY, width + 12, btnY + 40, 0xFF333333);
        guiGraphics.drawString(this.font, isOpen ? "<" : ">", width + 3, btnY + 16, 0xFFFFFFFF);

        guiGraphics.pose().popPose();

        if (draggingStack != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            draggingStack.render(guiGraphics, mouseX - 8, mouseY - 8, partialTick);
            guiGraphics.pose().popPose();
        }

        if (hoveredStack != null && draggingStack == null) {
            guiGraphics.renderTooltip(this.font, hoveredStack.getTooltipText(), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderItemsAndScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listTop = 70;
        int yOffset = listTop - scrollY;
        int cols = 6;
        int iconSize = 24;

        guiGraphics.enableScissor(0, listTop, (int)(currentX + width), height);

        String query = searchBox.getValue().toLowerCase();
        boolean isSearching = !query.isEmpty();

        List<EmiStack> itemsToRender = new ArrayList<>();

        // Se stiamo cercando, prendiamo da TUTTE le mod. Altrimenti, solo dalla mod selezionata.
        if (isSearching) {
            for (List<EmiStack> items : modRegistry.values()) {
                for (EmiStack stack : items) {
                    if (stack.getName().getString().toLowerCase().contains(query)) {
                        itemsToRender.add(stack);
                    }
                }
            }
        } else {
            itemsToRender.addAll(modRegistry.getOrDefault(selectedMod, List.of()).stream()
                    .filter(s -> s.getName().getString().toLowerCase().contains(query))
                    .toList());
        }

        // Griglia uniforme senza interruzioni di categoria
        for (int i = 0; i < itemsToRender.size(); i++) {
            int r = i / cols, c = i % cols;
            int x = 10 + c * iconSize, y = yOffset + r * iconSize;

            if (y + iconSize > listTop && y < height) {
                EmiStack stack = itemsToRender.get(i);
                stack.render(guiGraphics, x + 4, y + 4, partialTick);
                activeHitboxes.add(new Hitbox(stack, x, y));
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    hoveredStack = stack;
                }
            }
        }

        // Calcolo altezza totale per lo scorrimento
        int totalRows = itemsToRender.isEmpty() ? 0 : (itemsToRender.size() - 1) / cols + 1;
        int totalContentHeight = totalRows * iconSize;

        this.maxScroll = Math.max(0, totalContentHeight - (height - listTop) + 20);
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int sbX = width - 6;
            int sbHeight = height - listTop - 10;
            int barHeight = Math.max(20, (int) (sbHeight * ((float) sbHeight / (sbHeight + maxScroll))));
            int barPos = (int) (listTop + (sbHeight - barHeight) * ((float) scrollY / maxScroll));
            guiGraphics.fill(sbX, listTop, sbX + 4, listTop + sbHeight, 0xFF111111);
            guiGraphics.fill(sbX, barPos, sbX + 4, barPos + barHeight, isScrolling ? 0xFFCCCCCC : 0xFF666666);
        }
    }

    private final Map<String, ItemStack> modIconCache = new HashMap<>();

    private ItemStack getModIcon(String modid) {
        return modIconCache.computeIfAbsent(modid, id -> {
            // Casi hardcoded
            ResourceLocation res = switch (id) {
                case "create" -> ResourceLocation.parse("create:wrench");
                case "minecraft" -> ResourceLocation.parse("minecraft:grass_block");
                case "creatediagram" -> ResourceLocation.parse("creatediagram:diagram");
                default -> null;
            };

            if (res != null) {
                return new ItemStack(BuiltInRegistries.ITEM.get(res));
            }

            // Cerca il primo CreativeModeTab registrato dalla mod
            for (CreativeModeTab tab : net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                ResourceLocation tabId = net.neoforged.neoforge.common.CreativeModeTabRegistry.getName(tab);
                if (tabId != null && tabId.getNamespace().equals(id)) {
                    return tab.getIconItem();
                }
            }

            // Fallback
            List<EmiStack> items = modRegistry.get(id);
            if (items != null && !items.isEmpty()) {
                return items.get(new Random().nextInt(items.size())).getItemStack();
            }

            return new ItemStack(Items.CHEST);
        });
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX = (int) (mouseX - currentX);

        if (localX >= width && localX <= width + 12 && mouseY >= height / 2f - 20 && mouseY <= height / 2f + 20) {
            this.isOpen = !this.isOpen;
            return true;
        }
        if (localX > width || localX < 0) return false;

        if (localX >= width - 10) {
            this.isScrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        if (this.searchBox.mouseClicked(localX, mouseY, button)) {
            this.searchBox.setFocused(true);
            return true;
        } else {
            this.searchBox.setFocused(false);
        }

        if (mouseY >= 25 && mouseY <= 41) {
            int iconX = 10;
            for (String mod : availableMods) {
                if (localX >= iconX && localX <= iconX + 16) {
                    this.selectedMod = mod;
                    this.scrollY = 0;
                    this.searchBox.setValue("");
                    return true;
                }
                iconX += 24;
            }
        }

        if (button == 0) {
            for (Hitbox box : activeHitboxes) {
                if (localX >= box.x && localX < box.x + 16 && mouseY >= box.y && mouseY < box.y + 16) {
                    this.draggingStack = box.stack;
                    return true;
                }
            }
        }
        return true;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) updateScrollFromMouse(mouseY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int listTop = 70;
        int sbHeight = height - listTop - 10;
        float pct = (float) (mouseY - listTop) / sbHeight;
        this.scrollY = (int) (Mth.clamp(pct, 0, 1) * maxScroll);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (isMouseOverPanel(mouseX, mouseY)) {
            this.scrollY = Mth.clamp(this.scrollY - (int) (scrollY * 25), 0, maxScroll);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.searchBox.isFocused() && this.searchBox.keyPressed(keyCode, scanCode, modifiers);
    }
    public boolean charTyped(char codePoint, int modifiers) {
        return this.searchBox.isFocused() && this.searchBox.charTyped(codePoint, modifiers);
    }

    public boolean isScrolling() { return isScrolling; }
    public void setScrolling(boolean scrolling) { isScrolling = scrolling; }
    public boolean isMouseOverPanel(double mouseX, double mouseY) { return mouseX <= currentX + width + 15; }
    public void unfocusSearch() { this.searchBox.setFocused(false); }
    public int getWidth() { return width; }

    public Item getDraggingItem() {
        if (draggingStack == null) return null;
        if (draggingStack.getKey() instanceof Item item) return item;
        return net.minecraft.world.item.Items.WATER_BUCKET;
    }
    public String getDraggingItemId() {
        if (draggingStack != null) {
            return draggingStack.getId().toString();
        }
        return null;
    }
    public void setDraggingItem(Item item) {
        if (item == null) this.draggingStack = null;
    }
}