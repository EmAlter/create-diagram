package com.emalter.creatediagram.client.diagram.canvas;

import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeController;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeModel;
import com.emalter.creatediagram.client.diagram.canvas.edge.EdgeView;
import com.emalter.creatediagram.client.diagram.canvas.text.TextController;
import com.emalter.creatediagram.client.diagram.canvas.text.TextModel;
import com.emalter.creatediagram.client.diagram.canvas.text.TextView;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.logic.EmiHelper;
import com.emalter.creatediagram.client.diagram.Color;
import com.emalter.creatediagram.client.toolbar.Tool;
import com.emalter.creatediagram.client.toolbar.ToolbarController;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * CanvasPanel is the central UI component that manages nodes, drawing strokes, panning/zooming and
 * delegates connection rendering and interaction to a ConnectionManager.
 */
public class CanvasController extends CanvasModel {
    private final ToolbarController toolbar = new ToolbarController();
    private final CanvasView view;
    private final Font font;
    //private final TextController textController;
    // Last known palette width (set every frame) so we can compute toolbar positions on mouse release
    private int lastPaletteWidth = 0;

    public CanvasController(Font font) {
        this.font = font;
        this.view = new CanvasView(font);
        EdgeModel em = new EdgeModel();
        EdgeView ev = new EdgeView(em);
        EdgeController ec = new EdgeController(em, ev);
        this.edgeModel = em;
        this.edgeController = ec;
        // Initialize Text MVC
        TextModel tm = new TextModel();
        TextView tv = new TextView(font);
        TextController tc = new TextController(tm, tv);
        this.textModel = tm;
        this.textController = tc;
    }

    public void setPreviewItem(String itemType) { this.previewItemType = itemType; }
    public void setNodes(List<DiagramNode> loadedNodes) { this.nodes = new ArrayList<>(loadedNodes); }
    public void addNode(DiagramNode node) { this.nodes.add(node); }
    public List<DiagramNode> getNodes() { return this.nodes; }
    public float getZoom() { return this.zoom; }
    public EdgeController getEdgeController() { return this.edgeController; }
    // Text controller accessor
    public TextController getTextController() { return this.textController; }

    public int getNodeWidth(String itemType) {
        if (itemType.equals("creatediagram:text_comment")) return 60;
        if (itemType.contains("crushing_wheel")) return 80;
        return 40;
    }

    public int getNodeHeight(String itemType) {
        if (itemType.equals("creatediagram:text_comment")) return 20;
        if (itemType.contains("crushing_wheel")) return 60;
        return itemType.contains("mechanical_mixer") || itemType.contains("mechanical_press") ? 60 : 40;
    }

    public double getWorldX(double mouseX) { return (mouseX - offsetX) / zoom; }
    public double getWorldY(double mouseY) { return (mouseY - offsetY) / zoom; }

    public DiagramNode findNode(UUID id) {
        for (DiagramNode n : nodes) if (n.id().equals(id)) return n;
        return null;
    }

    private boolean isMouseOverNode(double worldX, double worldY) {
        for (DiagramNode node : nodes) {
            if (worldX >= node.x() && worldX <= node.x() + node.width() &&
                    worldY >= node.y() && worldY <= node.y() + node.height()) {
                return true;
            }
        }
        return false;
    }

    public void addTextComment(int worldX, int worldY) {
        DiagramNode comment = new DiagramNode(UUID.randomUUID(), "creatediagram:text_comment", worldX, worldY, "", 1,
                toolbar.getCurrentColor(), 80, 20);
        nodes.add(comment);
        textModel.startEditing(comment.id(), "");
    }

