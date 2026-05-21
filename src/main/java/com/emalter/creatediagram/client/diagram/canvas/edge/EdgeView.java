package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.component.RecipeOutput;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * View for edge rendering in the canvas. Handles visual representation of edges,
 * connection lines, tooltips, and UI elements like the quantity slider.
 */
public class EdgeView {
    private final EdgeModel model;

    public EdgeView(EdgeModel model) {
        this.model = model;
    }

    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, double worldX, double worldY) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;

        for (DiagramEdge edge : model.getEdges()) {
            renderEdge(guiGraphics, edge, nodes, font);
        }

        // Render slider if open
        if (model.getEdgeWithOpenSlider() != null) {
            renderSlider(guiGraphics, font);
        }

        // Render dragging connection
        if (model.getDraggingFromNode() != null) {
            renderDraggingConnection(guiGraphics, nodes);
        }

        // Render port indicators on nodes
        renderPortIndicators(guiGraphics, nodes, font);
    }

    private void renderEdge(GuiGraphics guiGraphics, DiagramEdge edge, List<DiagramNode> nodes, Font font) {
        DiagramNode from = findNode(nodes, edge.fromNode());
        DiagramNode to = findNode(nodes, edge.toNode());

        if (from == null || to == null) return;

        int startX, startY;

        if (EmiHelper.isMachine(from.itemType())) {
            List<RecipeOutput> outputs = model.getDynamicOutputs(from, nodes);
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

        if (EmiHelper.isMachine(from.itemType())) {
            int[] midPoint = getBezierMidPoint(startX, startY, endX, endY);
            int badgeX = midPoint[0] - 8;
            int badgeY = midPoint[1] - 6;

            guiGraphics.fill(badgeX, badgeY, badgeX + 16, badgeY + 12, 0xFF222222);
            guiGraphics.renderOutline(badgeX, badgeY, 16, 12, edge == model.getEdgeWithOpenSlider() ? 0xFFFFAA00 : 0xFF888888);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(badgeX + 1, badgeY + 2, 10);
            guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
            guiGraphics.drawString(font, "x" + edge.amount(), 0, 0, 0xFFFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderSlider(GuiGraphics guiGraphics, Font font) {
        int sliderX = model.getSliderX();
        int sliderY = model.getSliderY();
        int sliderWidth = model.getSliderWidth();
        int sliderHeight = model.getSliderHeight();
        int sliderMin = model.getSliderMin();
        int sliderMax = model.getSliderMax();
        int sliderValue = model.getSliderValue();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);

        // Panel background
        guiGraphics.fill(sliderX - 10, sliderY - 15, sliderX + sliderWidth + 10, sliderY + sliderHeight + 10, 0xEE222222);
        guiGraphics.renderOutline(sliderX - 10, sliderY - 15, sliderWidth + 20, sliderHeight + 25, 0xFFFFAA00);

        // Dynamic text showing current quantity
        String label = "Quantity: " + sliderValue;
        int textW = font.width(label);
        guiGraphics.drawString(font, label, sliderX + (sliderWidth - textW) / 2, sliderY - 11, 0xFFFFFFFF, false);

        // Slider track (dark background)
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF111111);
        guiGraphics.renderOutline(sliderX, sliderY, sliderWidth, sliderHeight, 0xFF555555);

        // Golden fill (styled)
        float fillRatio = sliderMax > sliderMin ? (float)(sliderValue - sliderMin) / (sliderMax - sliderMin) : 0;
        int fillW = (int)(fillRatio * sliderWidth);
        guiGraphics.fill(sliderX + 1, sliderY + 1, sliderX + fillW, sliderY + sliderHeight - 1, 0xFFFFAA00);

        // White thumb
        int thumbX = sliderX + fillW;
        guiGraphics.fill(thumbX - 2, sliderY - 2, thumbX + 2, sliderY + sliderHeight + 2, 0xFFEEEEEE);
        guiGraphics.renderOutline(thumbX - 2, sliderY - 2, 4, sliderHeight + 4, 0xFF333333);

        guiGraphics.pose().popPose();
    }

    private void renderDraggingConnection(GuiGraphics guiGraphics, List<DiagramNode> nodes) {
        DiagramNode draggingNode = model.getDraggingFromNode();
        int slotIndex = model.getDraggingSlotIndex();

        int startX, startY;
        if (slotIndex == -1) {
            startX = draggingNode.x() + draggingNode.width() + 8;
            startY = draggingNode.y() + (draggingNode.height() / 2);
        } else {
            List<RecipeOutput> outputs = model.getDynamicOutputs(draggingNode, nodes);
            int totalOutHeight = outputs.size() * 18;
            startX = draggingNode.x() + draggingNode.width() + 18;
            startY = draggingNode.y() + (draggingNode.height() - totalOutHeight) / 2 + (slotIndex * 18) + 8;
        }
        drawBezierCurve(guiGraphics, startX, startY, model.getMouseWorldX(), model.getMouseWorldY(), 0x88FFAA00);
    }

    private void renderPortIndicators(GuiGraphics guiGraphics, List<DiagramNode> nodes, Font font) {
        for (DiagramNode node : nodes) {
            boolean isMach = EmiHelper.isMachine(node.itemType());
            int w = node.width();
            int h = node.height();

            if (isMach) {
                int portY = node.y() + (h/2) - 6;
                guiGraphics.fill(node.x() - 6, portY, node.x() + 2, portY + 12, 0xFF222222);
                guiGraphics.renderOutline(node.x() - 6, portY, 8, 12, 0xFFAAAAAA);

                List<RecipeOutput> outputs = model.getDynamicOutputs(node, nodes);
                int outX = node.x() + w + 2;
                int totalOutHeight = outputs.size() * 18;
                int startOutY = node.y() + (h - totalOutHeight) / 2;

                for (int i = 0; i < outputs.size(); i++) {
                    int outY = startOutY + (i * 18);
                    guiGraphics.fill(outX, outY, outX + 16, outY + 16, 0xFF111111);
                    guiGraphics.renderOutline(outX, outY, 16, 16, 0xFF444444);

                    EmiStack outStack = EmiHelper.getStack(outputs.get(i).itemId());
                    int amount = outputs.get(i).amount();

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(outX + 4, outY + 4, 0);
                    guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                    outStack.render(guiGraphics, 0, 0, 0f);

                    if (amount > 1) {
                        String qtyStr = outStack.getItemStack().isEmpty() ? amount + "mB" : String.valueOf(amount);
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(10, 10, 200);
                        guiGraphics.pose().scale(1.0f, 1.0f, 1.0f);
                        guiGraphics.drawString(font, qtyStr, 0, 0, 0xFFFFFFFF, true);
                        guiGraphics.pose().popPose();
                    }
                    guiGraphics.pose().popPose();
                }
            } else if (!node.itemType().equals("creatediagram:text_comment")) {
                guiGraphics.fill(node.x() + w, node.y() + (h/2) - 6, node.x() + w + 8, node.y() + (h/2) + 6, 0xFF222222);
                guiGraphics.renderOutline(node.x() + w, node.y() + (h/2) - 6, 8, 12, 0xFFAAAAAA);
            }
        }
    }

    public boolean renderTooltips(GuiGraphics gui, int mouseX, int mouseY, double worldX, double worldY, List<DiagramNode> nodes, Font font) {
        for (DiagramNode node : nodes) {
            if (EmiHelper.isMachine(node.itemType())) {
                List<RecipeOutput> outputs = model.getDynamicOutputs(node, nodes);
                int outX = node.x() + node.width() + 2;
                int totalOutHeight = outputs.size() * 18;
                int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                for (int i = 0; i < outputs.size(); i++) {
                    int outY = startOutY + (i * 18);
                    if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                        RecipeOutput out = outputs.get(i);
                        EmiStack outStack = EmiHelper.getStack(out.itemId());
                        List<Component> tooltip = new ArrayList<>(outStack.getTooltipText());

                        int chance = (int)(out.chance() * 100);
                        tooltip.add(Component.literal(chance + "%").withStyle(chance == 100 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));

                        gui.pose().pushPose();
                        gui.pose().translate(0, 0, 400);
                        gui.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                        gui.pose().popPose();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void drawBezierCurve(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int segments = 40;
        int dist = Math.abs(x2 - x1) / 2;
        int weight = Math.max(dist, 40);
        int cp1x = x1 + weight, cp1y = y1;
        int cp2x = x2 - weight, cp2y = y2;

        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float u = 1 - t;
            int px = (int) (u*u*u*x1 + 3*u*u*t*cp1x + 3*u*t*t*cp2x + t*t*t*x2);
            int py = (int) (u*u*u*y1 + 3*u*u*t*cp1y + 3*u*t*t*cp2y + t*t*t*y2);
            gui.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }

    private int[] getBezierMidPoint(int x1, int y1, int x2, int y2) {
        int dist = Math.abs(x2 - x1) / 2;
        int weight = Math.max(dist, 40);
        int cp1x = x1 + weight, cp1y = y1;
        int cp2x = x2 - weight, cp2y = y2;
        float t = 0.5f, u = 0.5f;
        int px = (int) (u*u*u*x1 + 3*u*u*t*cp1x + 3*u*t*t*cp2x + t*t*t*x2);
        int py = (int) (u*u*u*y1 + 3*u*u*t*cp1y + 3*u*t*t*cp2y + t*t*t*y2);
        return new int[]{px, py};
    }

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
    }
}







