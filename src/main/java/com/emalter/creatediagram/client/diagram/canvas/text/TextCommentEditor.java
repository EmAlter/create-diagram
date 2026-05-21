package com.emalter.creatediagram.client.diagram.canvas.text;

import java.util.UUID;

public class TextCommentEditor {
    private UUID editingNodeId = null;
    private String currentText = "";
    /**
     * Manages in-place text editing for a node. Stores the node id currently being edited and the working text.
     */
    /**
     * Begin editing a node with an optional initial text.
     */
    public void startEditing(UUID nodeId, String initialText) {
        this.editingNodeId = nodeId;
        this.currentText = initialText != null ? initialText : "";
    }

    /**
     * Stop the current editing session.
     */
    public void stopEditing() {
        this.editingNodeId = null;
    }

    public boolean isEditing() {
        return this.editingNodeId != null;
    }

    public boolean isEditing(UUID nodeId) {
        return this.editingNodeId != null && this.editingNodeId.equals(nodeId);
    }

    public String getCurrentText() {
        return currentText;
    }

    public UUID getEditingNodeId() {
        return editingNodeId;
    }

    /**
     * Returns the string to display, including a blinking cursor while editing.
     */
    public String getDisplayText() {
        if (editingNodeId == null) return "";
        return ((System.currentTimeMillis() / 500) % 2 == 0) ? currentText + "_" : currentText;
    }

    /**
     * Handles control key presses while editing. Returns true if the key was consumed.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingNodeId == null) return false;

        if (keyCode == 256) { // ESCAPE
            stopEditing();
            return true;
        }
        if (keyCode == 259 && !currentText.isEmpty()) { // BACKSPACE
            currentText = currentText.substring(0, currentText.length() - 1);
            return true;
        }
        if (keyCode == 257) { // ENTER
            currentText += "\n";
            return true;
        }
        // Consume arrow keys and other commands to prevent player movement while typing
        return true;
    }

    /**
     * Handles typed characters and appends printable characters to the current text.
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingNodeId != null) {
            // Accept only printable characters (space and above, excluding DEL)
            if (codePoint >= 32 && codePoint != 127) {
                currentText += codePoint;
            }
            return true;
        }
        return false;
    }
}