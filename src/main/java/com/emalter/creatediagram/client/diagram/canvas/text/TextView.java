package com.emalter.creatediagram.client.diagram.canvas.text;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
import java.util.UUID;

/**
 * View for text node rendering in the canvas. Handles all visual representation
 * of text nodes including background, text content, color button, and color menu.
 */
public class TextView {
    private final Font font;

    public TextView(Font font) {
        this.font = font;
    }

    /**
     * Renders all text nodes on the canvas.
     */
    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, TextModel model, UUID draggedNodeId) {
        for (DiagramNode node : nodes) {
            if (node.itemType().equals("creatediagram:text_comment")) {
                renderTextNode(guiGraphics, node, model, draggedNodeId);
            }
        }

        // Render color menu if open
        if (model.isColorMenuOpen()) {
            UUID nodeId = model.getNodeWithOpenColorMenu();
            DiagramNode node = findNode(nodes, nodeId);
            if (node != null) {
                renderColorMenu(guiGraphics, node, model);
            }
        }
    }

    /**
     * Renders a single text node: background, text content, color button.
     */
    private void renderTextNode(GuiGraphics guiGraphics, DiagramNode node, TextModel model, UUID draggedNodeId) {
        int w = node.width();
        int h = node.height();
        boolean isInvalid = (node.id().equals(draggedNodeId));  // Check if being dragged to invalid position

        int bgColor = isInvalid ? 0x88FF0000 : (node.color() | 0xFF000000);
        guiGraphics.fill(node.x(), node.y(), node.x() + w, node.y() + h, bgColor);
        guiGraphics.renderOutline(node.x(), node.y(), w, h, 0xFF000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1);

        // Display text with cursor if editing
        String displayText = model.isEditing(node.id()) ? model.getDisplayText() : node.property();
        guiGraphics.drawWordWrap(this.font, FormattedText.of(displayText), node.x() + 4, node.y() + 4, w - 8, 0xFF000000);

        // Render color button
        int btnX = node.x() + w + 2;
        int btnY = node.y() + (h / 2) - 8;
        guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0xFF111111);
        guiGraphics.renderOutline(btnX, btnY, 16, 16, 0xFF444444);
        guiGraphics.fill(btnX + 4, btnY + 4, btnX + 12, btnY + 12, node.color() | 0xFF000000);

        guiGraphics.pose().popPose();
    }

    /**
     * Renders the color selection menu for a text node.
     */
    private void renderColorMenu(GuiGraphics guiGraphics, DiagramNode node, TextModel model) {
        Color[] colors = Color.values();
        int menuX = model.getColorMenuX();
        int menuY = model.getColorMenuY();

        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (colors.length * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, colors.length * 20, 0xFFFFAA00);

        for (int i = 0; i < colors.length; i++) {
            EmiStack stack = EmiHelper.getStack(colors[i].getDyeId());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Convenience method to find a node by ID in a list.
     */
    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        if (id == null) return null;
        for (DiagramNode n : nodes) {
            if (n.id().equals(id)) return n;
        }
        return null;
    }
}

