package com.emalter.creatediagram.client.diagram.canvas.node;

import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.logic.integration.ModIntegration;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Map;

public class NodeController {
    private final NodeModel model;
    private final NodeView view;

    public NodeController(NodeModel model, NodeView view) {
        this.model = model;
        this.view = view;
    }

    public List<OutputPort> getDynamicOutputs(DiagramNode node, List<DiagramNode> allNodes, List<DiagramEdge> edges) {
        return model.getDynamicOutputs(node, allNodes, edges);
    }

    public void renderNodeAndPorts(GuiGraphics guiGraphics, DiagramNode node, boolean isInvalid, List<DiagramNode> allNodes, List<DiagramEdge> edges) {
        view.renderNode(guiGraphics, node, isInvalid);
        view.renderPorts(guiGraphics, node, getDynamicOutputs(node, allNodes, edges));
    }

    public void renderCatalystMenu(GuiGraphics guiGraphics, DiagramNode node) {
        view.renderCatalystMenu(guiGraphics, node);
    }
    
    public DiagramNode cycleOutputTarget(DiagramNode machineNode, List<DiagramNode> allNodes, List<DiagramEdge> edges) {
        Map<String, Integer> inputs = model.getIncomingItemsForNode(machineNode, allNodes, edges);
        String newProp = ModIntegration.get().getNextAlternativeTarget(machineNode.itemType(), machineNode.property(), inputs);
        if (newProp != null) {
            return new DiagramNode(machineNode.id(), machineNode.itemType(), machineNode.x(), machineNode.y(), newProp, machineNode.amount(), machineNode.color(), machineNode.width(), machineNode.height());
        }
        return null;
    }
    
    public NodeModel getModel() {
        return model;
    }
    
    public NodeView getView() {
        return view;
    }
    
}