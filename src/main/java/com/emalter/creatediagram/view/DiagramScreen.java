package com.emalter.creatediagram.view;

import com.emalter.creatediagram.component.DiagramData;
import com.emalter.creatediagram.component.DiagramEdge;
import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.ModDataComponents;
import com.emalter.creatediagram.logic.DiagramNetworking;
import com.emalter.creatediagram.view.widget.CanvasPanel;
import com.emalter.creatediagram.view.widget.PalettePanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class DiagramScreen extends Screen {
    private final ItemStack blueprintStack;
    private final InteractionHand hand;

    // UI components
    private CanvasPanel canvas;
    private PalettePanel palette;

    public DiagramScreen(ItemStack stack, InteractionHand hand) {
        super(Component.literal("Editor Diagrammi Create"));
        this.blueprintStack = stack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        this.canvas = new CanvasPanel(this.font);
        this.palette = new PalettePanel(this.height, this.font);
        this.palette.init();

        DiagramData data = blueprintStack.get(ModDataComponents.DIAGRAM_DATA);
        if (data != null) {
            if (data.nodes() != null && !data.nodes().isEmpty()) {
                this.canvas.setNodes(data.nodes());
            }
            if (data.edges() != null && !data.edges().isEmpty()) {
                this.canvas.getConnectionManager().setEdges(data.edges());
            }
            // Load saved freehand strokes if present
            if (data.strokes() != null && !data.strokes().isEmpty()) {
                this.canvas.setStrokes(data.strokes());
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Compute palette width for this frame
        int currentPaletteWidth = this.palette.getIsOpen() ? 150 : 0;

        String draggingId = this.palette.getDraggingItemId();
        this.canvas.setPreviewItem(draggingId);

        // 2. Pass the updated width to the canvas so the toolbar stays centered
        this.canvas.render(guiGraphics, mouseX, mouseY, partialTick, this.width, this.height, currentPaletteWidth);

        // 3. Elevate the palette Z so it overlays canvas elements
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        this.palette.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }

    // Input routing: forward events to palette or canvas appropriately

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.palette.isMouseOverPanel(mouseX, mouseY)) {
            return this.palette.mouseClicked(mouseX, mouseY, button);
        } else {
            this.palette.unfocusSearch();

            // Use palette state to determine open width
            int paletteWidth = this.palette.getIsOpen() ? 150 : 0;

            boolean handled = this.canvas.mouseClicked(mouseX, mouseY, button, paletteWidth);
            if (handled) this.setDragging(true);
            return handled;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // If the palette is handling scroll, forward drag to it
        if (this.palette.isScrolling()) {
            this.palette.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        // Otherwise, pass the drag to the canvas
        return this.canvas.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.palette.setScrolling(false);

        // 1. Dropping a new item from the palette into the canvas
        if (button == 0 && this.palette.getDraggingItemId() != null) {
            if (!this.palette.isMouseOverPanel(mouseX, mouseY)) {
                double worldX = canvas.getWorldX(mouseX) - 20;
                double worldY = canvas.getWorldY(mouseY) - 20;

                int snappedX = Math.round((float)worldX / 20.0f) * 20;
                int snappedY = Math.round((float)worldY / 20.0f) * 20;

                // Extract the dragging item id before validation
                String itemId = this.palette.getDraggingItemId();

                int targetW = canvas.getNodeWidth(itemId);
                int targetH = canvas.getNodeHeight(itemId);

                while (!canvas.isPositionValid(snappedX, snappedY, targetW, targetH, null)) {
                    snappedX += 20;
                    snappedY += 20;
                }

                // Add node with explicit width and height parameters
                this.canvas.addNode(new DiagramNode(
                        UUID.randomUUID(),
                        itemId,
                        snappedX,
                        snappedY,
                        "",
                        1,
                        0xFFFFFF, // default color for nodes
                        targetW,
                        targetH
                ));
            }
            this.palette.setDraggingItem(null); // reset dragging selection
            return true;
        }

        // 2. Releasing an existing node over the palette cancels the drag
        if (button == 0 && this.palette.isMouseOverPanel(mouseX, mouseY)) {
            this.canvas.cancelDrag();
        }

        this.canvas.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < palette.getWidth()) {
            return this.palette.mouseScrolled(mouseX, mouseY, scrollY);
        } else {
            return this.canvas.mouseScrolled(mouseX, mouseY, scrollY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.palette.keyPressed(keyCode, scanCode, modifiers)) return true;
        // Forward to canvas (enter/delete handling)
        if (this.canvas.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.palette.charTyped(codePoint, modifiers)) return true;
        // Forward typed chars to canvas (numeric fields)
        if (this.canvas.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        super.removed();

        List<DiagramNode> currentNodes = this.canvas.getNodes();
        List<DiagramEdge> currentEdges = this.canvas.getConnectionManager().getEdges();
        List<CanvasPanel.DiagramStroke> currentStrokes = this.canvas.getStrokes();

        // The constructor now includes strokes as part of the saved data
        DiagramData newData = new DiagramData(currentNodes, currentEdges, currentStrokes);

        DiagramNetworking.sendSavePacket(newData);
    }
}