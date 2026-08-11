package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.client.diagram.canvas.CanvasController;
import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class EdgeView {

    public EdgeView(EdgeModel model) {
    }

    public void render(GuiGraphics guiGraphics, EdgeModel model, CanvasController canvasController) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;

        for (DiagramEdge edge : model.getEdges()) {
            renderEdge(guiGraphics, edge, canvasController, font, model);
        }

        if (model.getEdgeWithOpenSlider() != null) {
            renderSlider(guiGraphics, font, model);
        }

        if (model.getDraggingFromNode() != null) {
            renderDraggingConnection(guiGraphics, model, canvasController);
        }
    }
    
    private void renderEdge(GuiGraphics guiGraphics, DiagramEdge edge, CanvasController canvasController, Font font, EdgeModel model) {
        DiagramNode from = canvasController.findNode(edge.fromNode());
        DiagramNode to = canvasController.findNode(edge.toNode());

        if (from == null || to == null) return;

        int startX, startY;
        if (canvasController.isMachine(from.itemType())) {
            List<OutputPort> outputs = canvasController.getDynamicOutputs(from);
            int outIndex = 0;
            for (int i = 0; i < outputs.size(); i++) {
                if (outputs.get(i).itemId().equals(edge.outputItem())) outIndex = i;
            }
            startX = from.x() + from.width() + 18;
            int totalOutHeight = outputs.size() * 18;
            startY = from.y() + (from.height() - totalOutHeight) / 2 + (outIndex * 18) + 8;
        } else {
            startX = from.x() + from.width() + 8;
            startY = from.y() + (from.height() / 2);
        }

        int endX = to.x() - 6;
        int endY = to.y() + (to.height() / 2);

        drawBezierCurve(guiGraphics, startX, startY, endX, endY, 0xFFFFAA00);

        // RIPRISTINATO: Il badge grafico compare solo in uscita da un macchinario
        if (canvasController.isMachine(from.itemType())) {
            int[] midPoint = getBezierMidPoint(startX, startY, endX, endY);
            int badgeX = midPoint[0] - 8;
            int badgeY = midPoint[1] - 6;

            boolean isInfinite = canvasController.isEdgeInfinite(edge);

            guiGraphics.fill(badgeX, badgeY, badgeX + 16, badgeY + 12, 0xFF222222);
            guiGraphics.renderOutline(badgeX, badgeY, 16, 12, edge == model.getEdgeWithOpenSlider() ? 0xFFFFAA00 : 0xFF888888);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(badgeX + 1, badgeY + 2, 10);
            guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);

            String textToDraw = isInfinite ? "∞" : ("x" + edge.amount());
            int textColor = isInfinite ? 0xFF55FFFF : 0xFFFFFFFF;

            guiGraphics.drawString(font, textToDraw, 0, 0, textColor, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderSlider(GuiGraphics guiGraphics, Font font, EdgeModel model) {
        int sliderX = model.getSliderX(), sliderY = model.getSliderY(), sliderWidth = model.getSliderWidth(), sliderHeight = model.getSliderHeight();
        int sliderMin = model.getSliderMin(), sliderMax = model.getSliderMax(), sliderValue = model.getSliderValue();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);

        guiGraphics.fill(sliderX - 10, sliderY - 15, sliderX + sliderWidth + 10, sliderY + sliderHeight + 10, 0xEE222222);
        guiGraphics.renderOutline(sliderX - 10, sliderY - 15, sliderWidth + 20, sliderHeight + 25, 0xFFFFAA00);

        String label = "Quantity: " + sliderValue;
        int textW = font.width(label);
        guiGraphics.drawString(font, label, sliderX + (sliderWidth - textW) / 2, sliderY - 11, 0xFFFFFFFF, false);

        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF111111);
        guiGraphics.renderOutline(sliderX, sliderY, sliderWidth, sliderHeight, 0xFF555555);

        float fillRatio = sliderMax > sliderMin ? (float)(sliderValue - sliderMin) / (sliderMax - sliderMin) : 0;
        int fillW = (int)(fillRatio * sliderWidth);
        guiGraphics.fill(sliderX + 1, sliderY + 1, sliderX + fillW, sliderY + sliderHeight - 1, 0xFFFFAA00);

        int thumbX = sliderX + fillW;
        guiGraphics.fill(thumbX - 2, sliderY - 2, thumbX + 2, sliderY + sliderHeight + 2, 0xFFEEEEEE);
        guiGraphics.renderOutline(thumbX - 2, sliderY - 2, 4, sliderHeight + 4, 0xFF333333);

        guiGraphics.pose().popPose();
    }

    private void renderDraggingConnection(GuiGraphics guiGraphics, EdgeModel model, CanvasController canvasController) {
        DiagramNode draggingNode = model.getDraggingFromNode();
        int slotIndex = model.getDraggingSlotIndex();

        int startX, startY;
        if (slotIndex == -1) {
            startX = draggingNode.x() + draggingNode.width() + 8;
            startY = draggingNode.y() + (draggingNode.height() / 2);
        } else {
            List<OutputPort> outputs = canvasController.getDynamicOutputs(draggingNode);
            int totalOutHeight = outputs.size() * 18;
            startX = draggingNode.x() + draggingNode.width() + 18;
            startY = draggingNode.y() + (draggingNode.height() - totalOutHeight) / 2 + (slotIndex * 18) + 8;
        }
        drawBezierCurve(guiGraphics, startX, startY, model.getMouseWorldX(), model.getMouseWorldY(), 0x88FFAA00);
    }

    public int[] getBezierMidPointForEdge(DiagramEdge edge, CanvasController canvasController) {
        DiagramNode from = canvasController.findNode(edge.fromNode());
        DiagramNode to = canvasController.findNode(edge.toNode());
        if (from == null || to == null) return null;

        int startX, startY;
        if (canvasController.isMachine(from.itemType())) {
            List<OutputPort> outputs = canvasController.getDynamicOutputs(from);
            int outIndex = 0;
            for (int i = 0; i < outputs.size(); i++) {
                if (outputs.get(i).itemId().equals(edge.outputItem())) outIndex = i;
            }
            startX = from.x() + from.width() + 18;
            int totalOutHeight = outputs.size() * 18;
            startY = from.y() + (from.height() - totalOutHeight) / 2 + (outIndex * 18) + 8;
        } else {
            startX = from.x() + from.width() + 8;
            startY = from.y() + (from.height() / 2);
        }

        int endX = to.x() - 6;
        int endY = to.y() + (to.height() / 2);
        return getBezierMidPoint(startX, startY, endX, endY);
    }

    private void drawBezierCurve(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2 && y1 == y2) return;
        float distance = (float) Math.hypot(x2 - x1, y2 - y1);
        int segments = Math.max(10, Math.min(30, (int) (distance / 8)));
        int distX = Math.abs(x2 - x1) / 2;
        int weight = Math.max(distX, 40);
        int cp1x = x1 + weight, cp1y = y1, cp2x = x2 - weight, cp2y = y2;
        int a = (color >> 24) & 255, r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = gui.pose().last().pose();
        float thickness = 1.5f;

        int lastX = x1, lastY = y1;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments, u = 1 - t;
            int px = (int) (u*u*u*x1 + 3*u*u*t*cp1x + 3*u*t*t*cp2x + t*t*t*x2);
            int py = (int) (u*u*u*y1 + 3*u*u*t*cp1y + 3*u*t*t*cp2y + t*t*t*y2);

            float dx = px - lastX, dy = py - lastY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                float nx = (dy / len) * thickness, ny = (-dx / len) * thickness;
                bufferbuilder.addVertex(matrix, lastX + nx, lastY + ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, lastX - nx, lastY - ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, px - nx, py - ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, px + nx, py + ny, 0.0F).setColor(r, g, b, a);
            }
            lastX = px; lastY = py;
        }
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow()); RenderSystem.disableBlend();
    }

    private int[] getBezierMidPoint(int x1, int y1, int x2, int y2) {
        int weight = Math.max(Math.abs(x2 - x1) / 2, 40);
        int cp1x = x1 + weight, cp1y = y1, cp2x = x2 - weight, cp2y = y2;
        float t = 0.5f, u = 0.5f;
        int px = (int) (u*u*u*x1 + 3*u*u*t*cp1x + 3*u*t*t*cp2x + t*t*t*x2);
        int py = (int) (u*u*u*y1 + 3*u*u*t*cp1y + 3*u*t*t*cp2y + t*t*t*y2);
        return new int[]{px, py};
    }
}