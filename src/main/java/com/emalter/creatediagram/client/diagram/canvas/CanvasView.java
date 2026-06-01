package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class CanvasView {
    private final Font font;

    public CanvasView(Font font) {
        this.font = font;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int paletteWidth, CanvasController controller) {
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xFF111111);
        drawGrid(guiGraphics, screenWidth, screenHeight, controller);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(controller.getOffsetX(), controller.getOffsetY(), 0);
        guiGraphics.pose().scale(controller.getZoom(), controller.getZoom(), 1.0f);

        double worldX = controller.getWorldX(mouseX);
        double worldY = controller.getWorldY(mouseY);

        for (CanvasModel.DiagramStroke stroke : controller.getStrokes()) {
            drawStroke(guiGraphics, stroke);
        }

        if (controller.getCurrentTool() == com.emalter.creatediagram.client.toolbar.Tool.PEN && controller.getCurrentStrokePoints() != null && controller.getCurrentStrokePoints().size() > 1) {
            drawStroke(guiGraphics, new CanvasModel.DiagramStroke(null, controller.getCurrentColor(), controller.getCurrentStrokePoints()));
        }
        if (controller.getCurrentTool() == com.emalter.creatediagram.client.toolbar.Tool.LINE && controller.isDrawingLine()) {
            drawFastLine(guiGraphics, controller.getLineStartX(), controller.getLineStartY(), controller.getLineCurrentX(), controller.getLineCurrentY(), controller.getCurrentColor());
        }

        if (controller.getEdgeController() != null) controller.getEdgeController().render(guiGraphics, controller, worldX, worldY);
        
        for (DiagramNode node : controller.getNodes()) {
            boolean isInvalid = (node == controller.getDraggedNode()) && !controller.isPositionValid(node.x(), node.y(), node.width(), node.height(), node.id());
            controller.getNodeController().renderNodeAndPorts(guiGraphics, node, isInvalid, controller.getNodes(), controller.getEdges());
        }

        if (controller.getNodeWithOpenMenu() != null) {
            DiagramNode menuNode = controller.findNode(controller.getNodeWithOpenMenu());
            if (menuNode != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 250);
                controller.getNodeController().renderCatalystMenu(guiGraphics, menuNode);
                guiGraphics.pose().popPose();
            }
        }

        if (controller.getTextController() != null) {
            controller.getTextController().render(guiGraphics, controller.getNodes(), controller.getDraggedNode() != null ? controller.getDraggedNode().id() : null);
        }

        if (controller.getActiveAmountField() != null && controller.getNodeWithActiveAmountField() != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 4);
            controller.getActiveAmountField().render(guiGraphics, (int) worldX, (int) worldY, partialTick);
            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();

        boolean tooltipDrawn = false;
        if (controller.getNodeWithOpenMenu() == null && controller.getActiveAmountField() == null && (controller.getTextModel() == null || !controller.getTextModel().isEditing())) {
            for (DiagramNode node : controller.getNodes()) {
                if (node.itemType().equals("creatediagram:text_comment")) continue;

                // OTTIMIZZAZIONE SPAZIALE: Se il cursore è molto lontano dal nodo, ignora completamente
                // il calcolo delle porte e risparmia CPU preziose.
                if (worldX < node.x() - 20 || worldX > node.x() + node.width() + 40 ||
                        worldY < node.y() - 100 || worldY > node.y() + node.height() + 100) {
                    continue;
                }

                boolean isMachine = controller.isMachine(node.itemType());

                if (isMachine) {
                    List<OutputPort> ports = controller.getDynamicOutputs(node);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = ports.size() * 18;
                    int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                    for (int i = 0; i < ports.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            OutputPort port = ports.get(i);
                            EmiStack portStack = controller.getStack(port.itemId());
                            List<Component> tooltip = com.emalter.creatediagram.client.tooltip.TooltipManager.getOutputTooltip(portStack, port);
                            com.emalter.creatediagram.client.tooltip.TooltipManager.renderTooltip(guiGraphics, this.font, tooltip, mouseX, mouseY);
                            tooltipDrawn = true;
                            break;
                        }
                    }
                }

                if (tooltipDrawn) break;

                if (worldX >= node.x() && worldX <= node.x() + node.width() && worldY >= node.y() && worldY <= node.y() + node.height()) {
                    EmiStack nodeStack = controller.getStack(node.itemType());
                    List<Component> tooltip = com.emalter.creatediagram.client.tooltip.TooltipManager.getBaseTooltip(nodeStack);
                    com.emalter.creatediagram.client.tooltip.TooltipManager.renderTooltip(guiGraphics, this.font, tooltip, mouseX, mouseY);
                    break;
                }
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        guiGraphics.drawString(this.font, "Zoom: " + Math.round(controller.getZoom() * 100) + "%", 10, 10, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private void drawGrid(GuiGraphics guiGraphics, int screenWidth, int screenHeight, CanvasController controller) {
        int color = 0x33333333;
        float scaledGridSize = controller.getGridSize() * controller.getZoom();
        int firstLineX = (int) (controller.getOffsetX() % scaledGridSize);
        if (firstLineX < 0) firstLineX += (int) scaledGridSize;
        int firstLineY = (int) (controller.getOffsetY() % scaledGridSize);
        if (firstLineY < 0) firstLineY += (int) scaledGridSize;

        for (float x = firstLineX; x < screenWidth; x += scaledGridSize) guiGraphics.fill((int) x, 0, (int) x + 1, screenHeight, color);
        for (float y = firstLineY; y < screenHeight; y += scaledGridSize) guiGraphics.fill(0, (int) y, screenWidth, (int) y + 1, color);
    }

    private void drawStroke(GuiGraphics guiGraphics, CanvasModel.DiagramStroke stroke) {
        if (stroke.points().size() < 2) return;
        boolean hasDistance = false;
        for (int i = 0; i < stroke.points().size() - 1; i++) {
            if (stroke.points().get(i)[0] != stroke.points().get(i+1)[0] || stroke.points().get(i)[1] != stroke.points().get(i+1)[1]) {
                hasDistance = true; break;
            }
        }
        if (!hasDistance) return;

        int color = stroke.color();
        int a = (color >> 24) & 255, r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float thickness = 1.0f;

        for (int i = 0; i < stroke.points().size() - 1; i++) {
            int[] p1 = stroke.points().get(i), p2 = stroke.points().get(i + 1);
            float dx = p2[0] - p1[0], dy = p2[1] - p1[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                float nx = (dy / len) * thickness, ny = (-dx / len) * thickness;
                bufferbuilder.addVertex(matrix, p1[0] + nx, p1[1] + ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, p1[0] - nx, p1[1] - ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, p2[0] - nx, p2[1] - ny, 0.0F).setColor(r, g, b, a);
                bufferbuilder.addVertex(matrix, p2[0] + nx, p2[1] + ny, 0.0F).setColor(r, g, b, a);
            }
        }
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow()); RenderSystem.disableBlend();
    }

    private void drawFastLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2 && y1 == y2) return;
        int a = (color >> 24) & 255, r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = gui.pose().last().pose();
        float thickness = 1.0f;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len > 0) {
            float nx = (dy / len) * thickness, ny = (-dx / len) * thickness;
            bufferbuilder.addVertex(matrix, x1 + nx, y1 + ny, 0.0F).setColor(r, g, b, a);
            bufferbuilder.addVertex(matrix, x1 - nx, y1 - ny, 0.0F).setColor(r, g, b, a);
            bufferbuilder.addVertex(matrix, x2 - nx, y2 - ny, 0.0F).setColor(r, g, b, a);
            bufferbuilder.addVertex(matrix, x2 + nx, y2 + ny, 0.0F).setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow()); RenderSystem.disableBlend();
    }
}