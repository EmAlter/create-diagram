package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.component.RecipeOutput;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for edge management in the canvas. Handles all user interactions
 * with edges such as clicking, dragging, and slider manipulation.
 */
public class EdgeController {
    private final EdgeModel model;
    private final EdgeView view;

    public EdgeController(EdgeModel model, EdgeView view) {
        this.model = model;
        this.view = view;
    }

    public void render(GuiGraphics guiGraphics, List<DiagramNode> nodes, double worldX, double worldY) {
        model.setMouseWorldX((int) worldX);
        model.setMouseWorldY((int) worldY);
        model.triggerCascadeClean(nodes);
        view.render(guiGraphics, nodes, worldX, worldY);
    }

    public boolean renderTooltips(GuiGraphics gui, int mouseX, int mouseY, double worldX, double worldY, List<DiagramNode> nodes) {
        return view.renderTooltips(gui, mouseX, mouseY, worldX, worldY, nodes, net.minecraft.client.Minecraft.getInstance().font);
    }

    public boolean mouseDragged(double worldX, double worldY, int button) {
        if (model.isDraggingSlider() && model.getEdgeWithOpenSlider() != null) {
            model.updateSliderValue(worldX);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double worldX, double worldY, List<DiagramNode> nodes, int button) {
        // Handle slider interaction
        if (model.getEdgeWithOpenSlider() != null) {
            int sliderX = model.getSliderX();
            int sliderY = model.getSliderY();
            int sliderWidth = model.getSliderWidth();
            int sliderHeight = model.getSliderHeight();

            if (worldX >= sliderX - 10 && worldX <= sliderX + sliderWidth + 10 && worldY >= sliderY - 15 && worldY <= sliderY + sliderHeight + 10) {
                model.setDraggingSlider(true);
                model.updateSliderValue(worldX);
                return true;
            } else {
                model.setEdgeWithOpenSlider(null);
                model.setDraggingSlider(false);
                return true;
            }
        }

        // Handle edge clicking (left button)
        if (button == 0) {
            // Check if clicking on an existing edge badge
                for (DiagramEdge edge : model.getEdges()) {
                DiagramNode from = findNode(nodes, edge.fromNode());
                DiagramNode to = findNode(nodes, edge.toNode());
                if (from != null && to != null && EmiHelper.isMachine(from.itemType())) {
                    int startX, startY;
                    List<RecipeOutput> outputs = model.getDynamicOutputs(from, nodes);
                    int outIndex = 0;
                    for (int i = 0; i < outputs.size(); i++) if (outputs.get(i).itemId().equals(edge.outputItem())) outIndex = i;
                    startX = from.x() + from.width() + 18;
                    int totalOutHeight = outputs.size() * 18;
                    startY = from.y() + (from.height() - totalOutHeight) / 2 + (outIndex * 18) + 8;

                    int endX = to.x() - 6;
                    int endY = to.y() + (to.height() / 2);

                    int[] mid = getBezierMidPoint(startX, startY, endX, endY);
                    if (worldX >= mid[0] - 8 && worldX <= mid[0] + 8 && worldY >= mid[1] - 6 && worldY <= mid[1] + 6) {
                        openSlider(edge, from, nodes, mid);
                        return true;
                    }
                }
            }

            // Check if dragging from an output port
            for (DiagramNode node : nodes) {
                if (node.itemType().equals("creatediagram:text_comment")) continue;

                if (EmiHelper.isMachine(node.itemType())) {
                    List<RecipeOutput> outputs = model.getDynamicOutputs(node, nodes);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                    for (int i = 0; i < outputs.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            if (model.getRemainingOutputAmountForController(node, outputs.get(i).itemId(), nodes) > 0) {
                                model.setDraggingFromNode(node);
                                model.setDraggingOutputItem(outputs.get(i).itemId());
                                model.setDraggingSlotIndex(i);
                                return true;
                            }
                        }
                    }
                } else {
                    if (worldX >= node.x() + node.width() && worldX <= node.x() + node.width() + 8 &&
                            worldY >= node.y() + (node.height()/2) - 6 && worldY <= node.y() + (node.height()/2) + 6) {
                        if (model.getRemainingOutputAmountForController(node, node.itemType(), nodes) > 0) {
                            model.setDraggingFromNode(node);
                            model.setDraggingOutputItem(node.itemType());
                            model.setDraggingSlotIndex(-1);
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
                    model.getEdges().removeIf(edge -> edge.toNode().equals(targetId));
                    return true;
                }

                if (isMach) {
                    List<RecipeOutput> outputs = model.getDynamicOutputs(node, nodes);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (h - totalOutHeight) / 2;

                    for (int i = 0; i < outputs.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            java.util.Map<String, Integer> inputs = getIncomingItems(node, nodes);
                            String newProp = new com.emalter.creatediagram.logic.RecipeEngine().getNextAlternativeTarget(node.itemType(), node.property(), inputs);
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
        if (model.isDraggingSlider()) {
            model.setDraggingSlider(false);
            if (model.getEdgeWithOpenSlider() != null) {
                int idx = model.getEdges().indexOf(model.getEdgeWithOpenSlider());
                if (idx != -1 && model.getSliderValue() != model.getEdgeWithOpenSlider().amount()) {
                    DiagramEdge newEdge = new DiagramEdge(model.getEdgeWithOpenSlider().fromNode(), model.getEdgeWithOpenSlider().outputItem(), model.getEdgeWithOpenSlider().toNode(), model.getSliderValue());
                    model.getEdges().set(idx, newEdge);
                    model.setEdgeWithOpenSlider(newEdge);
                }
            }
            return true;
        }

        if (button == 0 && model.getDraggingFromNode() != null) {
            DiagramNode draggingNode = model.getDraggingFromNode();
            String draggingItem = model.getDraggingOutputItem();
            
            for (DiagramNode node : nodes) {
                if (node == draggingNode) continue;
                int h = node.height();

                if (EmiHelper.isMachine(node.itemType()) && worldX >= node.x() - 6 && worldX <= node.x() + 2 && worldY >= node.y() + (h/2) - 6 && worldY <= node.y() + (h/2) + 6) {
                    int amountToAdd = model.getRemainingOutputAmountForController(draggingNode, draggingItem, nodes);
                    if (amountToAdd > 0) {
                        model.addEdge(new DiagramEdge(draggingNode.id(), draggingItem, node.id(), amountToAdd));
                    }
                    break;
                }
            }
            model.setDraggingFromNode(null);
            return true;
        }
        return false;
    }

    private void openSlider(DiagramEdge edge, DiagramNode from, List<DiagramNode> nodes, int[] midPoint) {
        int maxOutput = 1;
        List<RecipeOutput> outputs = model.getDynamicOutputs(from, nodes);
        for (RecipeOutput out : outputs) if (out.itemId().equals(edge.outputItem())) { maxOutput = out.amount(); break; }

        int maxOptions = model.getRemainingOutputAmountForController(from, edge.outputItem(), nodes) + edge.amount();

        model.setSliderMin(1);
        model.setSliderMax(maxOptions);
        model.setSliderValue(edge.amount());
        model.setSliderWidth(100);
        model.setSliderX(midPoint[0] - 50);
        model.setSliderY(midPoint[1] + 15);
        model.setEdgeWithOpenSlider(edge);
    }

    private java.util.Map<String, Integer> getIncomingItems(DiagramNode machine, List<DiagramNode> allNodes) {
        java.util.Map<String, Integer> incoming = new java.util.HashMap<>();

        for (DiagramEdge edge : model.getEdges()) {
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

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
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
}





