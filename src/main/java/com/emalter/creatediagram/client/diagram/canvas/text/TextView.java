package com.emalter.creatediagram.client.diagram.canvas.text;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.client.diagram.Color;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.emalter.creatediagram.logic.integration.ModIntegration;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
import java.util.UUID;

public class TextView {
    private final Font font;

    public TextView(Font font) {
        this.font = font;
    }

    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, TextModel model, UUID draggedNodeId) {
        for (DiagramNode node : nodes) {
            if (node.itemType().equals("creatediagram:text_comment")) {
                renderTextNode(guiGraphics, node, model, draggedNodeId);
            }
        }

        if (model.isColorMenuOpen()) {
            UUID nodeId = model.getNodeWithOpenColorMenu();
            DiagramNode node = findNode(nodes, nodeId);
            if (node != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 250);
                renderColorMenu(guiGraphics, node, model);
                guiGraphics.pose().popPose();
            }
        }
    }

    private void renderTextNode(GuiGraphics guiGraphics, DiagramNode node, TextModel model, UUID draggedNodeId) {
        int w = node.width();
        int h = node.height();
        boolean isEditingThis = model.isEditing(node.id());
        boolean isInvalid = (node.id().equals(draggedNodeId));

        int bgColor = isInvalid ? 0x88FF0000 : (node.color() | 0xFF000000);
        guiGraphics.fill(node.x(), node.y(), node.x() + w, node.y() + h, bgColor);
        guiGraphics.renderOutline(node.x(), node.y(), w, h, 0xFF000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1);

        int textX = node.x() + 4;
        int textY = node.y() + 4;

        if (isEditingThis) {
            guiGraphics.drawWordWrap(font, FormattedText.of(model.getDisplayText()), textX, textY, w - 8, 0xFF000000);
        } else {
            // Disegna il testo normale quando l'editing è finito
            guiGraphics.drawWordWrap(font, FormattedText.of(node.property()), textX, textY, w - 8, 0xFF000000);
        }

        int btnX = node.x() + w + 2;
        int btnY = node.y() + (h / 2) - 8;
        guiGraphics.fill(btnX, btnY, btnX + 16, btnY + 16, 0xFF111111);
        guiGraphics.renderOutline(btnX, btnY, 16, 16, 0xFF444444);
        guiGraphics.fill(btnX + 4, btnY + 4, btnX + 12, btnY + 12, node.color() | 0xFF000000);

        guiGraphics.pose().popPose();
    }

    private void renderColorMenu(GuiGraphics guiGraphics, DiagramNode node, TextModel model) {
        Color[] colors = Color.values();
        int menuX = model.getColorMenuX();
        int menuY = model.getColorMenuY();

        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (colors.length * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, colors.length * 20, 0xFFFFAA00);

        for (int i = 0; i < colors.length; i++) {
            EmiStack stack = ModIntegration.get().getStack(colors[i].getDyeId());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        if (id == null) return null;
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
    }
}