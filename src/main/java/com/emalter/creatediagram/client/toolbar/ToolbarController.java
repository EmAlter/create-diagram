package com.emalter.creatediagram.client.toolbar;

import com.emalter.creatediagram.client.diagram.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

public class ToolbarController {
    private final ToolbarModel model;
    private final ToolbarView view;

    public ToolbarController() {
        this.model = new ToolbarModel();
        this.view = new ToolbarView();
    }

    // Metodi di input — ricevono click grezzi, traducono in azioni model
    public boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int screenHeight, int paletteWidth) {
        Tool[] tools = Tool.values();
        int startX = view.getToolbarStartX(screenWidth, paletteWidth);
        int startY = view.getToolbarStartY(screenHeight);
        int toolbarW = view.getToolbarWidth();

        // Click dentro la toolbar
        if (mouseX >= startX && mouseX <= startX + toolbarW && mouseY >= startY && mouseY <= startY + 26) {
            int index = (int) ((mouseX - startX - 4) / 24);
            if (index >= 0 && index < tools.length) {
                model.setCurrentTool(tools[index]);
                int anchorX = startX + 4 + (index * 24);
                if (tools[index] == Tool.PEN || tools[index] == Tool.LINE) {
                    // Open color menu and mark that we're awaiting a release selection
                    model.setColorMenuOpen(true);
                    model.setColorMenuAnchor(anchorX, startY);
                    model.setAwaitingColorSelection(true);
                } else {
                    model.setColorMenuOpen(false);
                    model.setAwaitingColorSelection(false);
                }
            }
            return true;
        }

        // Click dentro il popup colori (se aperto)
        if (model.isColorMenuOpen()) {
            String[] dyeIds = Color.getAllDyeIds();
            int[] dyeColors = Color.getAllHexValues();
            int menuW = (dyeIds.length * 20) + 4;
            int menuX = model.getColorMenuAnchorX() - (menuW / 2) + 10;
            int menuY = model.getColorMenuAnchorY() - 30;

            if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + 26) {
                int index = (int) ((mouseX - menuX - 4) / 20);
                if (index >= 0 && index < dyeColors.length) {
                    model.setCurrentColor(dyeColors[index]);
                    model.setColorMenuOpen(false);
                    model.setAwaitingColorSelection(false);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Handle mouse released events. If we were awaiting a color selection (press-and-hold),
     * choose the color under the release point if inside the menu, otherwise close the menu.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int screenWidth, int screenHeight, int paletteWidth) {
        if (!model.isColorMenuOpen() || !model.isAwaitingColorSelection()) return false;

        String[] dyeIds = Color.getAllDyeIds();
        int[] dyeColors = Color.getAllHexValues();
        int menuW = (dyeIds.length * 20) + 4;
        int menuX = model.getColorMenuAnchorX() - (menuW / 2) + 10;
        int menuY = model.getColorMenuAnchorY() - 30;

        if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + 26) {
            int index = (int) ((mouseX - menuX - 4) / 20);
            if (index >= 0 && index < dyeColors.length) {
                model.setCurrentColor(dyeColors[index]);
            }
        }

        // Close menu and clear awaiting flag regardless
        model.setColorMenuOpen(false);
        model.setAwaitingColorSelection(false);
        return true;
    }

    // Wrapper per render (diagram la fa, controller la chiama)
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight, int paletteWidth, Font font) {
        view.render(guiGraphics, mouseX, mouseY, screenWidth, screenHeight, paletteWidth, font,
                model.getCurrentTool(), model.getCurrentColor(), model.isColorMenuOpen(),
                model.getColorMenuAnchorX(), model.getColorMenuAnchorY());
    }
    
    public Tool getCurrentTool() { return model.getCurrentTool(); }
    public int getCurrentColor() { return model.getCurrentColor(); }
    public boolean isColorMenuOpen() { return model.isColorMenuOpen(); }
    public int getColorMenuAnchorX() { return model.getColorMenuAnchorX(); }
    public int getColorMenuAnchorY() { return model.getColorMenuAnchorY(); }
}