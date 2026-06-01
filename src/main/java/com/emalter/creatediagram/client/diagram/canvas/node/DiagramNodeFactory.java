package com.emalter.creatediagram.client.diagram.canvas.node;

import com.emalter.creatediagram.component.DiagramNode;

import java.util.UUID;

public class DiagramNodeFactory {
    public enum NodeCategory {
        TEXT, MACHINE, INPUT
    }

    public static DiagramNode createNode(NodeCategory category, String itemType, int x, int y, int width, int height) {
        return switch (category) {
            case TEXT -> new DiagramNode(UUID.randomUUID(), itemType, x, y, "", 0, 0xFFFFFF, width, height);
            case MACHINE -> new DiagramNode(UUID.randomUUID(), itemType, x, y, "", 0, 0xFFFFFF, width, height);
            case INPUT -> new DiagramNode(UUID.randomUUID(), itemType, x, y, "", 1, 0xFFFFFF, width, height);
        };
    }
}

