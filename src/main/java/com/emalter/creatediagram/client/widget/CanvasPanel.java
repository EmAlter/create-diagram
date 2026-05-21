package com.emalter.creatediagram.client.widget;
import com.emalter.creatediagram.client.diagram.canvas.CanvasController;
import net.minecraft.client.gui.Font;
/**
 * Legacy compatibility wrapper kept for callers that still reference `CanvasPanel`.
 * The real implementation now lives in `com.emalter.creatediagram.client.diagram.canvas`.
 */
@Deprecated
public class CanvasPanel extends CanvasController {
    public CanvasPanel(Font font) {
        super(font);
    }
}