package com.emalter.creatediagram.client.diagram.canvas.text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TextModel {
    private UUID editingNodeId = null;
    private String currentText = "";

    private UUID nodeWithOpenColorMenu = null;
    private int colorMenuX = 0, colorMenuY = 0;

    // --- STATO AUTOCOMPLETAMENTO ---
    private List<String> suggestions = new ArrayList<>();
    private int suggestionIndex = 0;
    private int activeWordStartIndex = -1;
    private String activeQuery = "";

    public void startEditing(UUID nodeId, String initialText) {
        this.editingNodeId = nodeId;
        this.currentText = initialText != null ? initialText : "";

        // Rimuove a capo accidentali alla fine della stringa che spingono il cursore giù
        while (this.currentText.endsWith("\n")) {
            this.currentText = this.currentText.substring(0, this.currentText.length() - 1);
        }

        clearSuggestions();
    }

    public void stopEditing() {
        this.editingNodeId = null;
        clearSuggestions();
    }

    public boolean isEditing() { return this.editingNodeId != null; }
    public boolean isEditing(UUID nodeId) { return this.editingNodeId != null && this.editingNodeId.equals(nodeId); }
    public String getCurrentText() { return currentText; }
    public UUID getEditingNodeId() { return editingNodeId; }

    public String getDisplayText() {
        if (editingNodeId == null) return "";
        return ((System.currentTimeMillis() / 500) % 2 == 0) ? currentText + "|" : currentText;
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingNodeId == null) return false;

        // --- GESTIONE MENU SUGGERIMENTI (Tasti direzionali e Conferma) ---
        if (!suggestions.isEmpty()) {
            if (keyCode == 258 || keyCode == 257) { // TAB o ENTER
                applySuggestion();
                return true;
            }
            if (keyCode == 264) { // FRECCIA GIÙ
                suggestionIndex = (suggestionIndex + 1) % suggestions.size();
                return true;
            }
            if (keyCode == 265) { // FRECCIA SU
                suggestionIndex = (suggestionIndex - 1 + suggestions.size()) % suggestions.size();
                return true;
            }
        }

        if (keyCode == 256) { stopEditing(); return true; } // ESC
        if (keyCode == 259 && !currentText.isEmpty()) { // BACKSPACE
            currentText = currentText.substring(0, currentText.length() - 1);
            updateSuggestions();
            return true;
        }

        // --- NUOVA GESTIONE INVIO (ENTER) ---
        if (keyCode == 257) {
            // Se il tasto SHIFT è premuto (il bit 1 di modifiers è a 1)
            if ((modifiers & 1) != 0) {
                currentText += "\n"; // Va a capo
                updateSuggestions();
            } else {
                stopEditing(); // Altrimenti salva ed esce dalla modalità di modifica
            }
            return true;
        }

        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (editingNodeId != null) {
            if (codePoint >= 32 && codePoint != 127) {
                currentText += codePoint;
                updateSuggestions();
            }
            return true;
        }
        return false;
    }

    // --- LOGICA AUTOCOMPLETAMENTO DEI NOMI GIOCATORE ---
    private void updateSuggestions() {
        int lastSpace = currentText.lastIndexOf(' ');
        int lastNewline = currentText.lastIndexOf('\n');
        activeWordStartIndex = Math.max(lastSpace, lastNewline) + 1;

        if (activeWordStartIndex >= currentText.length()) {
            clearSuggestions();
            return;
        }

        activeQuery = currentText.substring(activeWordStartIndex);

        if (activeQuery.startsWith("@")) {
            String q = activeQuery.substring(1).toLowerCase();
            // Cerca tra i giocatori connessi al server
            var conn = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (conn != null) {
                suggestions = conn.getOnlinePlayers().stream()
                        .map(info -> info.getProfile().getName())
                        .filter(java.util.Objects::nonNull)
                        .filter(name -> name.toLowerCase().startsWith(q))
                        .map(name -> "@" + name)
                        .toList();
                suggestionIndex = 0;
            }
        } else {
            clearSuggestions();
        }
    }

    private void applySuggestion() {
        String chosen = suggestions.get(suggestionIndex);
        currentText = currentText.substring(0, activeWordStartIndex) + chosen + " ";
        clearSuggestions();
    }

    private void clearSuggestions() {
        suggestions = new ArrayList<>();
        suggestionIndex = 0;
        activeWordStartIndex = -1;
    }

    public List<String> getSuggestions() { return suggestions; }
    public int getSuggestionIndex() { return suggestionIndex; }

    // Color menu management
    public UUID getNodeWithOpenColorMenu() { return nodeWithOpenColorMenu; }
    public void openColorMenu(UUID nodeId, int x, int y) { this.nodeWithOpenColorMenu = nodeId; this.colorMenuX = x; this.colorMenuY = y; }
    public void closeColorMenu() { this.nodeWithOpenColorMenu = null; }
    public int getColorMenuX() { return colorMenuX; }
    public int getColorMenuY() { return colorMenuY; }
    public boolean isColorMenuOpen() { return nodeWithOpenColorMenu != null; }
}