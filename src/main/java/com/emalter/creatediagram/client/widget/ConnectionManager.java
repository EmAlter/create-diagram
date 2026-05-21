package com.emalter.creatediagram.client.widget;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.logic.RecipeEngine;
import com.emalter.creatediagram.component.RecipeOutput;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Manages diagram connections (edges) between nodes and provides rendering and interaction helpers.
 * Responsible for computing dynamic outputs for machines, balancing edges, and handling UI interactions
 * such as dragging connections and a quantity slider.
 *
 * @deprecated This class has been replaced by the MVC architecture.
 * Use {@link com.emalter.creatediagram.client.diagram.canvas.edge.EdgeController} instead,
 * with {@link com.emalter.creatediagram.client.diagram.canvas.edge.EdgeModel} and
 * {@link com.emalter.creatediagram.client.diagram.canvas.edge.EdgeView} for a clean separation of concerns.
 * This class will be removed in a future version.
 */
@Deprecated(since = "0.0.2", forRemoval = true)
public class ConnectionManager {
    private List<DiagramEdge> edges = new ArrayList<>();
    private final RecipeEngine recipeEngine = new RecipeEngine();

    private DiagramNode draggingFromNode = null;
    private String draggingOutputItem = null;
    private int draggingSlotIndex = 0;
    private int mouseWorldX = 0;
    private int mouseWorldY = 0;

    // Slider state (visual quantity slider shown when interacting with an edge)
    private DiagramEdge edgeWithOpenSlider = null;
    private int sliderX = 0;
    private int sliderY = 0;
    private int sliderWidth = 100;
    private int sliderHeight = 10;
    private int sliderMin = 1;
    private int sliderMax = 1;
    private int sliderValue = 1;
    private boolean isDraggingSlider = false;


    public void addEdge(DiagramEdge edge) {
        if (!edges.contains(edge)) this.edges.add(edge);
    }

    public List<RecipeOutput> getDynamicOutputs(DiagramNode node, List<DiagramNode> allNodes) {
        return getDynamicOutputs(node, allNodes, new HashSet<>());
    }

    private List<RecipeOutput> getDynamicOutputs(DiagramNode node, List<DiagramNode> allNodes, Set<UUID> visited) {
        if (visited.contains(node.id())) return List.of();
        visited.add(node.id());

        String id = node.itemType();
        Map<String, Integer> inputs = getIncomingItems(node, allNodes, visited);
        return recipeEngine.getOutputs(id, node.property(), inputs);
    }

    private Map<String, Integer> getIncomingItems(DiagramNode machine, List<DiagramNode> allNodes, Set<UUID> visited) {
        Map<String, Integer> incoming = new HashMap<>();

        for (DiagramEdge edge : edges) {
            if (edge.toNode().equals(machine.id())) {
                DiagramNode fromNode = findNode(allNodes, edge.fromNode());
                if (fromNode != null) {
                    String itemId;
                    int amountToAdd = 0;

                    if (!EmiHelper.isMachine(fromNode.itemType())) {
                        itemId = fromNode.itemType();
                        amountToAdd = fromNode.amount();
                    } else {
                        itemId = edge.outputItem();
                        amountToAdd = edge.amount();
                    }
                    if (amountToAdd > 0) incoming.put(itemId, incoming.getOrDefault(itemId, 0) + amountToAdd);
                }
            }
        }
        return incoming;
    }

    public void cleanOrphanEdges(UUID fromNodeId, List<DiagramNode> allNodes) {
        DiagramNode fromNode = findNode(allNodes, fromNodeId);
        if (fromNode == null) return;

        if (!EmiHelper.isMachine(fromNode.itemType())) {
            balanceEdges(fromNodeId, fromNode.itemType(), allNodes, null);
            return;
        }

        List<RecipeOutput> validOutputs = getDynamicOutputs(fromNode, allNodes);

        edges.removeIf(edge -> {
            if (edge.fromNode().equals(fromNodeId)) {
                boolean isValid = false;
                for (RecipeOutput out : validOutputs) {
                    if (out.itemId().equals(edge.outputItem())) {
                        isValid = true;
                        break;
                    }
                }
                // Close the slider if the edge being inspected was removed
                if (!isValid && edge.equals(edgeWithOpenSlider)) edgeWithOpenSlider = null;
                return !isValid;
            }
            return false;
        });

        for (RecipeOutput out : validOutputs) {
            balanceEdges(fromNodeId, out.itemId(), allNodes, null);
        }
    }