    public boolean isPositionValid(int targetX, int targetY, int targetW, int targetH, UUID ignoreId) {
        for (DiagramNode node : nodes) {
            if (ignoreId != null && node.id().equals(ignoreId)) continue;
            if (targetX < node.x() + node.width() && targetX + targetW > node.x() &&
                    targetY < node.y() + node.height() && targetY + targetH > node.y()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Main render method for the canvas. Delegates drawing to the diagram and draws the toolbar on top.
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int paletteWidth) {
        syncToolbarState();
        // remember palette width for later mouse release events
        this.lastPaletteWidth = paletteWidth;
        view.render(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, paletteWidth, this);
        toolbar.render(guiGraphics, mouseX, mouseY, screenWidth, screenHeight, paletteWidth, this.font);
    }

    private void syncToolbarState() {
        this.currentTool = toolbar.getCurrentTool();
        this.currentColor = toolbar.getCurrentColor();
        this.isColorMenuOpen = toolbar.isColorMenuOpen();
        this.colorMenuAnchorX = toolbar.getColorMenuAnchorX();
        this.colorMenuAnchorY = toolbar.getColorMenuAnchorY();
    }

    /**
     * Draws the snapping grid based on the current zoom and offsets.
     */
    private void drawGrid(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        int color = 0x33333333;
        float scaledGridSize = gridSize * zoom;
        int firstLineX = (int) (offsetX % scaledGridSize);
        if (firstLineX < 0) firstLineX += scaledGridSize;
        int firstLineY = (int) (offsetY % scaledGridSize);
        if (firstLineY < 0) firstLineY += scaledGridSize;

        for (float x = firstLineX; x < screenWidth; x += scaledGridSize) guiGraphics.fill((int)x, 0, (int)x + 1, screenHeight, color);
        for (float y = firstLineY; y < screenHeight; y += scaledGridSize) guiGraphics.fill(0, (int)y, screenWidth, (int)y + 1, color);
    }

    /**
     * Draws a freehand stroke by connecting consecutive points with thin lines.
     */
    private void drawStroke(GuiGraphics guiGraphics, DiagramStroke stroke) {
        if (stroke.points().size() < 2) return;
        for (int i = 0; i < stroke.points().size() - 1; i++) {
            int[] p1 = stroke.points().get(i);
            int[] p2 = stroke.points().get(i + 1);
            drawFastLine(guiGraphics, p1[0], p1[1], p2[0], p2[1], stroke.color());
        }
    }

    /**
     * Draws a straight anti-aliased line between two points using a rotated rectangle technique.
     */
    private void drawFastLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);
        gui.pose().pushPose();
        gui.pose().translate(x1, y1, 0);
        gui.pose().mulPose(com.mojang.math.Axis.ZP.rotation(angle));
        gui.fill(0, -1, (int) Math.ceil(length), 1, color);
        gui.pose().popPose();
    }

