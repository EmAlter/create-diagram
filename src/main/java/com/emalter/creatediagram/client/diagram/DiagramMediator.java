package com.emalter.creatediagram.client.diagram;

import com.emalter.creatediagram.client.diagram.canvas.CanvasController;
import com.emalter.creatediagram.client.menu.MenuController;
import com.emalter.creatediagram.client.toolbar.ToolbarController;

/**
 * Interface to coordinate the Diagram controllers
 */
public interface DiagramMediator {
    CanvasController getCanvas();
    MenuController getPalette();
    ToolbarController getToolbar();

    void onToolChanged();
    void onColorChanged();
    
}
