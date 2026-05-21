package com.emalter.creatediagram.client.diagram.canvas.save;

import com.emalter.creatediagram.component.DiagramData;
import com.emalter.creatediagram.client.diagram.canvas.CanvasController;

public final class CanvasSaveFactory {
    private CanvasSaveFactory() {}

    public static DiagramData create(CanvasController canvas) {
        return new DiagramData(
                canvas.getNodes(),
                canvas.getEdges(),
                canvas.getStrokes()
        );
    }
}
