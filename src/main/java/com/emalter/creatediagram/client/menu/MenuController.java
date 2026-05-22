package com.emalter.creatediagram.client.menu;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Controller for the palette menu panel. Exposes public interfaces and handles
 * interaction between View and Model.
 */
public class MenuController {
    private final MenuModel model;
    private final MenuView view;

    public MenuController(int height, Font font) {
        this.model = new MenuModel(height, font);
        this.view = new MenuView(model);
    }

    public void init() {
        model.init();
        view.init();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        view.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX = (int) (mouseX - model.getCurrentX());

        // Panel toggle button
        if (localX >= model.getWidth() && localX <= model.getWidth() + 12 &&
                mouseY >= model.getHeight() / 2f - 20 && mouseY <= model.getHeight() / 2f + 20) {
            model.setIsOpen(!model.getIsOpen());
            return true;
        }

        if (localX > model.getWidth() || localX < 0) return false;

        // Scrollbar
        if (localX >= model.getWidth() - 10) {
            model.setScrolling(true);
            view.updateScrollFromMouse(mouseY);
            return true;
        }

        // Search box
        if (view.getSearchBox().mouseClicked(localX, mouseY, button)) {
            view.getSearchBox().setFocused(true);
            return true;
        } else {
            view.getSearchBox().setFocused(false);
        }

        // Category selection
        if (mouseY >= 23 && mouseY <= 45) {
            int iconX = 10 - (int) model.getCategoryScrollOffset();
            for (String mod : model.getAvailableMods()) {
                if (localX >= iconX && localX <= iconX + 16) {
                    model.selectModCategory(mod);
                    return true;
                }
                iconX += 24;
            }
        }

        // Item selection/dragging
        if (button == 0) {
            int cols = 6;
            int iconSize = 24;
            int listTop = 70;
            int yOffset = listTop - model.getScrollY();

            EmiStack stack = model.checkHitboxes(localX, (int) mouseY, cols, iconSize, yOffset, listTop);
            if (stack != null) {
                model.setDraggingStack(stack);
                return true;
            }
        }

        return true;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (model.isScrolling()) {
            view.updateScrollFromMouse(mouseY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int localX = (int) (mouseX - model.getCurrentX());

        // Horizontal scrolling for categories
        if (localX >= 0 && localX <= model.getWidth() && mouseY >= 20 && mouseY <= 45) {
            view.updateCategoryScroll(scrollY);
            return true;
        }

        // Vertical scrolling for items
        if (isMouseOverPanel(mouseX, mouseY)) {
            int newScroll = (int) (model.getScrollY() - scrollY * 25);
            model.setScrollY(Math.max(0, Math.min(newScroll, model.getMaxScroll())));
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return view.getSearchBox().isFocused() && view.getSearchBox().keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return view.getSearchBox().isFocused() && view.getSearchBox().charTyped(codePoint, modifiers);
    }

    // Public API methods
    public boolean getIsOpen() {
        return model.getIsOpen();
    }

    public void setIsOpen(boolean isOpen) {
        model.setIsOpen(isOpen);
    }

    public int getWidth() {
        return model.getWidth();
    }

    public boolean isScrolling() {
        return model.isScrolling();
    }

    public void setScrolling(boolean scrolling) {
        model.setScrolling(scrolling);
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY) {
        // Always allow clicks on the small toggle button so the panel can be reopened
        float toggleLeft = model.getCurrentX() + model.getWidth();
        float toggleRight = toggleLeft + 12;
        float toggleTop = model.getHeight() / 2f - 20;
        float toggleBottom = model.getHeight() / 2f + 20;

        if (mouseX >= toggleLeft && mouseX <= toggleRight && mouseY >= toggleTop && mouseY <= toggleBottom) {
            return true;
        }

        // When panel is open, treat the full panel area as the hit region; when closed, clicks
        // outside the toggle button should fall through to the canvas (so zoom/interaction works).
        if (!model.getIsOpen()) return false;

        return mouseX >= model.getCurrentX() && mouseX <= model.getCurrentX() + model.getWidth() + 15
                && mouseY >= 0 && mouseY <= model.getHeight();
    }

    public void unfocusSearch() {
        view.getSearchBox().setFocused(false);
    }
    

    public String getDraggingItemId() {
        EmiStack draggingStack = model.getDraggingStack();
        if (draggingStack != null) {
            return draggingStack.getId().toString();
        }
        return null;
    }

    public void setDraggingItem(Item item) {
        if (item == null) {
            model.setDraggingStack(null);
        }
    }
}

