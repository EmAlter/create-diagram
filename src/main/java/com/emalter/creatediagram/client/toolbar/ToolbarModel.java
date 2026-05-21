package com.emalter.creatediagram.client.toolbar;

import com.emalter.creatediagram.client.diagram.Color;

public class ToolbarModel {
    private Tool currentTool = Tool.PEN;
    private int currentColor = Color.WHITE.getHexValue();

    private boolean isColorMenuOpen = false;
    private int colorMenuAnchorX = 0;
    private int colorMenuAnchorY = 0;
    private boolean awaitingColorSelection = false;

    // getter / setter
    public Tool getCurrentTool() { return currentTool; }
    public void setCurrentTool(Tool currentTool) { this.currentTool = currentTool; }
    public int getCurrentColor() { return currentColor; }
    public void setCurrentColor(int currentColor) { this.currentColor = currentColor; }
    public boolean isColorMenuOpen() { return isColorMenuOpen; }
    public int getColorMenuAnchorX() { return colorMenuAnchorX; }
    public int getColorMenuAnchorY() { return colorMenuAnchorY; }
    public void setColorMenuOpen(boolean colorMenuOpen) { isColorMenuOpen = colorMenuOpen; }
    public void setColorMenuAnchor(int anchorX, int anchorY) {
        this.colorMenuAnchorX = anchorX;
        this.colorMenuAnchorY = anchorY;
    }
    public boolean isAwaitingColorSelection() { return awaitingColorSelection; }
    public void setAwaitingColorSelection(boolean awaiting) { this.awaitingColorSelection = awaiting; }
}