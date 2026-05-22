package com.emalter.creatediagram.client.diagram.canvas.text;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.client.diagram.Color;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.UUID;

public class TextController {
    private final TextModel model;
    private final TextView view;

    public TextController(TextModel model, TextView view) {
        this.model = model;
        this.view = view;
    }

    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, UUID draggedNodeId) {
        view.render(guiGraphics, nodes, model, draggedNodeId);
    }

    public boolean mouseClicked(double worldX, double worldY, List<DiagramNode> nodes, int button) {
        // 1. Se clicco fuori dal testo mentre scrivo, smetto di editare
        if (model.isEditing()) {
            UUID editingId = model.getEditingNodeId();
            DiagramNode editNode = findNode(nodes, editingId);
            if (editNode == null || !(worldX >= editNode.x() && worldX <= editNode.x() + editNode.width() &&
                    worldY >= editNode.y() && worldY <= editNode.y() + editNode.height())) {
                model.stopEditing();
            }
        }

        // I pulsanti UI (colore) funzionano solo col tasto sinistro
        if (button != 0) return false;

        // 2. Gestione del Menù Colori Aperto
        if (model.isColorMenuOpen()) {
            UUID nodeId = model.getNodeWithOpenColorMenu();
            DiagramNode node = findNode(nodes, nodeId);
            if (node != null) {
                Color[] colors = Color.values();
                int menuX = model.getColorMenuX();
                int menuY = model.getColorMenuY();

                // Se clicco su un colore, aggiorno la lista dei nodi
                if (worldX >= menuX && worldX <= menuX + 20 && worldY >= menuY && worldY <= menuY + (colors.length * 20)) {
                    int clickedIndex = (int) ((worldY - menuY) / 20);
                    if (clickedIndex >= 0 && clickedIndex < colors.length) {
                        int newColor = colors[clickedIndex].getHexValue();
                        int idx = nodes.indexOf(node);
                        // Applica la modifica
                        nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), node.property(), node.amount(), newColor, node.width(), node.height()));
                    }
                }
            }
            // Chiude il menù in ogni caso (sia se ho scelto un colore, sia se ho cliccato a vuoto)
            model.closeColorMenu();
            return true;
        }

        // 3. Controllo apertura Menù Colori tramite il bottoncino laterale
        for (DiagramNode node : nodes) {
            if (!node.itemType().equals("creatediagram:text_comment")) continue;

            int w = node.width();
            int h = node.height();
            int btnX = node.x() + w + 2;
            int btnY = node.y() + (h / 2) - 8;

            if (worldX >= btnX && worldX <= btnX + 16 && worldY >= btnY && worldY <= btnY + 16) {
                int menuX = node.x() + w + 20;
                int menuY = node.y() + (h / 2) - 8;
                model.openColorMenu(node.id(), menuX, menuY);
                return true; // Consuma il click
            }
        }
        
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return model.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return model.charTyped(codePoint, modifiers);
    }

    public String getEditedText() {
        return model.getCurrentText();
    }

    public UUID getEditingNodeId() {
        return model.getEditingNodeId();
    }

    public boolean isEditing() {
        return model.isEditing();
    }

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        if (id == null) return null;
        for (DiagramNode n : nodes) {
            if (n.id().equals(id)) return n;
        }
        return null;
    }
}