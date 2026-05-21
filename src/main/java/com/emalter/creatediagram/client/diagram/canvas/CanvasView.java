package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CanvasView {
    private final Font font;

    public CanvasView(Font font) {
        this.font = font;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int paletteWidth, CanvasModel model) {
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xFF111111);
        drawGrid(guiGraphics, screenWidth, screenHeight, model);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(model.offsetX, model.offsetY, 0);
        guiGraphics.pose().scale(model.zoom, model.zoom, 1.0f);

        double worldX = model.getWorldX(mouseX);
        double worldY = model.getWorldY(mouseY);

        for (CanvasModel.DiagramStroke stroke : model.strokes) {
            drawStroke(guiGraphics, stroke);
        }

        if (model.currentTool == com.emalter.creatediagram.client.toolbar.Tool.PEN && model.currentStrokePoints != null && model.currentStrokePoints.size() > 1) {
            drawStroke(guiGraphics, new CanvasModel.DiagramStroke(null, model.currentColor, model.currentStrokePoints));
        }
        if (model.currentTool == com.emalter.creatediagram.client.toolbar.Tool.LINE && model.isDrawingLine) {
            drawFastLine(guiGraphics, model.lineStartX, model.lineStartY, model.lineCurrentX, model.lineCurrentY, model.currentColor);
        }

        if (model.getEdgeController() != null) model.getEdgeController().render(guiGraphics, model.nodes, worldX, worldY);
        for (DiagramNode node : model.nodes) {
            drawNode(guiGraphics, node, model);
        }

        if (model.nodeWithOpenMenu != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 3);
            drawCatalystMenu(guiGraphics, model.nodeWithOpenMenu, model);
            guiGraphics.pose().popPose();
        }

        // Delegate text node rendering to TextController (which handles rendering + color menu)
        if (model.textController != null) {
            model.textController.render(guiGraphics, model.nodes, model.draggedNode != null ? model.draggedNode.id() : null);
        }

        if (model.activeAmountField != null && model.nodeWithActiveAmountField != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 4);
            model.activeAmountField.render(guiGraphics, (int) worldX, (int) worldY, partialTick);
            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();

        boolean tooltipDrawn = model.getEdgeController() != null && model.getEdgeController().renderTooltips(guiGraphics, mouseX, mouseY, worldX, worldY, model.nodes);
        if (!tooltipDrawn && model.nodeWithOpenMenu == null && model.activeAmountField == null && (model.textModel == null || !model.textModel.isEditing())) {
            for (DiagramNode node : model.nodes) {
                if (node.itemType().equals("creatediagram:text_comment")) continue;
                if (worldX >= node.x() && worldX <= node.x() + node.width() && worldY >= node.y() && worldY <= node.y() + node.height()) {
                    EmiStack nodeStack = EmiHelper.getStack(node.itemType());
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 100);
                    guiGraphics.renderTooltip(this.font, nodeStack.getTooltipText(), java.util.Optional.empty(), mouseX, mouseY);
                    guiGraphics.pose().popPose();
                    break;
                }
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        guiGraphics.drawString(this.font, "Zoom: " + Math.round(model.zoom * 100) + "%", 10, 10, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private void drawGrid(GuiGraphics guiGraphics, int screenWidth, int screenHeight, CanvasModel model) {
        int color = 0x33333333;
        float scaledGridSize = model.gridSize * model.zoom;
        int firstLineX = (int) (model.offsetX % scaledGridSize);
        if (firstLineX < 0) firstLineX += (int) scaledGridSize;
        int firstLineY = (int) (model.offsetY % scaledGridSize);
        if (firstLineY < 0) firstLineY += (int) scaledGridSize;

        for (float x = firstLineX; x < screenWidth; x += scaledGridSize) guiGraphics.fill((int) x, 0, (int) x + 1, screenHeight, color);
        for (float y = firstLineY; y < screenHeight; y += scaledGridSize) guiGraphics.fill(0, (int) y, screenWidth, (int) y + 1, color);
    }

    private void drawStroke(GuiGraphics guiGraphics, CanvasModel.DiagramStroke stroke) {
        if (stroke.points().size() < 2) return;
        for (int i = 0; i < stroke.points().size() - 1; i++) {
            int[] p1 = stroke.points().get(i);
            int[] p2 = stroke.points().get(i + 1);
            drawFastLine(guiGraphics, p1[0], p1[1], p2[0], p2[1], stroke.color());
        }
    }

    private void drawFastLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);
        gui.pose().pushPose();
        gui.pose().translate(x1, y1, 0);
        gui.pose().mulPose(com.mojang.math.Axis.ZP.rotation(angle));
        gui.fill(0, -1, (int) Math.ceil(length), 1, color);
        gui.pose().popPose();
    }

    private void drawNode(GuiGraphics guiGraphics, DiagramNode node, CanvasModel model) {
        int w = node.width();
        int h = node.height();
        boolean isInvalid = (node == model.draggedNode) && !model.isPositionValid(node.x(), node.y(), w, h, node.id());

        // Text nodes are rendered by TextController
        if (node.itemType().equals("creatediagram:text_comment")) {
            return;
        } else {
            int bgColor = isInvalid ? 0x88FF0000 : 0xFF333333;
            guiGraphics.fill(node.x(), node.y(), node.x() + w, node.y() + h, bgColor);
            guiGraphics.renderOutline(node.x(), node.y(), w, h, 0xFF888888);

            ResourceLocation resId = ResourceLocation.parse(node.itemType());
            String path = resId.getPath();
            boolean hasCatalyst = !EmiHelper.getValidCatalystsForMachine(node.itemType()).isEmpty();
            boolean isMachine = EmiHelper.isMachine(node.itemType());

            float baseW = 40f;
            float baseH = path.equals("mechanical_mixer") || path.equals("mechanical_press") ? 60f : 40f;
            float scaleX = w / baseW;
            float scaleY = h / baseH;
            float imgScale = Math.min(scaleX, scaleY) * 2.0f;

            if (path.equals("mechanical_mixer") || path.equals("mechanical_press")) {
                EmiStack basin = EmiHelper.getStack("create:basin");
                float bScale = (w / 40f) * 2.0f;
                float bx = node.x() + (w - (16 * bScale)) / 2f;
                float by = node.y() + (h - (16 * bScale)) / 2f + (10 * (h / 60f));

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(bx, by, 0);
                guiGraphics.pose().scale(bScale, bScale, 1.0f);
                basin.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            }

            EmiStack emiStack = EmiHelper.getStack(node.itemType());

            if (path.contains("crushing_wheel")) {
                float cwScale = (w / 80f) * 2.0f;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + (w / 2f) - (18 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
                guiGraphics.pose().scale(cwScale, cwScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + (w / 2f) + (2 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
                guiGraphics.pose().scale(cwScale, cwScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            } else {
                float ix = node.x() + (w - (16 * imgScale)) / 2f;
                float iy = node.y() + (h - (16 * imgScale)) / 2f - ((path.equals("mechanical_mixer") || path.equals("mechanical_press")) ? (10 * scaleY) : 0);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(ix, iy, 1);
                guiGraphics.pose().scale(imgScale, imgScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            }

            if (!isMachine) {
                float txtScale = Math.max(1.0f, Math.min(scaleX, scaleY) * 0.8f);
                String qtyText;
                if (emiStack.getItemStack().isEmpty()) {
                    qtyText = node.amount() + "mB";
                } else {
                    qtyText = String.valueOf(node.amount());
                }

                int textWidth = this.font.width(qtyText);
                int textColor = node.amount() > 1 ? 0xFFFFFF : 0x888888;

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + w - (4 * txtScale), node.y() + (4 * txtScale), 250);
                guiGraphics.pose().scale(txtScale, txtScale, 1.0f);
                guiGraphics.drawString(this.font, qtyText, -textWidth, 0, textColor, true);
                guiGraphics.pose().popPose();
            }

            if (hasCatalyst) {
                int slotX = node.x() + 4;
                int slotY = node.y() + h - 18;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 250);
                guiGraphics.fill(slotX, slotY, slotX + 14, slotY + 14, 0xFF111111);
                guiGraphics.renderOutline(slotX, slotY, 14, 14, 0xFFFFAA00);

                if (node.property() != null && !node.property().isEmpty() && node.property().contains(":")) {
                    EmiStack catStack = EmiHelper.getStack(node.property());
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(slotX + 3, slotY + 3, 10);
                    guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                    catStack.render(guiGraphics, 0, 0, 0f);
                    guiGraphics.pose().popPose();
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 10);
                    guiGraphics.drawString(this.font, "?", slotX + 4, slotY + 3, 0x888888, false);
                    guiGraphics.pose().popPose();
                }
                guiGraphics.pose().popPose();
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 250);
        guiGraphics.fill(node.x() + w - 6, node.y() + h - 2, node.x() + w - 2, node.y() + h, 0xFF999999);
        guiGraphics.fill(node.x() + w - 2, node.y() + h - 6, node.x() + w, node.y() + h, 0xFF999999);
        guiGraphics.pose().popPose();
    }

    private void drawCatalystMenu(GuiGraphics guiGraphics, java.util.UUID nodeId, CanvasModel model) {
        DiagramNode node = model.findNode(nodeId);
        if (node == null) return;
        List<String> options = EmiHelper.getValidCatalystsForMachine(node.itemType());
        int menuX = node.x() + node.width();
        int menuY = node.y();
        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (options.size() * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, options.size() * 20, 0xFFFFAA00);
        for (int i = 0; i < options.size(); i++) {
            EmiStack stack = EmiHelper.getStack(options.get(i));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    private void drawNodeColorMenu(GuiGraphics guiGraphics, java.util.UUID nodeId, CanvasModel model) {
        DiagramNode node = model.findNode(nodeId);
        if (node == null) return;
        Color[] colors = Color.values();
        int menuX = node.x() + node.width() + 20;
        int menuY = node.y() + (node.height() / 2) - 8;
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
}
