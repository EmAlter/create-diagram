package com.emalter.creatediagram.client.widget;

import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import com.emalter.creatediagram.client.toolbar.Tool;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Small toolbar providing drawing tools (pen, line, eraser) and a color picker popup.
 */
public class DrawingToolbar {
    private final String[] TOOLS = Tool.getAllToolsIDs();
    private Tool currentTool = Tool.PEN;

    private int currentDrawingColor = Color.WHITE.getHexValue();
    private final String[] DYE_IDS = Color.getAllDyeIds();
    private final int[] DYE_COLORS = Color.getAllHexValues();

    private boolean isColorMenuOpen = false;
    private int colorMenuAnchorX = 0;
    private int colorMenuAnchorY = 0;

    public Tool getCurrentTool() { return currentTool; }
    public int getCurrentColor() { return currentDrawingColor; }
    public boolean isColorMenuOpen() { return isColorMenuOpen; }

    /**
     * Renders the toolbar and its popup color menu when open.
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight, int paletteWidth, Font font) {
        Tool[] tools = Tool.values();
        int toolbarW = (tools.length * 24) + 4;

        // Compute centered area: palette + half of remaining space
        int startX = paletteWidth + ((screenWidth - paletteWidth) / 2) - (toolbarW / 2);
        int startY = screenHeight - 35;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300); // Always render toolbar on top

        // Draw toolbar background
        guiGraphics.fill(startX, startY, startX + toolbarW, startY + 26, 0xDD222222);
        guiGraphics.renderOutline(startX, startY, toolbarW, 26, 0xFF555555);

        for (int i = 0; i < tools.length; i++) {
            int btnX = startX + 4 + (i * 24);
            int btnY = startY + 3;
            boolean isHovered = mouseX >= btnX && mouseX <= btnX + 20 && mouseY >= btnY && mouseY <= btnY + 20;
            int bg = tools[i] == currentTool ? 0xFFFFAA00 : (isHovered ? 0xFF444444 : 0xFF333333);

            guiGraphics.fill(btnX, btnY, btnX + 20, btnY + 20, bg);
            guiGraphics.renderOutline(btnX, btnY, 20, 20, 0xFF888888);
            String label = switch (tools[i]) { case PEN -> "P"; case LINE -> "L"; case ERASER -> "E"; };
            guiGraphics.drawString(font, label, btnX + 7, btnY + 6, tools[i] == currentTool ? 0x000000 : 0xFFFFFF, false);
        }

        // Render color popup (opens above the pressed button)
        if (isColorMenuOpen) {
            int menuW = (DYE_IDS.length * 20) + 4;
            int menuX = colorMenuAnchorX - (menuW / 2) + 10;
            int menuY = colorMenuAnchorY - 30; // 30 pixels above the toolbar

            guiGraphics.fill(menuX, menuY, menuX + menuW, menuY + 26, 0xEE222222);
            guiGraphics.renderOutline(menuX, menuY, menuW, 26, 0xFFFFAA00);

            for (int i = 0; i < DYE_IDS.length; i++) {
                int btnX = menuX + 4 + (i * 20);
                int btnY = menuY + 5;

                boolean hovered = mouseX >= btnX && mouseX <= btnX + 16 && mouseY >= btnY && mouseY <= btnY + 16;
                if (currentDrawingColor == DYE_COLORS[i] || hovered) {
                    guiGraphics.fill(btnX - 2, btnY - 2, btnX + 18, btnY + 18, hovered ? 0xAAFFFFFF : 0x55FFFFFF);
                }

                EmiStack dyeStack = EmiHelper.getStack(DYE_IDS[i]);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(btnX, btnY, 10);
                dyeStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            }
        }
        guiGraphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int screenHeight, int paletteWidth) {
        Tool[] tools = Tool.values();
        int toolbarW = (tools.length * 24) + 4;
        int startX = paletteWidth + ((screenWidth - paletteWidth) / 2) - (toolbarW / 2);
        int startY = screenHeight - 35;

        if (mouseX >= startX && mouseX <= startX + toolbarW && mouseY >= startY && mouseY <= startY + 26) {
            int index = (int) ((mouseX - startX - 4) / 24);
            if (index >= 0 && index < tools.length) {
                currentTool = tools[index];
                // HOLD-TO-OPEN: clicking Pen or Line opens the color menu
                if (currentTool == Tool.PEN || currentTool == Tool.LINE) {
                    isColorMenuOpen = true;
                    colorMenuAnchorX = startX + 4 + (index * 24);
                    colorMenuAnchorY = startY;
                } else {
                    isColorMenuOpen = false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY) {
        if (isColorMenuOpen) {
            int menuW = (DYE_IDS.length * 20) + 4;
            int menuX = colorMenuAnchorX - (menuW / 2) + 10;
            int menuY = colorMenuAnchorY - 30;

            // If the mouse release occurred over a color, apply it
            if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + 26) {
                int index = (int) ((mouseX - menuX - 4) / 20);
                if (index >= 0 && index < DYE_IDS.length) {
                    currentDrawingColor = DYE_COLORS[index];
                }
            }
            isColorMenuOpen = false;
            return true; // Consumiamo l'evento del rilascio
        }
        return false;
    }
}