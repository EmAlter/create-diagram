package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.client.diagram.canvas.CanvasController;
import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class EdgeController {
    private final EdgeModel model;
    private final EdgeView view;

    public EdgeController(EdgeModel model, EdgeView view) {
        this.model = model;
        this.view = view;
    }

    public EdgeModel getModel() { return model; }

    public void render(GuiGraphics guiGraphics, CanvasController canvasController, double worldX, double worldY) {
        model.setMouseWorldX((int) worldX);
        model.setMouseWorldY((int) worldY);
        canvasController.cascadeCleanEdges();
        view.render(guiGraphics, model, canvasController);
    }

    public boolean mouseDragged(double worldX, double worldY, int button) {
        if (model.isDraggingSlider() && model.getEdgeWithOpenSlider() != null) {
            model.updateSliderValue(worldX);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double worldX, double worldY, CanvasController canvasController, int button) {
        if (model.getEdgeWithOpenSlider() != null) {
            int sliderX = model.getSliderX();
            int sliderY = model.getSliderY();
            if (worldX >= sliderX - 10 && worldX <= sliderX + model.getSliderWidth() + 10 && worldY >= sliderY - 15 && worldY <= sliderY + model.getSliderHeight() + 10) {
                model.setDraggingSlider(true);
                model.updateSliderValue(worldX);
                return true;
            } else {
                model.setEdgeWithOpenSlider(null);
                model.setDraggingSlider(false);
                return true;
            }
        }

        if (button == 0) {
            for (DiagramEdge edge : model.getEdges()) {
                DiagramNode from = canvasController.findNode(edge.fromNode());
                DiagramNode to = canvasController.findNode(edge.toNode());
                
                if (from != null && to != null && canvasController.isMachine(from.itemType())) {
                    int[] mid = view.getBezierMidPointForEdge(edge, canvasController);
                    if (mid != null && worldX >= mid[0] - 8 && worldX <= mid[0] + 8 && worldY >= mid[1] - 6 && worldY <= mid[1] + 6) {

                        // Blocca l'apertura dello slider se l'output è marcato come infinito
                        if (!canvasController.isEdgeInfinite(edge)) {
                            openSlider(edge, from, canvasController, mid);
                        }
                        return true;
                    }
                }
            }

            for (DiagramNode node : canvasController.getNodes()) {
                if (node.itemType().equals("creatediagram:text_comment")) continue;

                if (canvasController.isMachine(node.itemType())) {
                    List<OutputPort> outputs = canvasController.getDynamicOutputs(node);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                    for (int i = 0; i < outputs.size(); i++) {
                        int outY = startOutY + (i * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            if (canvasController.getRemainingOutputAmount(node, outputs.get(i).itemId()) > 0) {
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
                        if (canvasController.getRemainingOutputAmount(node, node.itemType()) > 0) {
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
            for (int index = 0; index < canvasController.getNodes().size(); index++) {
                DiagramNode node = canvasController.getNodes().get(index);
                boolean isMach = canvasController.isMachine(node.itemType());
                int h = node.height();

                if (isMach && worldX >= node.x() - 6 && worldX <= node.x() + 2 && worldY >= node.y() + (h/2) - 6 && worldY <= node.y() + (h/2) + 6) {
                    final java.util.UUID targetId = node.id();
                    model.getEdges().removeIf(edge -> edge.toNode().equals(targetId));
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(double worldX, double worldY, CanvasController canvasController, int button) {
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

            for (DiagramNode node : canvasController.getNodes()) {
                if (node == draggingNode) continue;
                int h = node.height();

                if (canvasController.isMachine(node.itemType()) && worldX >= node.x() - 6 && worldX <= node.x() + 2 && worldY >= node.y() + (h/2) - 6 && worldY <= node.y() + (h/2) + 6) {
                    int amountToAdd = canvasController.getRemainingOutputAmount(draggingNode, draggingItem);
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

    private void openSlider(DiagramEdge edge, DiagramNode from, CanvasController canvasController, int[] midPoint) {
        int maxOptions = canvasController.getRemainingOutputAmount(from, edge.outputItem()) + edge.amount();
        model.setSliderMin(1);
        model.setSliderMax(maxOptions);
        model.setSliderValue(edge.amount());
        model.setSliderWidth(100);
        model.setSliderX(midPoint[0] - 50);
        model.setSliderY(midPoint[1] + 15);
        model.setEdgeWithOpenSlider(edge);
    }
}