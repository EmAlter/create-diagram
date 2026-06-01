package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.client.diagram.DiagramMediator;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeController;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeModel;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeView;
import com.emalter.creatediagram.client.diagram.canvas.node.DiagramNodeFactory;
import com.emalter.creatediagram.client.diagram.canvas.text.TextController;
import com.emalter.creatediagram.client.diagram.canvas.text.TextModel;
import com.emalter.creatediagram.client.diagram.canvas.text.TextView;
import com.emalter.creatediagram.client.diagram.canvas.node.NodeController;
import com.emalter.creatediagram.client.diagram.canvas.node.NodeModel;
import com.emalter.creatediagram.client.diagram.canvas.node.NodeView;
import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.logic.integration.ModIntegration;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.client.toolbar.Tool;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;

public class CanvasController {
    private final CanvasModel model;
    private final CanvasView view;
    private final Font font;
    private final DiagramMediator mediator;
    private final ModIntegration modIntegration = ModIntegration.get();
    
    private final Map<String, Boolean> machineCache = new HashMap<>();
    private final Map<String, EmiStack> stackCache = new HashMap<>();
    private final Map<String, List<String>> catalystCache = new HashMap<>();

    public boolean isMachine(String id) {
        return machineCache.computeIfAbsent(id, modIntegration::isMachine);
    }
    public EmiStack getStack(String id) {
        return stackCache.computeIfAbsent(id, modIntegration::getStack);
    }
    public List<String> getValidCatalystsForMachine(String id) {
        return catalystCache.computeIfAbsent(id, modIntegration::getValidCatalystsForMachine);
    }

    public CanvasController(Font font, DiagramMediator mediator) {
        this.font = font;
        this.mediator = mediator;
        this.model = new CanvasModel();
        this.view = new CanvasView(font);

        EdgeModel em = new EdgeModel();
        EdgeView ev = new EdgeView(em);
        EdgeController ec = new EdgeController(em, ev);
        this.model.edgeModel = em;
        this.model.edgeController = ec;

        TextModel tm = new TextModel();
        TextView tv = new TextView(font);
        TextController tc = new TextController(tm, tv);
        this.model.textModel = tm;
        this.model.textController = tc;

        NodeModel nm = new NodeModel();
        NodeView nv = new NodeView(font);
        NodeController nc = new NodeController(nm, nv);
        this.model.nodeController = nc;
    }

    // Rimosso il parametro paletteWidth
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        // 1. Usa il Mediator per parlare con il MenuController!
        int paletteWidth = mediator.getPalette().getIsOpen() ? mediator.getPalette().getWidth() : 0;
        String draggingId = mediator.getPalette().getDraggingItemId();

        // 2. Aggiorna il modello autonomamente
        model.setPreviewItem(draggingId);

