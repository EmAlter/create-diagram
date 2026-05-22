package com.emalter.creatediagram.client.diagram.canvas.edge;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.logic.RecipeEngine;
import com.emalter.creatediagram.component.RecipeOutput;


import java.util.*;

/**
 * Model for edge management in the canvas. Manages all state related to diagram edges,
 * connection logic, and recipe output calculations.
 */
public class EdgeModel {
    private List<DiagramEdge> edges = new ArrayList<>();
    private final RecipeEngine recipeEngine = new RecipeEngine();

    // Dragging state
    private DiagramNode draggingFromNode = null;
    private String draggingOutputItem = null;
    private int draggingSlotIndex = 0;
    private int mouseWorldX = 0;
    private int mouseWorldY = 0;

    // Slider state (quantity selector)
    private DiagramEdge edgeWithOpenSlider = null;
    private int sliderX = 0;
    private int sliderY = 0;
    private int sliderWidth = 100;
    private int sliderHeight = 10;
    private int sliderMin = 1;
    private int sliderMax = 1;
    private int sliderValue = 1;
    private boolean isDraggingSlider = false;
    
    private final Map<UUID, List<RecipeOutput>> outputCache = new HashMap<>();
    private long lastCacheTime = 0;
    private long lastCleanTime = 0;

    public void addEdge(DiagramEdge edge) {
        if (!edges.contains(edge)) this.edges.add(edge);
    }

    private List<RecipeOutput> getDynamicOutputsInternal(DiagramNode node, List<DiagramNode> allNodes, Set<UUID> visited) {
        if (visited.contains(node.id())) return List.of();
        visited.add(node.id());

        String id = node.itemType();
        Map<String, Integer> inputs = getIncomingItems(node, allNodes, visited);
        return recipeEngine.getOutputs(id, node.property(), inputs);
    }

    public List<RecipeOutput> getDynamicOutputs(DiagramNode node, List<DiagramNode> allNodes) {
        long currentTime = System.currentTimeMillis();

        // Svuota la cache se è passato un po' di tempo (es. 250ms)
        // In questo modo le ricette si aggiornano se colleghi un nuovo cavo, ma non 60 volte al secondo!
        if (currentTime - lastCacheTime > 250) {
            outputCache.clear();
            lastCacheTime = currentTime;
        }

        // Se abbiamo già calcolato la ricetta per questo nodo di recente, restituisci il salvataggio istantaneo!
        if (outputCache.containsKey(node.id())) {
            return outputCache.get(node.id());
        }

        // Altrimenti, fai il calcolo pesante
        List<RecipeOutput> result = getDynamicOutputsInternal(node, allNodes, new HashSet<>());

        // Salva il risultato nella cache per le prossime richieste
        outputCache.put(node.id(), result);
        return result;
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
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanTime < 250) return;
        lastCleanTime = currentTime;

        for (DiagramNode node : nodes) {
            cleanOrphanEdges(node.id(), nodes);
        }
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

    public void updateSliderValue(double worldX) {
        if (sliderMax <= sliderMin) {
            sliderValue = sliderMin;
            return;
        }
        double ratio = (worldX - sliderX) / (double) sliderWidth;
        ratio = Math.max(0, Math.min(1, ratio));
        sliderValue = sliderMin + (int) Math.round(ratio * (sliderMax - sliderMin));
    }

    public void removeNodeConnections(UUID nodeId) {
        edges.removeIf(edge -> edge.fromNode().equals(nodeId) || edge.toNode().equals(nodeId));
    }

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
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

    public int getRemainingOutputAmountForController(DiagramNode fromNode, String outputItem, List<DiagramNode> allNodes) {
        return getRemainingOutputAmount(fromNode, outputItem, allNodes);
    }
}







