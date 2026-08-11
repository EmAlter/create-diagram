package com.emalter.creatediagram.client.diagram.canvas.node;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.logic.integration.ModIntegration;

import java.util.*;

public class NodeModel {
    private final Map<String, Boolean> machineCache = new HashMap<>();

    private class CacheEntry {
        List<OutputPort> outputs;
        long timestamp;
        CacheEntry(List<OutputPort> o, long t) { this.outputs = o; this.timestamp = t; }
    }


    private final Map<UUID, CacheEntry> outputCache = new HashMap<>();

    private boolean isMachine(String id) {
        return machineCache.computeIfAbsent(id, ModIntegration.get()::isMachine);
    }

    public List<OutputPort> getDynamicOutputs(DiagramNode node, List<DiagramNode> allNodes, List<DiagramEdge> edges) {
        long currentTime = System.currentTimeMillis();
        CacheEntry entry = outputCache.get(node.id());

        // Keeps in cache for 250ms to avoid recalculating outputs too frequently
        if (entry != null && (currentTime - entry.timestamp < 250)) {
            return entry.outputs;
        }

        List<OutputPort> result = getDynamicOutputsInternal(node, allNodes, edges, new HashSet<>());
        outputCache.put(node.id(), new CacheEntry(result, currentTime));
        return result;
    }

    private List<OutputPort> getDynamicOutputsInternal(DiagramNode node, List<DiagramNode> allNodes, List<DiagramEdge> edges, Set<UUID> visited) {
        if (visited.contains(node.id())) return List.of();
        visited.add(node.id());

        Map<String, Integer> inputs = getIncomingItems(node, allNodes, edges, visited);
        return ModIntegration.get().getOutputs(node.itemType(), node.property(), inputs);
    }

    public Map<String, Integer> getIncomingItemsForNode(DiagramNode machine, List<DiagramNode> allNodes, List<DiagramEdge> edges) {
        return getIncomingItems(machine, allNodes, edges, new HashSet<>());
    }

    private Map<String, Integer> getIncomingItems(DiagramNode machine, List<DiagramNode> allNodes, List<DiagramEdge> edges, Set<UUID> visited) {
        Map<String, Integer> incoming = new HashMap<>();
        for (DiagramEdge edge : edges) {
            if (edge.toNode().equals(machine.id())) {
                DiagramNode fromNode = findNode(allNodes, edge.fromNode());
                if (fromNode != null) {
                    String itemId;
                    int amountToAdd = 0;
                    boolean isInfinite = false;

                    if (!isMachine(fromNode.itemType())) {
                        itemId = fromNode.itemType();
                        amountToAdd = fromNode.amount();
                    } else {
                        itemId = edge.outputItem();
                        amountToAdd = edge.amount();

                        // Check if the output from the machine is infinite by traversing its outputs
                        Set<UUID> nextVisited = new HashSet<>(visited);
                        List<OutputPort> fromOutputs = getDynamicOutputsInternal(fromNode, allNodes, edges, nextVisited);
                        for (OutputPort out : fromOutputs) {
                            if (out.itemId().equals(itemId) && out.recipeType() == com.emalter.creatediagram.logic.RecipeType.INFINITE) {
                                isInfinite = true;
                                break;
                            }
                        }
                    }

                    if (isInfinite) {
                        // Used MIX.VALUE to represent infinite amount
                        incoming.put(itemId, Integer.MAX_VALUE);
                    } else if (amountToAdd > 0) {
                        int current = incoming.getOrDefault(itemId, 0);
                        if (current != Integer.MAX_VALUE) {
                            incoming.put(itemId, current + amountToAdd);
                        }
                    }
                }
            }
        }
        return incoming;
    }

    private DiagramNode findNode(List<DiagramNode> nodes, UUID id) {
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
    }
}