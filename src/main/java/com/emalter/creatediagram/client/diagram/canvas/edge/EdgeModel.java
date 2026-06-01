package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EdgeModel {
    private List<DiagramEdge> edges = new ArrayList<>();

    private DiagramNode draggingFromNode = null;
    private String draggingOutputItem = null;
    private int draggingSlotIndex = 0;
    private int mouseWorldX = 0;
    private int mouseWorldY = 0;

    private DiagramEdge edgeWithOpenSlider = null;
    private int sliderX = 0, sliderY = 0, sliderWidth = 100, sliderHeight = 10;
    private int sliderMin = 1, sliderMax = 1, sliderValue = 1;
    private boolean isDraggingSlider = false;

    public void addEdge(DiagramEdge edge) {
        if (!edges.contains(edge)) this.edges.add(edge);
    }

    public void removeNodeConnections(UUID nodeId) {
        edges.removeIf(edge -> edge.fromNode().equals(nodeId) || edge.toNode().equals(nodeId));
    }

    public void updateSliderValue(double worldX) {
        if (sliderMax <= sliderMin) { sliderValue = sliderMin; return; }
        double ratio = (worldX - sliderX) / (double) sliderWidth;
        ratio = Math.max(0, Math.min(1, ratio));
        sliderValue = sliderMin + (int) Math.round(ratio * (sliderMax - sliderMin));
    }

    // Getters and Setters
    public List<DiagramEdge> getEdges() { return this.edges; }
    public void setEdges(List<DiagramEdge> loadedEdges) { this.edges = new ArrayList<>(loadedEdges); }
    public DiagramNode getDraggingFromNode() { return draggingFromNode; }
    public void setDraggingFromNode(DiagramNode node) { this.draggingFromNode = node; }
    public String getDraggingOutputItem() { return draggingOutputItem; }
    public void setDraggingOutputItem(String item) { this.draggingOutputItem = item; }
    public int getDraggingSlotIndex() { return draggingSlotIndex; }
    public void setDraggingSlotIndex(int index) { this.draggingSlotIndex = index; }
    public int getMouseWorldX() { return mouseWorldX; }
    public void setMouseWorldX(int x) { this.mouseWorldX = x; }
    public int getMouseWorldY() { return mouseWorldY; }
    public void setMouseWorldY(int y) { this.mouseWorldY = y; }
    public DiagramEdge getEdgeWithOpenSlider() { return edgeWithOpenSlider; }
    public void setEdgeWithOpenSlider(DiagramEdge edge) { this.edgeWithOpenSlider = edge; }
    public int getSliderX() { return sliderX; }
    public void setSliderX(int x) { this.sliderX = x; }
    public int getSliderY() { return sliderY; }
    public void setSliderY(int y) { this.sliderY = y; }
    public int getSliderWidth() { return sliderWidth; }
    public void setSliderWidth(int width) { this.sliderWidth = width; }
    public int getSliderHeight() { return sliderHeight; }
    public void setSliderHeight(int height) { this.sliderHeight = height; }
    public int getSliderMin() { return sliderMin; }
    public void setSliderMin(int min) { this.sliderMin = min; }
    public int getSliderMax() { return sliderMax; }
    public void setSliderMax(int max) { this.sliderMax = max; }
    public int getSliderValue() { return sliderValue; }
    public void setSliderValue(int value) { this.sliderValue = value; }
    public boolean isDraggingSlider() { return isDraggingSlider; }
    public void setDraggingSlider(boolean dragging) { this.isDraggingSlider = dragging; }
}