    public void triggerCascadeClean(List<DiagramNode> nodes) {
        for (DiagramNode node : nodes) {
            cleanOrphanEdges(node.id(), nodes);
        }
    }

    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, double worldX, double worldY) {
        this.mouseWorldX = (int) worldX;
        this.mouseWorldY = (int) worldY;
        Font font = net.minecraft.client.Minecraft.getInstance().font;

        triggerCascadeClean(nodes);

        for (DiagramEdge edge : edges) {
            DiagramNode from = findNode(nodes, edge.fromNode());
            DiagramNode to = findNode(nodes, edge.toNode());

            if (from != null && to != null) {
                int startX, startY;

                if (EmiHelper.isMachine(from.itemType())) {
                    List<RecipeOutput> outputs = getDynamicOutputs(from, nodes);
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
                    guiGraphics.renderOutline(badgeX, badgeY, 16, 12, edge == edgeWithOpenSlider ? 0xFFFFAA00 : 0xFF888888);

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(badgeX + 1, badgeY + 2, 10);
                    guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
                    guiGraphics.drawString(font, "x" + edge.amount(), 0, 0, 0xFFFFFFFF, true);
                    guiGraphics.pose().popPose();
                }
            }
        }

        // Slider rendering (quantity selector) if open
        if (edgeWithOpenSlider != null) {
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

        if (draggingFromNode != null) {
            int startX, startY;
            if (draggingSlotIndex == -1) {
                startX = draggingFromNode.x() + draggingFromNode.width() + 8;
                startY = draggingFromNode.y() + (draggingFromNode.height() / 2);
            } else {
                List<RecipeOutput> outputs = getDynamicOutputs(draggingFromNode, nodes);
                int totalOutHeight = outputs.size() * 18;
                startX = draggingFromNode.x() + draggingFromNode.width() + 18;
                startY = draggingFromNode.y() + (draggingFromNode.height() - totalOutHeight) / 2 + (draggingSlotIndex * 18) + 8;
            }
            drawBezierCurve(guiGraphics, startX, startY, mouseWorldX, mouseWorldY, 0x88FFAA00);
        }

        for (DiagramNode node : nodes) {
            boolean isMach = EmiHelper.isMachine(node.itemType());
            int w = node.width();
            int h = node.height();

            if (isMach) {
                int portY = node.y() + (h/2) - 6;
                guiGraphics.fill(node.x() - 6, portY, node.x() + 2, portY + 12, 0xFF222222);
                guiGraphics.renderOutline(node.x() - 6, portY, 8, 12, 0xFFAAAAAA);

                List<RecipeOutput> outputs = getDynamicOutputs(node, nodes);
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
                List<RecipeOutput> outputs = getDynamicOutputs(node, nodes);
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

    private int getRemainingOutputAmount(DiagramNode fromNode, String outputItem, List<DiagramNode> allNodes) {
        int maxAllowed = 1;
        if (EmiHelper.isMachine(fromNode.itemType())) {
            for (RecipeOutput out : getDynamicOutputs(fromNode, allNodes)) {
                if (out.itemId().equals(outputItem)) { maxAllowed = out.amount(); break; }
            }
        } else {
            maxAllowed = fromNode.amount();
        }

        int used = 0;
        for (DiagramEdge edge : edges) {
            if (edge.fromNode().equals(fromNode.id()) && edge.outputItem().equals(outputItem)) used += edge.amount();
        }
        return Math.max(0, maxAllowed - used);
    }

    private void balanceEdges(UUID fromNodeId, String outputItem, List<DiagramNode> allNodes, DiagramEdge preservedEdge) {
        DiagramNode fromNode = findNode(allNodes, fromNodeId);
        if (fromNode == null || !EmiHelper.isMachine(fromNode.itemType())) return;

        int maxAllowed = 1;
        for (RecipeOutput out : getDynamicOutputs(fromNode, allNodes)) {
            if (out.itemId().equals(outputItem)) { maxAllowed = out.amount(); break; }
        }

        int totalUsed = 0;
        List<DiagramEdge> affectedEdges = new ArrayList<>();
        for (DiagramEdge edge : edges) {
            if (edge.fromNode().equals(fromNodeId) && edge.outputItem().equals(outputItem)) {
                totalUsed += edge.amount();
                affectedEdges.add(edge);
            }
        }

        if (totalUsed > maxAllowed) {
            for (int i = affectedEdges.size() - 1; i >= 0; i--) {
                DiagramEdge target = affectedEdges.get(i);
                if (target.equals(preservedEdge)) continue;

                edges.remove(target);
                totalUsed -= target.amount();
                if (totalUsed <= maxAllowed) break;
            }
        }
    }

    private void updateSliderValue(double worldX) {
        if (sliderMax <= sliderMin) {
            sliderValue = sliderMin;
            return;
        }
        double ratio = (worldX - sliderX) / (double) sliderWidth;
        ratio = Math.max(0, Math.min(1, ratio));
        sliderValue = sliderMin + (int) Math.round(ratio * (sliderMax - sliderMin));
    }

    public boolean mouseDragged(double worldX, double worldY, int button) {
        // If the slider is being dragged, update its value in real time
        if (isDraggingSlider && edgeWithOpenSlider != null) {
            updateSliderValue(worldX);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double worldX, double worldY, List<DiagramNode> nodes, int button) {
        if (edgeWithOpenSlider != null) {
            if (worldX >= sliderX - 10 && worldX <= sliderX + sliderWidth + 10 && worldY >= sliderY - 15 && worldY <= sliderY + sliderHeight + 10) {
                isDraggingSlider = true;
                updateSliderValue(worldX);
                return true;
                } else {
                // Click outside: close the popup
                edgeWithOpenSlider = null;
                isDraggingSlider = false;
                return true;
            }
        }

        if (button == 0) {
            for (DiagramEdge edge : edges) {
                DiagramNode from = findNode(nodes, edge.fromNode());
                DiagramNode to = findNode(nodes, edge.toNode());
                if (from != null && to != null && EmiHelper.isMachine(from.itemType())) {
                    int startX, startY;
                    List<RecipeOutput> outputs = getDynamicOutputs(from, nodes);
                    int outIndex = 0;
                    for (int i = 0; i < outputs.size(); i++) if (outputs.get(i).itemId().equals(edge.outputItem())) outIndex = i;
                    startX = from.x() + from.width() + 18;
                    int totalOutHeight = outputs.size() * 18;
                    startY = from.y() + (from.height() - totalOutHeight) / 2 + (outIndex * 18) + 8;

                    int endX = to.x() - 6;
                    int endY = to.y() + (to.height() / 2);

                    int[] mid = getBezierMidPoint(startX, startY, endX, endY);
                    if (worldX >= mid[0] - 8 && worldX <= mid[0] + 8 && worldY >= mid[1] - 6 && worldY <= mid[1] + 6) {

                        int maxOutput = 1;
                        for (RecipeOutput out : outputs) if (out.itemId().equals(edge.outputItem())) { maxOutput = out.amount(); break; }

                        int maxOptions = getRemainingOutputAmount(from, edge.outputItem(), nodes) + edge.amount();

                        sliderMin = 1;
                        sliderMax = maxOptions;
                        sliderValue = edge.amount();
                        sliderWidth = 100;
                        sliderX = mid[0] - sliderWidth / 2;
                        sliderY = mid[1] + 15;
                        edgeWithOpenSlider = edge;
                        return true;
                    }
                }
            }

            for (DiagramNode node : nodes) {
                if (node.itemType().equals("creatediagram:text_comment")) continue;

                if (EmiHelper.isMachine(node.itemType())) {
                    List<RecipeOutput> outputs = getDynamicOutputs(node, nodes);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                    for (int i = 0; i < outputs.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            if (getRemainingOutputAmount(node, outputs.get(i).itemId(), nodes) > 0) {
                                this.draggingFromNode = node;
                                this.draggingOutputItem = outputs.get(i).itemId();
                                this.draggingSlotIndex = i;
                                return true;
                            }
                        }
                    }
                } else {
                    if (worldX >= node.x() + node.width() && worldX <= node.x() + node.width() + 8 &&
                            worldY >= node.y() + (node.height()/2) - 6 && worldY <= node.y() + (node.height()/2) + 6) {
                        if (getRemainingOutputAmount(node, node.itemType(), nodes) > 0) {
                            this.draggingFromNode = node;
                            this.draggingOutputItem = node.itemType();
                            this.draggingSlotIndex = -1;
                            return true;
                        }
                    }
                }
            }
        }

        if (button == 1) {
            for (int index = 0; index < nodes.size(); index++) {
                DiagramNode node = nodes.get(index);
                boolean isMach = EmiHelper.isMachine(node.itemType());
                int h = node.height();

                if (isMach && worldX >= node.x() - 6 && worldX <= node.x() + 2 && worldY >= node.y() + (h/2) - 6 && worldY <= node.y() + (h/2) + 6) {
                    final UUID targetId = node.id();
                    edges.removeIf(edge -> edge.toNode().equals(targetId));
                    return true;
                }

                if (isMach) {
                    List<RecipeOutput> outputs = getDynamicOutputs(node, nodes);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (h - totalOutHeight) / 2;

                    for (int i = 0; i < outputs.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            Map<String, Integer> inputs = getIncomingItems(node, nodes, new HashSet<>());
                            String newProp = recipeEngine.getNextAlternativeTarget(node.itemType(), node.property(), inputs);
                            if (newProp != null) {
                                nodes.set(index, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), newProp, node.amount(), node.color(), node.width(), node.height()));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(double worldX, double worldY, List<DiagramNode> nodes, int button) {
        // --- QUANDO RILASCIAMO LO SLIDER, SALVIAMO LA QUANTITA' ---
        if (isDraggingSlider) {
            isDraggingSlider = false;
            if (edgeWithOpenSlider != null) {
                int idx = edges.indexOf(edgeWithOpenSlider);
                if (idx != -1 && sliderValue != edgeWithOpenSlider.amount()) {
                    DiagramEdge newEdge = new DiagramEdge(edgeWithOpenSlider.fromNode(), edgeWithOpenSlider.outputItem(), edgeWithOpenSlider.toNode(), sliderValue);
                    edges.set(idx, newEdge);
                    balanceEdges(newEdge.fromNode(), newEdge.outputItem(), nodes, newEdge);
                    edgeWithOpenSlider = newEdge;
                }
            }
            return true;
        }

        if (button == 0 && this.draggingFromNode != null) {
            for (DiagramNode node : nodes) {
                if (node == this.draggingFromNode) continue;
                int h = node.height();

                if (EmiHelper.isMachine(node.itemType()) && worldX >= node.x() - 6 && worldX <= node.x() + 2 && worldY >= node.y() + (h/2) - 6 && worldY <= node.y() + (h/2) + 6) {

                    int amountToAdd = getRemainingOutputAmount(draggingFromNode, draggingOutputItem, nodes);
                    if (amountToAdd > 0) {
                        addEdge(new DiagramEdge(draggingFromNode.id(), draggingOutputItem, node.id(), amountToAdd));
                    }
                    break;
                }
            }
            this.draggingFromNode = null;
            return true;
        }
        return false;
    }

    public void removeNodeConnections(UUID nodeId) {
        edges.removeIf(edge -> edge.fromNode().equals(nodeId) || edge.toNode().equals(nodeId));
    }

    public List<DiagramEdge> getEdges() {
        return this.edges;
    }

    public void setEdges(List<DiagramEdge> loadedEdges) {
        this.edges = new ArrayList<>(loadedEdges);
    }
}