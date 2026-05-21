package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import com.emalter.creatediagram.client.toolbar.Tool;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeController;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeModel;
import com.emalter.creatediagram.client.diagram.canvas.text.TextModel;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanvasModel {
	Tool currentTool = Tool.PEN;
	int currentColor = Color.WHITE.getHexValue();
	boolean isColorMenuOpen = false;
	int colorMenuAnchorX = 0;
	int colorMenuAnchorY = 0;

	String previewItemType = null;

	public record DiagramStroke(UUID id, int color, List<int[]> points) {}

	final List<DiagramStroke> strokes = new ArrayList<>();
	List<int[]> currentStrokePoints = null;

	int lineStartX = 0, lineStartY = 0;
	int lineCurrentX = 0, lineCurrentY = 0;
	boolean isDrawingLine = false;

	double offsetX = 0;
	double offsetY = 0;
	float zoom = 1.0f;
	boolean isPanning = false;
	final int gridSize = 20;

	final float MIN_ZOOM = 0.2f;
	final float MAX_ZOOM = 3.0f;

	List<DiagramNode> nodes = new ArrayList<>();
	DiagramNode draggedNode = null;
	int dragStartX = 0;
	int dragStartY = 0;

	DiagramNode resizingNode = null;
	int resizeStartWidth = 0;
	int resizeStartHeight = 0;
	int resizeMouseStartX = 0;
	int resizeMouseStartY = 0;

	long lastClickTime = 0;
	UUID lastClickedNodeId = null;

	UUID nodeWithOpenMenu = null;
	UUID nodeWithOpenColorMenu = null;

	EditBox activeAmountField = null;
	UUID nodeWithActiveAmountField = null;

	protected TextModel textModel = null;
	// Edge MVC instances (initialized by CanvasController)
	protected EdgeModel edgeModel = null;
	protected EdgeController edgeController = null;
	protected com.emalter.creatediagram.client.diagram.canvas.text.TextController textController = null;

	public Tool getCurrentTool() { return currentTool; }
	public void setCurrentTool(Tool currentTool) { this.currentTool = currentTool; }
	public int getCurrentColor() { return currentColor; }
	public void setCurrentColor(int currentColor) { this.currentColor = currentColor; }
	public boolean isColorMenuOpen() { return isColorMenuOpen; }
	public int getColorMenuAnchorX() { return colorMenuAnchorX; }
	public int getColorMenuAnchorY() { return colorMenuAnchorY; }
	public void setColorMenuOpen(boolean colorMenuOpen) { isColorMenuOpen = colorMenuOpen; }
	public void setColorMenuAnchor(int anchorX, int anchorY) {
		this.colorMenuAnchorX = anchorX;
		this.colorMenuAnchorY = anchorY;
	}

	public void setPreviewItem(String itemType) { this.previewItemType = itemType; }
	public void setNodes(List<DiagramNode> loadedNodes) { this.nodes = new ArrayList<>(loadedNodes); }
	public void addNode(DiagramNode node) { this.nodes.add(node); }
	public List<DiagramNode> getNodes() { return this.nodes; }
	public float getZoom() { return this.zoom; }
	public EdgeController getEdgeController() { return this.edgeController; }
	public java.util.List<com.emalter.creatediagram.component.DiagramEdge> getEdges() { return this.edgeModel != null ? this.edgeModel.getEdges() : java.util.List.of(); }
	public void setEdges(java.util.List<com.emalter.creatediagram.component.DiagramEdge> edges) { if (this.edgeModel != null) this.edgeModel.setEdges(edges); }
	public List<DiagramStroke> getStrokes() { return this.strokes; }
	public void setStrokes(List<DiagramStroke> loadedStrokes) {
		this.strokes.clear();
		if (loadedStrokes != null) {
			this.strokes.addAll(loadedStrokes);
		}
	}

	public int getNodeWidth(String itemType) {
		if (itemType.equals("creatediagram:text_comment")) return 60;
		if (itemType.contains("crushing_wheel")) return 80;
		return 40;
	}

	public int getNodeHeight(String itemType) {
		if (itemType.equals("creatediagram:text_comment")) return 20;
		if (itemType.contains("crushing_wheel")) return 60;
		return itemType.contains("mechanical_mixer") || itemType.contains("mechanical_press") ? 60 : 40;
	}

	public double getWorldX(double mouseX) { return (mouseX - offsetX) / zoom; }
	public double getWorldY(double mouseY) { return (mouseY - offsetY) / zoom; }

	public DiagramNode findNode(UUID id) {
		for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
		return null;
	}

	public boolean isPositionValid(int targetX, int targetY, int targetW, int targetH, UUID ignoreId) {
		for (DiagramNode node : nodes) {
			if (ignoreId != null && node.id().equals(ignoreId)) continue;
			if (targetX < node.x() + node.width() && targetX + targetW > node.x() &&
					targetY < node.y() + node.height() && targetY + targetH > node.y()) {
				return false;
			}
		}
		return true;
	}

	public void openColorMenu(int anchorX, int anchorY) {
		isColorMenuOpen = true;
		colorMenuAnchorX = anchorX;
		colorMenuAnchorY = anchorY;
	}

	public void closeColorMenu() {
		isColorMenuOpen = false;
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
				} catch (NumberFormatException e) {
					newAmount = node.amount();
				}
				nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), node.property(), newAmount, node.color(), node.width(), node.height()));
			}
		}
		this.activeAmountField = null;
		this.nodeWithActiveAmountField = null;
	}
}

