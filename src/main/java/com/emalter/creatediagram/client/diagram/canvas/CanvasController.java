package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeController;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeModel;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeView;
import com.emalter.creatediagram.client.diagram.canvas.text.TextController;
import com.emalter.creatediagram.client.diagram.canvas.text.TextModel;
import com.emalter.creatediagram.client.diagram.canvas.text.TextView;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import com.emalter.creatediagram.client.toolbar.Tool;
import com.emalter.creatediagram.client.toolbar.ToolbarController;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * CanvasPanel is the central UI component that manages nodes, drawing strokes, panning/zooming and
 * delegates connection rendering and interaction to a ConnectionManager.
 */
public class CanvasController extends CanvasModel {
    private final ToolbarController toolbar = new ToolbarController();
    private final CanvasView view;
    private final Font font;
    // Last known palette width (set every frame) so we can compute toolbar positions on mouse release
    private int lastPaletteWidth = 0;

    public CanvasController(Font font) {
        this.font = font;
        this.view = new CanvasView(font);
        EdgeModel em = new EdgeModel();
        EdgeView ev = new EdgeView(em);
        EdgeController ec = new EdgeController(em, ev);
        this.edgeModel = em;
        this.edgeController = ec;
        // Initialize Text MVC
        TextModel tm = new TextModel();
        TextView tv = new TextView(font);
        TextController tc = new TextController(tm, tv);
        this.textModel = tm;
        this.textController = tc;
    }

    // Keep only controller-specific accessor; generic state accessors are inherited from CanvasModel
    public TextController getTextController() { return this.textController; }

    public int getNodeWidth(String itemType) { return super.getNodeWidth(itemType); }
    public int getNodeHeight(String itemType) { return super.getNodeHeight(itemType); }

    // Creation helper for text comments (sets default color to white)
    public void addTextComment(int worldX, int worldY) {
        DiagramNode comment = new DiagramNode(UUID.randomUUID(), "creatediagram:text_comment", worldX, worldY, "", 1,
                Color.WHITE.getHexValue(), 80, 20);
        nodes.add(comment);
        textModel.startEditing(comment.id(), "");
    }

    /**
     * Main render method for the canvas. Delegates drawing to the view and draws the toolbar on top.
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int paletteWidth) {
        syncToolbarState();
        this.lastPaletteWidth = paletteWidth;
        view.render(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, paletteWidth, this);
        toolbar.render(guiGraphics, mouseX, mouseY, screenWidth, screenHeight, paletteWidth, this.font);
    }

    private void syncToolbarState() {
        this.currentTool = toolbar.getCurrentTool();
        this.currentColor = toolbar.getCurrentColor();
        this.isColorMenuOpen = toolbar.isColorMenuOpen();
        this.colorMenuAnchorX = toolbar.getColorMenuAnchorX();
        this.colorMenuAnchorY = toolbar.getColorMenuAnchorY();
    }

    /** Erase stroke points near a world coordinate. */
    private void eraseAt(int wx, int wy) {
        strokes.removeIf(stroke -> {
            for (int[] p : stroke.points()) {
                if (Math.hypot(p[0] - wx, p[1] - wy) < 25.0) return true;
            }
            return false;
        });
    }