    /**
     * Renders a single diagram node: either a text comment or an item/machine node, including overlays like
     * quantity, catalyst slot and resize handle.
     */
    private void drawNode(GuiGraphics guiGraphics, DiagramNode node) {
        int w = node.width();
        int h = node.height();
        boolean isInvalid = (node == draggedNode) && !isPositionValid(node.x(), node.y(), w, h, node.id());

        if (node.itemType().equals("creatediagram:text_comment")) {
            // Delegate text node rendering to TextController
            textController.render(guiGraphics, List.of(node), (draggedNode != null) ? draggedNode.id() : null);
            return;
        } else {
            int bgColor = isInvalid ? 0x88FF0000 : 0xFF333333;
            guiGraphics.fill(node.x(), node.y(), node.x() + w, node.y() + h, bgColor);
            guiGraphics.renderOutline(node.x(), node.y(), w, h, 0xFF888888);

            ResourceLocation resId = ResourceLocation.parse(node.itemType());
            String path = resId.getPath();
            boolean hasCatalyst = !EmiHelper.getValidCatalystsForMachine(node.itemType()).isEmpty();
            boolean isMachine = EmiHelper.isMachine(node.itemType());

            float baseW = 40f;
            float baseH = path.equals("mechanical_mixer") || path.equals("mechanical_press") ? 60f : 40f;
            float scaleX = w / baseW;
            float scaleY = h / baseH;
            float imgScale = Math.min(scaleX, scaleY) * 2.0f;

            if (path.equals("mechanical_mixer") || path.equals("mechanical_press")) {
                EmiStack basin = EmiHelper.getStack("create:basin");
                float bScale = (w / 40f) * 2.0f;
                float bx = node.x() + (w - (16 * bScale)) / 2f;
                float by = node.y() + (h - (16 * bScale)) / 2f + (10 * (h/60f));

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(bx, by, 0);
                guiGraphics.pose().scale(bScale, bScale, 1.0f);
                basin.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            }

            EmiStack emiStack = EmiHelper.getStack(node.itemType());

            if (path.contains("crushing_wheel")) {
                float cwScale = (w / 80f) * 2.0f;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + (w/2f) - (18 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
                guiGraphics.pose().scale(cwScale, cwScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + (w/2f) + (2 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
                guiGraphics.pose().scale(cwScale, cwScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            } else {
                float ix = node.x() + (w - (16 * imgScale)) / 2f;
                float iy = node.y() + (h - (16 * imgScale)) / 2f - ((path.equals("mechanical_mixer") || path.equals("mechanical_press")) ? (10 * scaleY) : 0);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(ix, iy, 1);
                guiGraphics.pose().scale(imgScale, imgScale, 1.0f);
                emiStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            }

            // --- NIENTE NUMERO QUANTITA PER I MACCHINARI (SINGLETON) ---
            if (!isMachine) {
                float txtScale = Math.max(1.0f, Math.min(scaleX, scaleY) * 0.8f);
                String qtyText;
                if (emiStack.getItemStack().isEmpty()) {
                    qtyText = node.amount() + "mB";
                } else {
                    qtyText = String.valueOf(node.amount());
                }

                int textWidth = this.font.width(qtyText);
                int textColor = node.amount() > 1 ? 0xFFFFFF : 0x888888;

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(node.x() + w - (4 * txtScale), node.y() + (4 * txtScale), 250);
                guiGraphics.pose().scale(txtScale, txtScale, 1.0f);
                guiGraphics.drawString(this.font, qtyText, -textWidth, 0, textColor, true);
                guiGraphics.pose().popPose();
            }

            if (hasCatalyst) {
                int slotX = node.x() + 4;
                int slotY = node.y() + h - 18;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 250);
                guiGraphics.fill(slotX, slotY, slotX + 14, slotY + 14, 0xFF111111);
                guiGraphics.renderOutline(slotX, slotY, 14, 14, 0xFFFFAA00);

                if (node.property() != null && !node.property().isEmpty() && node.property().contains(":")) {
                    EmiStack catStack = EmiHelper.getStack(node.property());
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(slotX + 3, slotY + 3, 10);
                    guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                    catStack.render(guiGraphics, 0, 0, 0f);
                    guiGraphics.pose().popPose();
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 10);
                    guiGraphics.drawString(this.font, "?", slotX + 4, slotY + 3, 0x888888, false);
                    guiGraphics.pose().popPose();
                }
                guiGraphics.pose().popPose();
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 250);
        guiGraphics.fill(node.x() + w - 6, node.y() + h - 2, node.x() + w - 2, node.y() + h, 0xFF999999);
        guiGraphics.fill(node.x() + w - 2, node.y() + h - 6, node.x() + w, node.y() + h, 0xFF999999);
        guiGraphics.pose().popPose();
    }

    /**
     * Draws a small popup menu showing valid catalysts for the specified machine node.
     */
    private void drawCatalystMenu(GuiGraphics guiGraphics, UUID nodeId) {
        DiagramNode node = findNode(nodeId);
        if (node == null) return;
        List<String> options = EmiHelper.getValidCatalystsForMachine(node.itemType());
        int menuX = node.x() + node.width();
        int menuY = node.y();
        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (options.size() * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, options.size() * 20, 0xFFFFAA00);
        for (int i = 0; i < options.size(); i++) {
            EmiStack stack = EmiHelper.getStack(options.get(i));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Draws a palette popup for selecting a color for a comment node.
     */
    private void drawNodeColorMenu(GuiGraphics guiGraphics, UUID nodeId) {
        DiagramNode node = findNode(nodeId);
        if (node == null) return;
        com.emalter.creatediagram.client.diagram.Color[] colors = com.emalter.creatediagram.client.diagram.Color.values();
        int menuX = node.x() + node.width() + 20;
        int menuY = node.y() + (node.height() / 2) - 8;
        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (colors.length * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, colors.length * 20, 0xFFFFAA00);
        for (int i = 0; i < colors.length; i++) {
            EmiStack stack = EmiHelper.getStack(colors[i].getDyeId());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Erases any stroke points within a radius of the given world coordinates.
     */
    private void eraseAt(int wx, int wy) {
        strokes.removeIf(stroke -> {
            for (int[] p : stroke.points()) {
                if (Math.hypot(p[0] - wx, p[1] - wy) < 25.0) return true;
            }
            return false;
        });
    }

    /**
     * Handles mouse click events on the canvas area. This method manages node selection, creation, opening
     * of small menus, starting drawing operations and delegation to the toolbar and connection manager.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, int paletteWidth) {
        int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        if (toolbar.mouseClicked(mouseX, mouseY, screenWidth, screenHeight, paletteWidth)) {
            syncToolbarState();
            return true;
        }

        double worldX = getWorldX(mouseX);
        double worldY = getWorldY(mouseY);

        // Deleghiamo al textController (ora gestisce solo il colore e smette di editare)
        if (textController.mouseClicked(worldX, worldY, nodes, button)) {
            return true;
        }

        if (activeAmountField != null) {
            if (!(worldX >= activeAmountField.getX() && worldX <= activeAmountField.getX() + activeAmountField.getWidth() && worldY >= activeAmountField.getY() && worldY <= activeAmountField.getY() + activeAmountField.getHeight())) {
                closeAndSaveAmountField();
            }
        }

        if (nodeWithOpenMenu != null) {
            DiagramNode node = findNode(nodeWithOpenMenu);
            if (node != null) {
                List<String> options = EmiHelper.getValidCatalystsForMachine(node.itemType());
                int menuX = node.x() + node.width();
                int menuY = node.y();
                if (worldX >= menuX && worldX <= menuX + 20) {
                    int clickedIndex = (int) ((worldY - menuY) / 20);
                    if (clickedIndex >= 0 && clickedIndex < options.size()) {
                        int nodeIdx = nodes.indexOf(node);
                        nodes.set(nodeIdx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), options.get(clickedIndex), node.amount(), node.color(), node.width(), node.height()));
                        nodeWithOpenMenu = null;
                        return true;
                    }
                }
            }
            nodeWithOpenMenu = null;
            return true;
        }

        if (this.edgeController != null && this.edgeController.mouseClicked(worldX, worldY, this.nodes, button)) return true;

        if (button == 1) {
            boolean clickedOnNode = false;
            for (DiagramNode node : nodes) {
                if (worldX >= node.x() && worldX <= node.x() + node.width() && worldY >= node.y() && worldY <= node.y() + node.height()) {
                    if (this.edgeModel != null) this.edgeModel.removeNodeConnections(node.id());
                    nodes.remove(node);
                    clickedOnNode = true;
                    break;
                }
            }
            if (!clickedOnNode) {
                if (Screen.hasShiftDown()) {
                    addTextComment((int) worldX, (int) worldY);
                    return true;
                }
                this.isPanning = true;
            }
            return true;
        }

        if (button == 0) {
            boolean hitNode = false;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                DiagramNode node = nodes.get(i);
                int w = node.width();
                int h = node.height();
                boolean isComment = node.itemType().equals("creatediagram:text_comment");
                boolean isMachine = EmiHelper.isMachine(node.itemType());

                // 1. Controllo Ridimensionamento (Prima del Drag)
                if (worldX >= node.x() + w - 10 && worldX <= node.x() + w && worldY >= node.y() + h - 10 && worldY <= node.y() + h) {
                    this.resizingNode = node;
                    this.resizeStartWidth = w;
                    this.resizeStartHeight = h;
                    this.resizeMouseStartX = (int) worldX;
                    this.resizeMouseStartY = (int) worldY;
                    return true;
                }

                // ... check quantità (invariato)
                if (!isComment && !isMachine && worldX >= node.x() + w - 16 && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + 14) {
                    this.nodeWithActiveAmountField = node.id();
                    this.activeAmountField = new EditBox(this.font, node.x() + (w/2) - 15, node.y() - 16, 30, 14, Component.literal("Qty"));
                    this.activeAmountField.setValue(String.valueOf(node.amount()));
                    this.activeAmountField.setFocused(true);
                    return true;
                }

                // ... check menu catalizzatore (invariato)
                if (!isComment && worldX >= node.x() + 4 && worldX <= node.x() + 18 && worldY >= node.y() + h - 18 && worldY <= node.y() + h - 4) {
                    if (!EmiHelper.getValidCatalystsForMachine(node.itemType()).isEmpty()) {
                        this.nodeWithOpenMenu = node.id();
                        return true;
                    }
                }

                // 2. Controllo Drag & Click sul Nodo
                if (worldX >= node.x() && worldX <= node.x() + w && worldY >= node.y() && worldY <= node.y() + h) {
                    hitNode = true;
                    long currentTime = System.currentTimeMillis();

                    // Logica di Clonazione
                    if (node.id().equals(lastClickedNodeId) && (currentTime - lastClickTime) < 300) {
                        int cloneX = node.x() + node.width() + 20;
                        int cloneY = node.y() + node.height() + 20;

                        while (!isPositionValid(cloneX, cloneY, node.width(), node.height(), null)) {
                            cloneX += 20;
                            cloneY += 20;
                        }

                        DiagramNode clonedNode = new DiagramNode(
                                UUID.randomUUID(), node.itemType(), cloneX, cloneY,
                                node.property(), node.amount(), node.color(), w, h
                        );
                        nodes.add(clonedNode);
                        lastClickedNodeId = null;
                        return true;
                    }
                    lastClickedNodeId = node.id();
                    lastClickTime = currentTime;

                    // --- NUOVO: Riabilitato l'inizio della digitazione se clicco su un commento ---
                    if (isComment) {
                        textModel.startEditing(node.id(), node.property());
                    }

                    this.draggedNode = node;
                    this.dragStartX = node.x();
                    this.dragStartY = node.y();
                    break;
                }
            }

            if (!hitNode && !toolbar.isColorMenuOpen()) {
                if (toolbar.getCurrentTool() == Tool.PEN) {
                    currentStrokePoints = new ArrayList<>();
                    currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
                    return true;
                }
                if (toolbar.getCurrentTool() == Tool.LINE) {
                    lineStartX = (int) worldX;
                    lineStartY = (int) worldY;
                    lineCurrentX = lineStartX;
                    lineCurrentY = lineStartY;
                    isDrawingLine = true;
                    return true;
                }
                if (toolbar.getCurrentTool() == Tool.ERASER) {
                    eraseAt((int) worldX, (int) worldY);
                    return true;
                }
            }
            if (hitNode) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isPanning) {
            this.offsetX += dragX;
            this.offsetY += dragY;
            return true;
        }

        double worldX = getWorldX(mouseX);
        double worldY = getWorldY(mouseY);

        if (this.edgeController != null && this.edgeController.mouseDragged(worldX, worldY, button)) return true;

        if (this.resizingNode != null) {
            int index = nodes.indexOf(this.resizingNode);
            if (index != -1) {
                int newW = this.resizeStartWidth + (int) (worldX - resizeMouseStartX);
                int newH = this.resizeStartHeight + (int) (worldY - resizeMouseStartY);
                int minW = resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
                int minH = resizingNode.itemType().equals("creatediagram:text_comment") ? 60 : (resizingNode.itemType().contains("mechanical_mixer") || resizingNode.itemType().contains("mechanical_press") ? 60 : 40);

                newW = Math.max(minW, newW);
                newH = Math.max(minH, newH);

                DiagramNode updatedNode = new DiagramNode(
                        resizingNode.id(), resizingNode.itemType(), resizingNode.x(), resizingNode.y(),
                        resizingNode.property(), resizingNode.amount(), resizingNode.color(), newW, newH
                );
                nodes.set(index, updatedNode);
                this.resizingNode = updatedNode;
            }
            return true;
        }

        if (this.draggedNode != null) {
            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) {
                DiagramNode updatedNode = new DiagramNode(
                        draggedNode.id(), draggedNode.itemType(),
                        (int) worldX - (draggedNode.width()/2), (int) worldY - (draggedNode.height()/2),
                        draggedNode.property(), draggedNode.amount(), draggedNode.color(), draggedNode.width(), draggedNode.height()
                );
                nodes.set(index, updatedNode);
                this.draggedNode = updatedNode;
            }
            return true;
        }

        if (toolbar.getCurrentTool() == Tool.PEN && currentStrokePoints != null) {
            int[] lastP = currentStrokePoints.get(currentStrokePoints.size() - 1);
            if (Math.hypot(worldX - lastP[0], worldY - lastP[1]) > 3.0) {
                currentStrokePoints.add(new int[]{(int) worldX, (int) worldY});
            }
            return true;
        }

        if (toolbar.getCurrentTool() == Tool.LINE && isDrawingLine) {
            lineCurrentX = (int) worldX;
            lineCurrentY = (int) worldY;
            return true;
        }

        if (toolbar.getCurrentTool() == Tool.ERASER) {
            eraseAt((int) worldX, (int) worldY);
            return true;
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        // First, give toolbar a chance to handle release (for press-and-hold color selection)
        int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (this.toolbar.mouseReleased(mouseX, mouseY, screenWidth, screenHeight, this.lastPaletteWidth)) return true;

        if (button == 1) this.isPanning = false;

        double worldX = getWorldX(mouseX);
        double worldY = getWorldY(mouseY);

        if (toolbar.getCurrentTool() == Tool.PEN && currentStrokePoints != null) {
            if (currentStrokePoints.size() > 1) {
                strokes.add(new DiagramStroke(UUID.randomUUID(), toolbar.getCurrentColor(), currentStrokePoints));
            }
            currentStrokePoints = null;
            return true;
        }

        if (toolbar.getCurrentTool() == Tool.LINE && isDrawingLine) {
            List<int[]> pts = List.of(new int[]{lineStartX, lineStartY}, new int[]{(int) worldX, (int) worldY});
            strokes.add(new DiagramStroke(UUID.randomUUID(), toolbar.getCurrentColor(), pts));
            isDrawingLine = false;
            return true;
        }

        if (this.edgeController != null && this.edgeController.mouseReleased(worldX, worldY, this.nodes, button)) return true;

        if (this.resizingNode != null) {
            int snappedW = Math.round(resizingNode.width() / 20.0f) * 20;
            int snappedH = Math.round(resizingNode.height() / 20.0f) * 20;
            int minW = resizingNode.itemType().equals("creatediagram:text_comment") ? 80 : 40;
            int minH = resizingNode.itemType().equals("creatediagram:text_comment") ? 60 : (resizingNode.itemType().contains("mechanical_mixer") || resizingNode.itemType().contains("mechanical_press") ? 60 : 40);

            snappedW = Math.max(minW, snappedW);
            snappedH = Math.max(minH, snappedH);

            int index = nodes.indexOf(this.resizingNode);
            if (index != -1) {
                nodes.set(index, new DiagramNode(
                        resizingNode.id(), resizingNode.itemType(), resizingNode.x(), resizingNode.y(),
                        resizingNode.property(), resizingNode.amount(), resizingNode.color(), snappedW, snappedH
                ));
            }
            this.resizingNode = null;
            return true;
        }

        if (this.draggedNode != null) {
            int snappedX = Math.round(this.draggedNode.x() / 20.0f) * 20;
            int snappedY = Math.round(this.draggedNode.y() / 20.0f) * 20;

            // Snap drag: read current width/height before committing the snapped position
            int currentW = this.draggedNode.width();
            int currentH = this.draggedNode.height();

                    if (!isPositionValid(snappedX, snappedY, currentW, currentH, this.draggedNode.id())) {
                snappedX = dragStartX;
                snappedY = dragStartY;
            }

            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) {
                nodes.set(index, new DiagramNode(
                        draggedNode.id(), draggedNode.itemType(), snappedX, snappedY,
                        draggedNode.property(), draggedNode.amount(), draggedNode.color(), currentW, currentH
                ));
            }
            this.draggedNode = null;
            return true;
        }
        return false;
    }

    public void closeAndSaveAmountField() {
        if (nodeWithActiveAmountField != null && activeAmountField != null) {
            DiagramNode node = findNode(nodeWithActiveAmountField);
            if (node != null) {
                int idx = nodes.indexOf(node);
                int newAmount = 1;
                try {
                    newAmount = Integer.parseInt(activeAmountField.getValue().trim());
                    if (newAmount < 1) newAmount = 1;

                            // Hybrid limit: physical items max 64, fluids/gases max 1,000,000
                    EmiStack stackInfo = EmiHelper.getStack(node.itemType());
                    int maxAllowed = stackInfo.getItemStack().isEmpty() ? 1000000 : 64;
                    if (newAmount > maxAllowed) newAmount = maxAllowed;

                } catch (NumberFormatException e) { newAmount = node.amount(); }
                nodes.set(idx, new DiagramNode(node.id(), node.itemType(), node.x(), node.y(), node.property(), newAmount, node.color(), node.width(), node.height()));
            }
        }
        this.activeAmountField = null;
        this.nodeWithActiveAmountField = null;
    }

    public void cancelDrag() {
        if (this.draggedNode != null) {
            int index = nodes.indexOf(this.draggedNode);
            if (index != -1) {
                nodes.set(index, new DiagramNode(draggedNode.id(), draggedNode.itemType(), dragStartX, dragStartY, draggedNode.property(), draggedNode.amount(), draggedNode.color(), draggedNode.width(), draggedNode.height()));
            }
            this.draggedNode = null;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        float zoomDelta = (float) Math.signum(scrollY) * 0.15f * zoom;
        float newZoom = Mth.clamp(zoom + zoomDelta, MIN_ZOOM, MAX_ZOOM);

        if (newZoom != zoom) {
            double worldXBefore = getWorldX(mouseX);
            double worldYBefore = getWorldY(mouseY);
            this.zoom = newZoom;
            this.offsetX = mouseX - (worldXBefore * zoom);
            this.offsetY = mouseY - (worldYBefore * zoom);
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeAmountField != null) {
            if (keyCode == 257) { closeAndSaveAmountField(); return true; }
            return activeAmountField.keyPressed(keyCode, scanCode, modifiers);
        }

        if (textController.isEditing()) {
            if (textController.keyPressed(keyCode, scanCode, modifiers)) {
                syncEditedComment();
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (activeAmountField != null) {
            if (Character.isDigit(codePoint)) return activeAmountField.charTyped(codePoint, modifiers);
            return true;
        }

        if (textController.isEditing()) {
            if (textController.charTyped(codePoint, modifiers)) {
                syncEditedComment();
            }
            return true;
        }
        return false;
    }

    public void syncEditedComment() {
        if (!textController.isEditing()) return;
        UUID editingNodeId = textController.getEditingNodeId();
        DiagramNode node = findNode(editingNodeId);
        if (node != null) {
            int idx = nodes.indexOf(node);
            nodes.set(idx, new DiagramNode(
                    node.id(), node.itemType(), node.x(), node.y(),
                    textController.getEditedText(), node.amount(), node.color(), node.width(), node.height()
            ));
        }
    }

    public List<DiagramStroke> getStrokes() {
        return this.strokes;
    }

    public void setStrokes(List<DiagramStroke> loadedStrokes) {
        this.strokes.clear();
        if (loadedStrokes != null) {
            this.strokes.addAll(loadedStrokes);
        }
    }
}