        // 3. Passa il dato alla View
        view.render(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, paletteWidth, this);
    }

    public List<OutputPort> getDynamicOutputs(DiagramNode node) {
        return model.nodeController.getDynamicOutputs(node, model.nodes, model.getEdges());
    }

    public void cascadeCleanEdges() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - model.lastCleanTime < 250) return;
        model.lastCleanTime = currentTime;

        List<DiagramEdge> edges = model.getEdges();
        edges.removeIf(edge -> {
            DiagramNode fromNode = findNode(edge.fromNode());
            if (fromNode == null) return true;

            if (!isMachine(fromNode.itemType())) return false;

            List<OutputPort> validOutputs = getDynamicOutputs(fromNode);
            boolean isValid = validOutputs.stream().anyMatch(out -> out.itemId().equals(edge.outputItem()));

            if (!isValid && model.edgeController.getModel().getEdgeWithOpenSlider() == edge) {
                model.edgeController.getModel().setEdgeWithOpenSlider(null);
                model.edgeController.getModel().setDraggingSlider(false);
            }
            return !isValid;
        });

        for (DiagramNode node : model.getNodes()) {
            if (!isMachine(node.itemType())) continue;
            List<OutputPort> validOutputs = getDynamicOutputs(node);
            for (OutputPort out : validOutputs) {
                balanceEdges(node.id(), out.itemId(), out.amount());
            }
        }
    }

    private void balanceEdges(UUID fromNodeId, String outputItem, int maxAllowed) {
        List<DiagramEdge> affectedEdges = new ArrayList<>();
        int totalUsed = 0;
        for (DiagramEdge edge : model.getEdges()) {
            if (edge.fromNode().equals(fromNodeId) && edge.outputItem().equals(outputItem)) {
                totalUsed += edge.amount();
                affectedEdges.add(edge);
            }
        }
        if (totalUsed > maxAllowed) {
            for (int i = affectedEdges.size() - 1; i >= 0; i--) {
                DiagramEdge target = affectedEdges.get(i);
                model.getEdges().remove(target);
                totalUsed -= target.amount();
                if (totalUsed <= maxAllowed) break;
            }
        }
    }

    public int getRemainingOutputAmount(DiagramNode fromNode, String outputItem) {
        int maxAllowed = 1;
        if (isMachine(fromNode.itemType())) {
            for (OutputPort out : getDynamicOutputs(fromNode)) {
                if (out.itemId().equals(outputItem)) { maxAllowed = out.amount(); break; }
            }
        } else {
            maxAllowed = fromNode.amount();
        }

        int used = 0;
        for (DiagramEdge edge : getEdges()) {
            if (edge.fromNode().equals(fromNode.id()) && edge.outputItem().equals(outputItem)) used += edge.amount();
        }
        return Math.max(0, maxAllowed - used);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int paletteWidth = mediator.getPalette().getIsOpen() ? mediator.getPalette().getWidth() : 0;

        double worldX = getWorldX(mouseX);
        double worldY = getWorldY(mouseY);

        if (model.textController.mouseClicked(worldX, worldY, model.nodes, button)) return true;

        if (model.activeAmountField != null) {
            if (!(worldX >= model.activeAmountField.getX() && worldX <= model.activeAmountField.getX() + model.activeAmountField.getWidth() && worldY >= model.activeAmountField.getY() && worldY <= model.activeAmountField.getY() + model.activeAmountField.getHeight())) {
                closeAndSaveAmountField();
            }
        }

        if (model.nodeWithOpenMenu != null) {
            DiagramNode node = model.findNode(model.nodeWithOpenMenu);
            if (node != null) {
                List<String> options = getValidCatalystsForMachine(node.itemType());
                int menuX = node.x() + node.width();
                int menuY = node.y();
                if (worldX >= menuX && worldX <= menuX + 20) {
                    int clickedIndex = (int) ((worldY - menuY) / 20);
                    if (clickedIndex >= 0 && clickedIndex < options.size()) {
                        int nodeIdx = model.nodes.indexOf(node);
                        model.nodes.set(nodeIdx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), options.get(clickedIndex), node.amount(), node.color(), node.width(), node.height()));
                        model.nodeWithOpenMenu = null;
                        return true;
                    }
                }
            }
            model.nodeWithOpenMenu = null;
            return true;
        }

        if (model.edgeController != null && model.edgeController.mouseClicked(worldX, worldY, this, button)) return true;

        if (button == 1) {
            boolean clickedOnNode = false;
            for (int i = 0; i < model.nodes.size(); i++) {
                DiagramNode node = model.nodes.get(i);

                if (isMachine(node.itemType())) {
                    List<OutputPort> outputs = getDynamicOutputs(node);
                    int outX = node.x() + node.width() + 2;
                    int totalOutHeight = outputs.size() * 18;
                    int startOutY = node.y() + (node.height() - totalOutHeight) / 2;

                    for (int j = 0; j < outputs.size(); j++) {
                        int outY = startOutY + (j * 18);
                        if (worldX >= outX && worldX <= outX + 16 && worldY >= outY && worldY <= outY + 16) {
                            DiagramNode updatedNode = model.nodeController.cycleOutputTarget(node, model.nodes, getEdges());
                            if (updatedNode != null) {
                                model.nodes.set(i, updatedNode);
                                return true;
                            }
                        }
                    }
                }

                if (worldX >= node.x() && worldX <= node.x() + node.width() && worldY >= node.y() && worldY <= node.y() + node.height()) {
                    if (model.edgeModel != null) model.edgeModel.removeNodeConnections(node.id());
                    model.nodes.remove(i);
                    clickedOnNode = true;
                    break;
                }
            }

            if (!clickedOnNode) {
                if (Screen.hasShiftDown()) {
                    addTextComment((int) worldX, (int) worldY);
                    return true;
                }
                model.isPanning = true;
            }
            return true;
        }

        if (button == 0) {
            boolean hitNode = false;
            for (int i = model.nodes.size() - 1; i >= 0; i--) {
                DiagramNode node = model.nodes.get(i);
                int w = node.width();
                int h = node.height();
                boolean isComment = node.itemType().equals("creatediagram:text_comment");
                boolean isMachine = isMachine(node.itemType());

                if (worldX >= node.x() + w - 10 && worldX <= node.x() + w && worldY >= node.y() + h - 10 && worldY <= node.y() + h) {
                    model.resizingNode = node;
                    model.resizeStartWidth = w;
                    model.resizeStartHeight = h;
                    model.resizeMouseStartX = (int) worldX;
                    model.resizeMouseStartY = (int) worldY;
                    return true;
                }

                if (!isComment && !isMachine && worldX >= node.x() + w - 16 && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + 14) {
                    model.nodeWithActiveAmountField = node.id();
                    model.activeAmountField = new EditBox(this.font, node.x() + (w/2) - 15, node.y() - 16, 30, 14, Component.literal("Qty"));
                    model.activeAmountField.setValue(String.valueOf(node.amount()));
                    model.activeAmountField.setFocused(true);
                    return true;
                }

                if (!isComment && worldX >= node.x() + 4 && worldX <= node.x() + 18 && worldY >= node.y() + h - 18 && worldY <= node.y() + h - 4) {
                    if (!getValidCatalystsForMachine(node.itemType()).isEmpty()) {
                        model.nodeWithOpenMenu = node.id();
                        return true;
                    }
                }

                if (worldX >= node.x() && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + h) {
                    hitNode = true;
                    long currentTime = System.currentTimeMillis();

                    if (node.id().equals(model.lastClickedNodeId) && (currentTime - model.lastClickTime) < 300) {
                        int cloneX = node.x() + node.width() + 20;
                        int cloneY = node.y() + node.height() + 20;
                        while (!isPositionValid(cloneX, cloneY, node.width(), node.height(), null)) {
                            cloneX += 20; cloneY += 20;
                        }
                        DiagramNode clonedNode = new DiagramNode(UUID.randomUUID(), node.itemType(), cloneX, cloneY, node.property(), node.amount(), node.color(), w, h);
                        model.nodes.add(clonedNode);
                        model.lastClickedNodeId = null;
                        return true;
                    }

                    model.lastClickedNodeId = node.id();
                    model.lastClickTime = currentTime;
                    if (isComment) model.textModel.startEditing(node.id(), node.property());
                    model.draggedNode = node;
                    model.dragStartX = node.x();
                    model.dragStartY = node.y();
                    break;
                }
            }

            if (!hitNode && mediator.getToolbar() != null && !mediator.getToolbar().isColorMenuOpen()) {
                if (model.currentTool == Tool.PEN) {
                    model.currentStrokePoints = new ArrayList<>();
                    model.currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
                    return true;
                }
                if (model.currentTool == Tool.LINE) {
                    model.lineStartX = (int) worldX; model.lineStartY = (int) worldY; model.lineCurrentX = model.lineStartX; model.lineCurrentY = model.lineStartY; model.isDrawingLine = true; return true;
                }
                if (model.currentTool == Tool.ERASER) { eraseAt((int) worldX, (int) worldY); return true; }
            }
            if (hitNode) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (model.isPanning) { model.offsetX += dragX; model.offsetY += dragY; return true; }
        double worldX = getWorldX(mouseX); double worldY = getWorldY(mouseY);
        if (model.edgeController != null && model.edgeController.mouseDragged(worldX, worldY, button)) return true;

        if (model.resizingNode != null) {
            int index = model.nodes.indexOf(model.resizingNode);
            if (index != -1) {
                int newW = model.resizeStartWidth + (int) (worldX - model.resizeMouseStartX);
                int newH = model.resizeStartHeight + (int) (worldY - model.resizeMouseStartY);
                int minW = model.resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
                int minH = model.resizingNode.itemType().equals("creatediagram:text_comment") ? 20 : (model.resizingNode.itemType().contains("mechanical_mixer") || model.resizingNode.itemType().contains("mechanical_press") ? 60 : 40);
                newW = Math.max(minW, newW); newH = Math.max(minH, newH);
                DiagramNode updatedNode = new DiagramNode(model.resizingNode.id(), model.resizingNode.itemType(), model.resizingNode.x(), model.resizingNode.y(), model.resizingNode.property(), model.resizingNode.amount(), model.resizingNode.color(), newW, newH);
                model.nodes.set(index, updatedNode); model.resizingNode = updatedNode;
            }
            return true;
        }

        if (model.draggedNode != null) {
            int index = model.nodes.indexOf(model.draggedNode);
            if (index != -1) {
                DiagramNode updatedNode = new DiagramNode(model.draggedNode.id(), model.draggedNode.itemType(), (int) worldX - (model.draggedNode.width()/2), (int) worldY - (model.draggedNode.height()/2), model.draggedNode.property(), model.draggedNode.amount(), model.draggedNode.color(), model.draggedNode.width(), model.draggedNode.height());
                model.nodes.set(index, updatedNode); model.draggedNode = updatedNode;
            }
            return true;
        }

        if (model.currentTool == Tool.PEN && model.currentStrokePoints != null) {
            int[] lastP = model.currentStrokePoints.get(model.currentStrokePoints.size() - 1);
            if (Math.hypot(worldX - lastP[0], worldY - lastP[1]) > 3.0) model.currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
            return true;
        }
        if (model.currentTool == Tool.LINE && model.isDrawingLine) { model.lineCurrentX = (int) worldX; model.lineCurrentY = (int) worldY; return true; }
        if (model.currentTool == Tool.ERASER) { eraseAt((int) worldX, (int) worldY); return true; }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) model.isPanning = false;
        double worldX = getWorldX(mouseX); double worldY = getWorldY(mouseY);

        if (model.currentTool == Tool.PEN && model.currentStrokePoints != null) {
            if (model.currentStrokePoints.size() > 1) model.strokes.add(new CanvasModel.DiagramStroke(UUID.randomUUID(), model.currentColor, model.currentStrokePoints));
            model.currentStrokePoints = null; return true;
        }

        if (model.currentTool == Tool.LINE && model.isDrawingLine) { List<int[]> pts = List.of(new int[]{model.lineStartX, model.lineStartY}, new int[]{(int) worldX, (int) worldY}); model.strokes.add(new CanvasModel.DiagramStroke(UUID.randomUUID(), model.currentColor, pts)); model.isDrawingLine = false; return true; }
        if (model.edgeController != null && model.edgeController.mouseReleased(worldX, worldY, this, button)) return true;

        if (model.resizingNode != null) {
            int snappedW = Math.round(model.resizingNode.width() / 20.0f) * 20;
            int snappedH = Math.round(model.resizingNode.height() / 20.0f) * 20;
            int minW = model.resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
            int minH = model.resizingNode.itemType().equals("creatediagram:text_comment") ? 20 : (model.resizingNode.itemType().contains("mechanical_mixer") || model.resizingNode.itemType().contains("mechanical_press") ? 60 : 40);
            snappedW = Math.max(minW, snappedW); snappedH = Math.max(minH, snappedH);
            int index = model.nodes.indexOf(model.resizingNode);
            if (index != -1) model.nodes.set(index, new DiagramNode(model.resizingNode.id(), model.resizingNode.itemType(), model.resizingNode.x(), model.resizingNode.y(), model.resizingNode.property(), model.resizingNode.amount(), model.resizingNode.color(), snappedW, snappedH));
            model.resizingNode = null; return true;
        }

        if (model.draggedNode != null) {
            int snappedX = Math.round(model.draggedNode.x() / 20.0f) * 20;
            int snappedY = Math.round(model.draggedNode.y() / 20.0f) * 20;
            int currentW = model.draggedNode.width(); int currentH = model.draggedNode.height();
            if (!isPositionValid(snappedX, snappedY, currentW, currentH, model.draggedNode.id())) { snappedX = model.dragStartX; snappedY = model.dragStartY; }
            int index = model.nodes.indexOf(model.draggedNode);
            if (index != -1) model.nodes.set(index, new DiagramNode(model.draggedNode.id(), model.draggedNode.itemType(), snappedX, snappedY, model.draggedNode.property(), model.draggedNode.amount(), model.draggedNode.color(), currentW, currentH));
            model.draggedNode = null; return true;
        }
        return false;
    }

    public void cancelDrag() {
        if (model.draggedNode != null) {
            int index = model.nodes.indexOf(model.draggedNode);
            if (index != -1) {
                model.nodes.set(index, new DiagramNode(
                        model.draggedNode.id(),
                        model.draggedNode.itemType(),
                        model.dragStartX,
                        model.dragStartY,
                        model.draggedNode.property(),
                        model.draggedNode.amount(),
                        model.draggedNode.color(),
                        model.draggedNode.width(),
                        model.draggedNode.height()
                ));
            }
            model.draggedNode = null;
        }
    }

    public void addTextComment(int worldX, int worldY) {
        DiagramNode comment = DiagramNodeFactory.createNode(DiagramNodeFactory.NodeCategory.TEXT, "creatediagram:text_comment", worldX, worldY, 80, 20);
        model.nodes.add(comment);
        model.textModel.startEditing(comment.id(), "");
    }
    private void eraseAt(int wx, int wy) {
        model.strokes.removeIf(stroke -> { for (int[] p : stroke.points()) { if (Math.hypot(p[0] - wx, p[1] - wy) < 25.0) return true; } return false; });
    }
    public void closeAndSaveAmountField() {
        if (model.nodeWithActiveAmountField != null && model.activeAmountField != null) {
            DiagramNode node = model.findNode(model.nodeWithActiveAmountField);
            if (node != null) {
                int idx = model.nodes.indexOf(node);
                int newAmount;
                try {
                    newAmount = Integer.parseInt(model.activeAmountField.getValue().trim());
                    if (newAmount < 1) newAmount = 1;
                    EmiStack stackInfo = getStack(node.itemType());
                    int maxAllowed = stackInfo.getItemStack().isEmpty() ? 1000000 : 64;
                    if (newAmount > maxAllowed) newAmount = maxAllowed;
                } catch (NumberFormatException e) { newAmount = node.amount(); }
                model.nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), node.property(), newAmount, node.color(), node.width(), node.height()));
            }
        }
        model.activeAmountField = null; model.nodeWithActiveAmountField = null;
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        float zoomDelta = (float) Math.signum(scrollY) * 0.15f * model.zoom;
        float newZoom = Mth.clamp(model.zoom + zoomDelta, model.MIN_ZOOM, model.MAX_ZOOM);
        if (newZoom != model.zoom) {
            double worldXBefore = getWorldX(mouseX); double worldYBefore = getWorldY(mouseY);
            model.zoom = newZoom; model.offsetX = mouseX - (worldXBefore * model.zoom); model.offsetY = mouseY - (worldYBefore * model.zoom);
        }
        return true;
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (model.activeAmountField != null) { if (keyCode == 257) { closeAndSaveAmountField(); return true; } return model.activeAmountField.keyPressed(keyCode, scanCode, modifiers); }
        if (model.textController.isEditing()) { if (model.textController.keyPressed(keyCode, scanCode, modifiers)) syncEditedComment(); return true; }
        return false;
    }
    public boolean charTyped(char codePoint, int modifiers) {
        if (model.activeAmountField != null) { if (Character.isDigit(codePoint)) return model.activeAmountField.charTyped(codePoint, modifiers); return true; }
        if (model.textController.isEditing()) { if (model.textController.charTyped(codePoint, modifiers)) syncEditedComment(); return true; }
        return false;
    }
    public void syncEditedComment() {
        if (!model.textController.isEditing()) return;
        UUID editingNodeId = model.textController.getEditingNodeId();
        DiagramNode node = model.findNode(editingNodeId);
        if (node != null) {
            int idx = model.nodes.indexOf(node);
            model.nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), model.textController.getEditedText(), node.amount(), node.color(), node.width(), node.height()));
        }
    }

    public void setCurrentTool(Tool tool) { this.model.currentTool = tool; }
    public void setCurrentColor(int color) { this.model.currentColor = color; }
    public TextController getTextController() { return this.model.textController; }
    public NodeController getNodeController() { return this.model.nodeController; }
    public int getNodeWidth(String itemType) { return this.model.getNodeWidth(itemType); }
    public int getNodeHeight(String itemType) { return this.model.getNodeHeight(itemType); }
    public double getOffsetX() { return model.getOffsetX(); }
    public double getOffsetY() { return model.getOffsetY(); }
    public void setOffset(double x, double y) { model.setOffset(x, y); }
    public void setZoom(float zoom) { model.setZoom(zoom); }
    public void setPreviewItem(String itemType) { model.setPreviewItem(itemType); }
    public List<DiagramNode> getNodes() { return model.getNodes(); }
    public void setNodes(List<DiagramNode> nodes) { model.setNodes(nodes); }
    public List<DiagramEdge> getEdges() { return model.getEdges(); }
    public void setEdges(List<DiagramEdge> edges) { model.setEdges(edges); }
    public void setStrokes(List<CanvasModel.DiagramStroke> strokes) { model.setStrokes(strokes); }
    public double getWorldX(double mouseX) { return model.getWorldX(mouseX); }
    public double getWorldY(double mouseY) { return model.getWorldY(mouseY); }
    public void addNode(DiagramNode node) { model.addNode(node); }
    public boolean isPositionValid(int x, int y, int w, int h, UUID ignoreId) { return model.isPositionValid(x, y, w, h, ignoreId); }
    public float getGridSize() { return model.gridSize; }
    public List<int[]> getCurrentStrokePoints() { return model.currentStrokePoints; }
    public boolean isDrawingLine() { return model.isDrawingLine; }
    public int getLineStartX() { return model.lineStartX; }
    public int getLineStartY() { return model.lineStartY; }
    public int getLineCurrentX() { return model.lineCurrentX; }
    public int getLineCurrentY() { return model.lineCurrentY; }
    public UUID getNodeWithOpenMenu() { return model.nodeWithOpenMenu; }
    public DiagramNode getDraggedNode() { return model.draggedNode; }
    public EditBox getActiveAmountField() { return model.activeAmountField; }
    public UUID getNodeWithActiveAmountField() { return model.nodeWithActiveAmountField; }
    public TextModel getTextModel() { return model.textModel; }
    public DiagramNode findNode(UUID id) { return model.findNode(id); }
    public EdgeController getEdgeController() { return model.edgeController; }
    public Tool getCurrentTool() { return model.currentTool; }
    public int getCurrentColor() { return model.currentColor; }
    public float getZoom() { return model.getZoom(); }
    public List<CanvasModel.DiagramStroke> getStrokes() { return model.getStrokes(); }
}