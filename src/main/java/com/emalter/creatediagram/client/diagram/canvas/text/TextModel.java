package com.emalter.creatediagram.client.diagram.canvas.text;

import java.util.UUID;

public class TextModel {
    private UUID editingNodeId = null;
    private String currentText = "";

    private UUID nodeWithOpenColorMenu = null;
    private int colorMenuX = 0, colorMenuY = 0;

    public void startEditing(UUID nodeId, String initialText) {
        this.editingNodeId = nodeId;
        this.currentText = initialText != null ? initialText : "";
    }

    public void stopEditing() {
        this.editingNodeId = null;
    }

    public boolean isEditing() { return this.editingNodeId != null; }
    public boolean isEditing(UUID nodeId) { return this.editingNodeId != null && this.editingNodeId.equals(nodeId); }
    public String getCurrentText() { return currentText; }
    public UUID getEditingNodeId() { return editingNodeId; }

    public String getDisplayText() {
        if (editingNodeId == null) return "";
        return ((System.currentTimeMillis() / 500) % 2 == 0) ? currentText + "_" : currentText;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingNodeId == null) return false;

        if (keyCode == 256) { stopEditing(); return true; } // ESC
        if (keyCode == 259 && !currentText.isEmpty()) { // BACKSPACE
            currentText = currentText.substring(0, currentText.length() - 1);
            return true;
        }
        if (keyCode == 257) { currentText += "\n"; return true; } // ENTER

        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (editingNodeId != null) {
            if (codePoint >= 32 && codePoint != 127) {
                currentText += codePoint;
            }
            return true;
        }
        return false;
    }

    // Color menu management
    public UUID getNodeWithOpenColorMenu() { return nodeWithOpenColorMenu; }
    public void openColorMenu(UUID nodeId, int x, int y) { this.nodeWithOpenColorMenu = nodeId; this.colorMenuX = x; this.colorMenuY = y; }
    public void closeColorMenu() { this.nodeWithOpenColorMenu = null; }
    public int getColorMenuX() { return colorMenuX; }
    public int getColorMenuY() { return colorMenuY; }
    public boolean isColorMenuOpen() { return nodeWithOpenColorMenu != null; }
}