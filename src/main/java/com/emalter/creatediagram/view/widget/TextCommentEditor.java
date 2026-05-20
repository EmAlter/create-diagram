package com.emalter.creatediagram.view.widget;

import net.minecraft.SharedConstants;

import java.util.UUID;

public class TextCommentEditor {
    private UUID editingNodeId = null;
    private String currentText = "";

    // Avvia l'editing su un nodo specifico
    public void startEditing(UUID nodeId, String initialText) {
        this.editingNodeId = nodeId;
        this.currentText = initialText != null ? initialText : "";
    }

    // Ferma l'editing
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

    // Restituisce la stringa da disegnare, includendo il cursore lampeggiante
    public String getDisplayText() {
        if (editingNodeId == null) return "";
        return ((System.currentTimeMillis() / 500) % 2 == 0) ? currentText + "_" : currentText;
    }

    // Gestione dei tasti di controllo (Ritorna true se il tasto è stato consumato)
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingNodeId == null) return false;

        if (keyCode == 256) { // Tasto ESCAPE
            stopEditing();
            return true;
        }
        if (keyCode == 259 && !currentText.isEmpty()) { // Tasto BACKSPACE
            currentText = currentText.substring(0, currentText.length() - 1);
            return true;
        }
        if (keyCode == 257) { // Tasto ENTER
            currentText += "\n";
            return true;
        }
        // Consuma tasti freccia e altri comandi per non far muovere il giocatore in background
        return true;
    }

    // Gestione della digitazione delle lettere
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingNodeId != null) {
            // Accetta solo caratteri stampabili validi
            if (codePoint >= 32 && codePoint != 127) { // Spazio e sopra, escludendo DEL
                currentText += codePoint;
            }
            return true;
        }
        return false;
    }
}