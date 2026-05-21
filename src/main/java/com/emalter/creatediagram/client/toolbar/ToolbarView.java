package com.emalter.creatediagram.client.toolbar;

import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ToolbarView {
    public ToolbarView() {}

    /*
    Toolbar rendering 
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight, int paletteWidth, Font font,
                       Tool currentTool, int currentColor, boolean colorMenuOpen, int colorMenuAnchorX, int colorMenuAnchorY) {
        Tool[] tools = Tool.values();
        int toolbarW = (tools.length * 24) + 4;
        int startX = paletteWidth + ((screenWidth - paletteWidth) / 2) - (toolbarW / 2);
        int startY = screenHeight - 35;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.fill(startX, startY, startX + toolbarW, startY + 26, 0xDD222222);
        guiGraphics.renderOutline(startX, startY, toolbarW, 26, 0xFF555555);

        for (int i = 0; i < tools.length; i++) {
            int btnX = startX + 4 + (i * 24);
            int btnY = startY + 3;
            boolean isHovered = mouseX >= btnX && mouseX <= btnX + 20 && mouseY >= btnY && mouseY <= btnY + 20;
            int bg = tools[i] == currentTool ? 0xFFFFAA00 : (isHovered ? 0xFF444444 : 0xFF333333);

            guiGraphics.fill(btnX, btnY, btnX + 20, btnY + 20, bg);
            guiGraphics.renderOutline(btnX, btnY, 20, 20, 0xFF888888);

            // Uses the EMI icon if available, otherwise uses the short label
            EmiStack stack = tools[i].getEmiStack();
            if (stack != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(btnX + 2, btnY + 2, 10);
                stack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            } else {
                String label = tools[i].getShortLabel();
                guiGraphics.drawString(font, label, btnX + 7, btnY + 6,
                        tools[i] == currentTool ? 0x000000 : 0xFFFFFF, false);
            }
        }

        // Render color popup (opens above the pressed button)
        if (colorMenuOpen) {
            int[] dyeColors = Color.getAllHexValues();
            String[] dyeIds = Color.getAllDyeIds();
            int menuW = (dyeIds.length * 20) + 4;
            int menuX = colorMenuAnchorX - (menuW / 2) + 10;
            int menuY = colorMenuAnchorY - 30;

            guiGraphics.fill(menuX, menuY, menuX + menuW, menuY + 26, 0xEE222222);
            guiGraphics.renderOutline(menuX, menuY, menuW, 26, 0xFFFFAA00);

            for (int i = 0; i < dyeIds.length; i++) {
                int btnX = menuX + 4 + (i * 20);
                int btnY = menuY + 5;
                boolean hovered = mouseX >= btnX && mouseX <= btnX + 16 && mouseY >= btnY && mouseY <= btnY + 16;
                if (currentColor == dyeColors[i] || hovered) {
                    guiGraphics.fill(btnX - 2, btnY - 2, btnX + 18, btnY + 18, hovered ? 0xAAFFFFFF : 0x55FFFFFF);
                }

                EmiStack dyeStack = EmiHelper.getStack(dyeIds[i]);
                if (dyeStack != null) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(btnX, btnY, 10);
                    dyeStack.render(guiGraphics, 0, 0, 0f);
                    guiGraphics.pose().popPose();
                }
            }
        }

        guiGraphics.pose().popPose();
    }


    // Controller Helper: returns the X position of the toolbar based on screen and palette width
    public int getToolbarStartX(int screenWidth, int paletteWidth) {
        Tool[] tools = Tool.values();
        int toolbarW = (tools.length * 24) + 4;
        return paletteWidth + ((screenWidth - paletteWidth) / 2) - (toolbarW / 2);
    }
    
    public int getToolbarStartY(int screenHeight) {
        return screenHeight - 35;
    }

    public int getToolbarWidth() {
        return (Tool.values().length * 24) + 4;
    }
}
