package com.emalter.creatediagram.client.diagram;

import com.emalter.creatediagram.component.DiagramData;
import com.emalter.creatediagram.client.diagram.canvas.node.DiagramNodeFactory;
import com.emalter.creatediagram.component.ModDataComponents;
import com.emalter.creatediagram.logic.DiagramNetworking;
import com.emalter.creatediagram.client.diagram.canvas.CanvasController;
import com.emalter.creatediagram.client.diagram.canvas.save.CanvasSaveFactory;
import com.emalter.creatediagram.client.menu.MenuController;
import com.emalter.creatediagram.client.toolbar.ToolbarController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class DiagramScreen extends Screen implements DiagramMediator {
    private final ItemStack blueprintStack;
    private final InteractionHand hand;

    // UI components
    private CanvasController canvas;
    private MenuController palette;
    private ToolbarController toolbar;

    public DiagramScreen(ItemStack stack, InteractionHand hand) {
        super(Component.literal("Create Diagram Editor"));
        this.blueprintStack = stack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        this.canvas = new CanvasController(this.font, this);
        this.palette = new MenuController(this.height, this.font);
        this.toolbar = new ToolbarController(this);
        this.palette.init();

        DiagramData data = blueprintStack.get(ModDataComponents.DIAGRAM_DATA);
        if (data != null) {
            if (data.nodes() != null && !data.nodes().isEmpty()) {
                this.canvas.setNodes(data.nodes());
            }
            if (data.edges() != null && !data.edges().isEmpty()) {
                this.canvas.setEdges(data.edges());
            }
            if (data.strokes() != null && !data.strokes().isEmpty()) {
                this.canvas.setStrokes(data.strokes());
            }

            this.canvas.setOffset(data.offsetX(), data.offsetY());
            this.canvas.setZoom(data.zoom());
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.canvas.render(guiGraphics, mouseX, mouseY, partialTick, this.width, this.height);

        int currentPaletteWidth = this.palette.getIsOpen() ? 150 : 0;
        this.toolbar.render(guiGraphics, mouseX, mouseY, this.width, this.height, currentPaletteWidth, this.font);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        this.palette.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double commentMouseX = mouseX;
        if (button == 1 && hasShiftDown() && this.palette.getIsOpen() && mouseX <= this.palette.getWidth() + 15) {
            commentMouseX = this.palette.getWidth() + 20;
        }

        if (button == 1 && hasShiftDown()) {
            this.canvas.addTextComment((int) this.canvas.getWorldX(commentMouseX), (int) this.canvas.getWorldY(mouseY));
            this.palette.unfocusSearch();
            return true;
        }

        if (this.toolbar.mouseClicked(mouseX, mouseY, this.width, this.height, this.palette.getIsOpen() ? 150 : 0)) {
            return true;
        }

        if (this.palette.isMouseOverPanel(mouseX, mouseY)) {
            return this.palette.mouseClicked(mouseX, mouseY, button);
        } else {
            this.palette.unfocusSearch();
            boolean handled = this.canvas.mouseClicked(mouseX, mouseY, button);
            if (handled) this.setDragging(true);
            return handled;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.palette.isScrolling()) {
            this.palette.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return this.canvas.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.palette.setScrolling(false);

        if (this.toolbar.mouseReleased(mouseX, mouseY, this.width, this.height, this.palette.getIsOpen() ? 150 : 0)) {
            return true;
        }

        if (button == 0 && this.palette.getDraggingItemId() != null) {
            if (!this.palette.isMouseOverPanel(mouseX, mouseY)) {
                double worldX = canvas.getWorldX(mouseX) - 20;
                double worldY = canvas.getWorldY(mouseY) - 20;

                int snappedX = Math.round((float)worldX / 20.0f) * 20;
                int snappedY = Math.round((float)worldY / 20.0f) * 20;

                String itemId = this.palette.getDraggingItemId();
                int targetW = canvas.getNodeWidth(itemId);
                int targetH = canvas.getNodeHeight(itemId);

                while (!canvas.isPositionValid(snappedX, snappedY, targetW, targetH, null)) {
                    snappedX += 20;
                    snappedY += 20;
                }

                DiagramNodeFactory.NodeCategory category = canvas.isMachine(itemId) ?
                        DiagramNodeFactory.NodeCategory.MACHINE :
                        DiagramNodeFactory.NodeCategory.INPUT;

                this.canvas.addNode(DiagramNodeFactory.createNode(
                        category, itemId, snappedX, snappedY, targetW, targetH
                ));
            }
            this.palette.setDraggingItem(null);
            return true;
        }

        if (button == 0 && this.palette.isMouseOverPanel(mouseX, mouseY)) {
            this.canvas.cancelDrag();
        }

        this.canvas.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (palette.getIsOpen() && mouseX < palette.getWidth()) {
            return this.palette.mouseScrolled(mouseX, mouseY, scrollY);
        } else {
            return this.canvas.mouseScrolled(mouseX, mouseY, scrollY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.palette.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.canvas.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.palette.charTyped(codePoint, modifiers)) return true;
        if (this.canvas.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public CanvasController getCanvas() { return canvas; }

    @Override
    public MenuController getPalette() { return palette; }

    @Override
    public ToolbarController getToolbar() { return toolbar; }

    @Override
    public void onToolChanged() {
        if (this.canvas != null && this.toolbar != null) {
            this.canvas.setCurrentTool(this.toolbar.getCurrentTool());
        }
    }

    @Override
    public void onColorChanged() {
        if (this.canvas != null && this.toolbar != null) {
            this.canvas.setCurrentColor(this.toolbar.getCurrentColor());
        }
    }

    @Override
    public void removed() {
        super.removed();
        DiagramData newData = CanvasSaveFactory.create(this.canvas);
        DiagramNetworking.sendSavePacket(newData);
    }
}