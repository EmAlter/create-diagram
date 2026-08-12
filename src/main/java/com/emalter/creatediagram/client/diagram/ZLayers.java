package com.emalter.creatediagram.client.diagram;

/**
 * Manage the Z layers for rendering different components of the diagram.
 * Values are strongly spaced to absorb the native +150 offset forcibly applied by Minecraft during Item rendering.
 */
public class ZLayers {

    // Node rendering
    public static final int NODE_CONTENT = 10;   // Items are rendered at Z=160, so we need to be above that for the node content
    public static final int NODE_OVERLAY = 200;  // Text and other overlays on top of the node content
    
    // Temporary offsets for dragging and popups
    public static final int DRAG_OFFSET = 250;

    // Canvas popups (like color menu, suggestions, etc.)
    public static final int CANVAS_POPUP = 500;
    public static final int CANVAS_POPUP_CONTENT = 510; // Items are rendered at Z=660

    // Main UI
    public static final int SIDE_MENU = 700; // Side menu and other UI elements

    // Above all for zoomed-in content
    public static final int ZOOM = 900;
    public static final int TOOLTIP = 1000;
}