    /**
     * Input handling: click/drag/release. Delegates edge/text behavior to subcontrollers.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, int paletteWidth) {
        int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        if (toolbar.mouseClicked(mouseX, mouseY, screenWidth, screenHeight, paletteWidth)) {
            syncToolbarState();
            return true;
        }

        double worldX = getWorldX(mouseX);
        double worldY = getWorldY(mouseY);

        // Text controller handles color menu and editing clicks
        if (textController.mouseClicked(worldX, worldY, nodes, button)) return true;

        if (activeAmountField != null) {
            if (!(worldX >= activeAmountField.getX() && worldX <= activeAmountField.getX() + activeAmountField.getWidth() && worldY >= activeAmountField.getY() && worldY <= activeAmountField.getY() + activeAmountField.getHeight())) {
                closeAndSaveAmountField();
            }
        }

        if (nodeWithOpenMenu != null) {
            DiagramNode node = findNode(nodeWithOpenMenu);
            if (node != null) {
                List<String> options = EmiHelper.getValidCatalystsForMachine(node.itemType());
                int menuX = node.x() + node.width();
                int menuY = node.y();
                if (worldX >= menuX && worldX <= menuX + 20) {
                    int clickedIndex = (int) ((worldY - menuY) / 20);
                    if (clickedIndex >= 0 && clickedIndex < options.size()) {
                        int nodeIdx = nodes.indexOf(node);
                        nodes.set(nodeIdx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), options.get(clickedIndex), node.amount(), node.color(), node.width(), node.height()));
                        nodeWithOpenMenu = null;
                        return true;
                    }
                }
            }
            nodeWithOpenMenu = null;
            return true;
        }

        if (this.edgeController != null && this.edgeController.mouseClicked(worldX, worldY, this.nodes, button)) return true;

        if (button == 1) {
            boolean clickedOnNode = false;
            for (DiagramNode node : nodes) {
                if (worldX >= node.x() && worldX <= node.x() + node.width() && worldY >= node.y() && worldY <= node.y() + node.height()) {
                    if (this.edgeModel != null) this.edgeModel.removeNodeConnections(node.id());
                    nodes.remove(node);
                    clickedOnNode = true;
                    break;
                }
            }
            if (!clickedOnNode) {
                if (Screen.hasShiftDown()) {
                    addTextComment((int) worldX, (int) worldY);
                    return true;
                }
                this.isPanning = true;
            }
            return true;
        }

        if (button == 0) {
            boolean hitNode = false;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                DiagramNode node = nodes.get(i);
                int w = node.width();
                int h = node.height();
                boolean isComment = node.itemType().equals("creatediagram:text_comment");
                boolean isMachine = EmiHelper.isMachine(node.itemType());

                // Resize handle
                if (worldX >= node.x() + w - 10 && worldX <= node.x() + w && worldY >= node.y() + h - 10 && worldY <= node.y() + h) {
                    this.resizingNode = node;
                    this.resizeStartWidth = w;
                    this.resizeStartHeight = h;
                    this.resizeMouseStartX = (int) worldX;
                    this.resizeMouseStartY = (int) worldY;
                    return true;
                }

                // Amount field
                if (!isComment && !isMachine && worldX >= node.x() + w - 16 && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + 14) {
                    this.nodeWithActiveAmountField = node.id();
                    this.activeAmountField = new EditBox(this.font, node.x() + (w/2) - 15, node.y() - 16, 30, 14, Component.literal("Qty"));
                    this.activeAmountField.setValue(String.valueOf(node.amount()));
                    this.activeAmountField.setFocused(true);
                    return true;
                }

                // Catalyst menu button
                if (!isComment && worldX >= node.x() + 4 && worldX <= node.x() + 18 && worldY >= node.y() + h - 18 && worldY <= node.y() + h - 4) {
                    if (!EmiHelper.getValidCatalystsForMachine(node.itemType()).isEmpty()) {
                        this.nodeWithOpenMenu = node.id();
                        return true;
                    }
                }

                // Click / drag node
                if (worldX >= node.x() && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + h) {
                    hitNode = true;
                    long currentTime = System.currentTimeMillis();

                    if (node.id().equals(lastClickedNodeId) && (currentTime - lastClickTime) < 300) {
                        int cloneX = node.x() + node.width() + 20;
                        int cloneY = node.y() + node.height() + 20;
                        while (!isPositionValid(cloneX, cloneY, node.width(), node.height(), null)) {
                            cloneX += 20; cloneY += 20;
                        }
                        DiagramNode clonedNode = new DiagramNode(UUID.randomUUID(), node.itemType(), cloneX, cloneY, node.property(), node.amount(), node.color(), w, h);
                        nodes.add(clonedNode);
                        lastClickedNodeId = null;
                        return true;
                    }

                    lastClickedNodeId = node.id();
                    lastClickTime = currentTime;

                    if (isComment) textModel.startEditing(node.id(), node.property());

                    this.draggedNode = node;
                    this.dragStartX = node.x();
                    this.dragStartY = node.y();
                    break;
                }
            }

            if (!hitNode && !toolbar.isColorMenuOpen()) {
                if (toolbar.getCurrentTool() == Tool.PEN) {
                    currentStrokePoints = new ArrayList<>();
                    currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
                    return true;
                }
                if (toolbar.getCurrentTool() == Tool.LINE) {
                    lineStartX = (int) worldX; lineStartY = (int) worldY; lineCurrentX = lineStartX; lineCurrentY = lineStartY; isDrawingLine = true; return true;
                }
                if (toolbar.getCurrentTool() == Tool.ERASER) { eraseAt((int) worldX, (int) worldY); return true; }
            }
            if (hitNode) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isPanning) { this.offsetX += dragX; this.offsetY += dragY; return true; }
        double worldX = getWorldX(mouseX); double worldY = getWorldY(mouseY);
        if (this.edgeController != null && this.edgeController.mouseDragged(worldX, worldY, button)) return true;

        if (this.resizingNode != null) {
            int index = nodes.indexOf(this.resizingNode);
            if (index != -1) {
                int newW = this.resizeStartWidth + (int) (worldX - resizeMouseStartX);
                int newH = this.resizeStartHeight + (int) (worldY - resizeMouseStartY);
                int minW = resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
                int minH = resizingNode.itemType().equals("creatediagram:text_comment") ? 20 : (resizingNode.itemType().contains("mechanical_mixer") || resizingNode.itemType().contains("mechanical_press") ? 60 : 40);
                newW = Math.max(minW, newW); newH = Math.max(minH, newH);
                DiagramNode updatedNode = new DiagramNode(resizingNode.id(), resizingNode.itemType(), resizingNode.x(), resizingNode.y(), resizingNode.property(), resizingNode.amount(), resizingNode.color(), newW, newH);
                nodes.set(index, updatedNode); this.resizingNode = updatedNode;
            }
            return true;
        }

        if (this.draggedNode != null) {
            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) {
                DiagramNode updatedNode = new DiagramNode(draggedNode.id(), draggedNode.itemType(), (int) worldX - (draggedNode.width()/2), (int) worldY - (draggedNode.height()/2), draggedNode.property(), draggedNode.amount(), draggedNode.color(), draggedNode.width(), draggedNode.height());
                nodes.set(index, updatedNode); this.draggedNode = updatedNode;
            }
            return true;
        }

        if (toolbar.getCurrentTool() == Tool.PEN && currentStrokePoints != null) {
            int[] lastP = currentStrokePoints.get(currentStrokePoints.size() - 1);
            if (Math.hypot(worldX - lastP[0], worldY - lastP[1]) > 3.0) currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
            return true;
        }
        if (toolbar.getCurrentTool() == Tool.LINE && isDrawingLine) { lineCurrentX = (int) worldX; lineCurrentY = (int) worldY; return true; }
        if (toolbar.getCurrentTool() == Tool.ERASER) { eraseAt((int) worldX, (int) worldY); return true; }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (this.toolbar.mouseReleased(mouseX, mouseY, screenWidth, screenHeight, this.lastPaletteWidth)) return true;
        if (button == 1) this.isPanning = false;
        double worldX = getWorldX(mouseX); double worldY = getWorldY(mouseY);

        if (toolbar.getCurrentTool() == Tool.PEN && currentStrokePoints != null) {
            if (currentStrokePoints.size() > 1) strokes.add(new DiagramStroke(UUID.randomUUID(), toolbar.getCurrentColor(), currentStrokePoints));
            currentStrokePoints = null; return true;
        }

        if (toolbar.getCurrentTool() == Tool.LINE && isDrawingLine) { List<int[]> pts = List.of(new int[]{lineStartX, lineStartY}, new int[]{(int) worldX, (int) worldY}); strokes.add(new DiagramStroke(UUID.randomUUID(), toolbar.getCurrentColor(), pts)); isDrawingLine = false; return true; }
        if (this.edgeController != null && this.edgeController.mouseReleased(worldX, worldY, this.nodes, button)) return true;

        if (this.resizingNode != null) {
            int snappedW = Math.round(resizingNode.width() / 20.0f) * 20;
            int snappedH = Math.round(resizingNode.height() / 20.0f) * 20;
            int minW = resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
            int minH = resizingNode.itemType().equals("creatediagram:text_comment") ? 20 : (resizingNode.itemType().contains("mechanical_mixer") || resizingNode.itemType().contains("mechanical_press") ? 60 : 40);
            snappedW = Math.max(minW, snappedW); snappedH = Math.max(minH, snappedH);
            int index = nodes.indexOf(this.resizingNode);
            if (index != -1) nodes.set(index, new DiagramNode(resizingNode.id(), resizingNode.itemType(), resizingNode.x(), resizingNode.y(), resizingNode.property(), resizingNode.amount(), resizingNode.color(), snappedW, snappedH));
            this.resizingNode = null; return true;
        }

        if (this.draggedNode != null) {
            int snappedX = Math.round(this.draggedNode.x() / 20.0f) * 20;
            int snappedY = Math.round(this.draggedNode.y() / 20.0f) * 20;
            int currentW = this.draggedNode.width(); int currentH = this.draggedNode.height();
            if (!isPositionValid(snappedX, snappedY, currentW, currentH, this.draggedNode.id())) { snappedX = dragStartX; snappedY = dragStartY; }
            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) nodes.set(index, new DiagramNode(draggedNode.id(), draggedNode.itemType(), snappedX, snappedY, draggedNode.property(), draggedNode.amount(), draggedNode.color(), currentW, currentH));
            this.draggedNode = null; return true;
        }
        return false;
    }

    public void closeAndSaveAmountField() {
        if (nodeWithActiveAmountField != null && activeAmountField != null) {
            DiagramNode node = findNode(nodeWithActiveAmountField);
            if (node != null) {
                int idx = nodes.indexOf(node);
                int newAmount;
                try {
                    newAmount = Integer.parseInt(activeAmountField.getValue().trim());
                    if (newAmount < 1) newAmount = 1;
                    EmiStack stackInfo = EmiHelper.getStack(node.itemType());
                    int maxAllowed = stackInfo.getItemStack().isEmpty() ? 1000000 : 64;
                    if (newAmount > maxAllowed) newAmount = maxAllowed;
                } catch (NumberFormatException e) { newAmount = node.amount(); }
                nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), node.property(), newAmount, node.color(), node.width(), node.height()));
            }
        }
        this.activeAmountField = null; this.nodeWithActiveAmountField = null;
    }

    public void cancelDrag() {
        if (this.draggedNode != null) {
            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) nodes.set(index, new DiagramNode(draggedNode.id(), draggedNode.itemType(), dragStartX, dragStartY, draggedNode.property(), draggedNode.amount(), draggedNode.color(), draggedNode.width(), draggedNode.height()));
            this.draggedNode = null;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        float zoomDelta = (float) Math.signum(scrollY) * 0.15f * zoom;
        float newZoom = Mth.clamp(zoom + zoomDelta, MIN_ZOOM, MAX_ZOOM);
        if (newZoom != zoom) {
            double worldXBefore = getWorldX(mouseX); double worldYBefore = getWorldY(mouseY);
            this.zoom = newZoom; this.offsetX = mouseX - (worldXBefore * zoom); this.offsetY = mouseY - (worldYBefore * zoom);
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeAmountField != null) {
            if (keyCode == 257) { closeAndSaveAmountField(); return true; }
            return activeAmountField.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textController.isEditing()) { if (textController.keyPressed(keyCode, scanCode, modifiers)) syncEditedComment(); return true; }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (activeAmountField != null) { if (Character.isDigit(codePoint)) return activeAmountField.charTyped(codePoint, modifiers); return true; }
        if (textController.isEditing()) { if (textController.charTyped(codePoint, modifiers)) syncEditedComment(); return true; }
        return false;
    }

    public void syncEditedComment() {
        if (!textController.isEditing()) return;
        UUID editingNodeId = textController.getEditingNodeId();
        DiagramNode node = findNode(editingNodeId);
        if (node != null) {
            int idx = nodes.indexOf(node);
            nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), textController.getEditedText(), node.amount(), node.color(), node.width(), node.height()));
        }
    }

    public double getOffsetX() { return this.offsetX; }
    public double getOffsetY() { return this.offsetY; }
    public void setOffset(double x, double y) { this.offsetX = x; this.offsetY = y; }

    public void setZoom(float zoom) { this.zoom = zoom; }
}
