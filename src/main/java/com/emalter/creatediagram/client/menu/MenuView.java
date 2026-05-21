package com.emalter.creatediagram.client.menu;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * View for the palette menu panel. Handles rendering and UI layout.
 */
public class MenuView {
    private final MenuModel model;
    private EditBox searchBox;

    public MenuView(MenuModel model) {
        this.model = model;
    }

    public void init() {
        this.searchBox = new EditBox(model.getFont(), 10, 45, model.getWidth() - 25, 16,
                net.minecraft.network.chat.Component.literal("Cerca..."));
        this.searchBox.setResponder(text -> {
            model.setSearchBoxValue(text);
            model.setScrollY(0);
        });
    }

    public EditBox getSearchBox() {
        return searchBox;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float targetX = model.getIsOpen() ? 0 : -model.getWidth();
        float currentX = model.getCurrentX();
        currentX += (targetX - currentX) * 0.3f;
        model.setCurrentX(currentX);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(currentX, 0, 0);

        int localMouseX = (int) (mouseX - currentX);
        model.setHoveredStack(null);

        guiGraphics.fill(0, 0, model.getWidth(), model.getHeight(), 0xFF222222);
        guiGraphics.fill(model.getWidth(), 0, model.getWidth() + 1, model.getHeight(), 0xFF000000);

        // Render categories
        renderCategoryBar(guiGraphics, currentX);

        // Render search box
        searchBox.render(guiGraphics, localMouseX, mouseY, partialTick);

        // Render items and scrollbar
        renderItemsAndScrollbar(guiGraphics, localMouseX, mouseY, partialTick);

        // Render panel toggle button
        int btnY = model.getHeight() / 2 - 20;
        guiGraphics.fill(model.getWidth(), btnY, model.getWidth() + 12, btnY + 40, 0xFF333333);
        guiGraphics.drawString(model.getFont(), model.getIsOpen() ? "<" : ">", model.getWidth() + 3, btnY + 16, 0xFFFFFFFF);

        guiGraphics.pose().popPose();

        // Render dragging item on top
        if (model.getDraggingStack() != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            model.getDraggingStack().render(guiGraphics, mouseX - 8, mouseY - 8, partialTick);
            guiGraphics.pose().popPose();
        }

        // Render tooltip
        if (model.getHoveredStack() != null && model.getDraggingStack() == null) {
            guiGraphics.renderTooltip(model.getFont(), model.getHoveredStack().getTooltipText(),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderCategoryBar(GuiGraphics guiGraphics, float currentX) {
        guiGraphics.enableScissor((int) currentX, 20, (int) currentX + model.getWidth(), 45);

        int iconX = 10 - (int) model.getCategoryScrollOffset();
        for (String mod : model.getAvailableMods()) {
            if (mod.equals(model.getSelectedMod())) {
                guiGraphics.fill(iconX - 2, 23, iconX + 18, 43, 0xFF555555);
            }
            guiGraphics.renderItem(model.getModIcon(mod), iconX, 25);
            iconX += 24;
        }

        guiGraphics.disableScissor();
    }

    private void renderItemsAndScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listTop = 70;
        int yOffset = listTop - model.getScrollY();
        int cols = 6;
        int iconSize = 24;

        guiGraphics.enableScissor((int)model.getCurrentX(), listTop, (int)(model.getCurrentX() + model.getWidth()), model.getHeight());

        List<EmiStack> itemsToRender = model.getItemsToRender();

        for (int i = 0; i < itemsToRender.size(); i++) {
            int r = i / cols, c = i % cols;
            int x = 10 + c * iconSize, y = yOffset + r * iconSize;

            if (y + iconSize > listTop && y < model.getHeight()) {
                EmiStack stack = itemsToRender.get(i);
                stack.render(guiGraphics, x + 4, y + 4, partialTick);

                // Check hover
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    model.setHoveredStack(stack);
                }
            }
        }

        // Update hitboxes for click detection
        model.updateActiveHitboxes(itemsToRender, cols, iconSize, yOffset, listTop);

        // Calculate max scroll
        int totalRows = itemsToRender.isEmpty() ? 0 : (itemsToRender.size() - 1) / cols + 1;
        int totalContentHeight = totalRows * iconSize;
        int maxScroll = Math.max(0, totalContentHeight - (model.getHeight() - listTop) + 20);
        model.setMaxScroll(maxScroll);

        guiGraphics.disableScissor();

        // Render scrollbar
        renderScrollbar(guiGraphics, listTop);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int listTop) {
        int maxScroll = model.getMaxScroll();
        if (maxScroll > 0) {
            int sbX = model.getWidth() - 6;
            int sbHeight = model.getHeight() - listTop - 10;
            int barHeight = Math.max(20, (int) (sbHeight * ((float) sbHeight / (sbHeight + maxScroll))));
            int barPos = (int) (listTop + (sbHeight - barHeight) * ((float) model.getScrollY() / maxScroll));
            guiGraphics.fill(sbX, listTop, sbX + 4, listTop + sbHeight, 0xFF111111);
            guiGraphics.fill(sbX, barPos, sbX + 4, barPos + barHeight, model.isScrolling() ? 0xFFCCCCCC : 0xFF666666);
        }
    }

    public void updateScrollFromMouse(double mouseY) {
        int listTop = 70;
        int sbHeight = model.getHeight() - listTop - 10;
        float pct = (float) (mouseY - listTop) / sbHeight;
        model.setScrollY((int) (Mth.clamp(pct, 0, 1) * model.getMaxScroll()));
    }

    public void updateCategoryScroll(double scrollDelta) {
        int totalCategoriesWidth = model.getAvailableMods().size() * 24;
        int maxVisibleWidth = model.getWidth() - 20;
        double maxCatScroll = Math.max(0, totalCategoriesWidth - maxVisibleWidth);

        double newOffset = model.getCategoryScrollOffset() - scrollDelta * 20;
        model.setCategoryScrollOffset(Mth.clamp(newOffset, 0, maxCatScroll));
    }
}

