package com.emalter.creatediagram.client.diagram.canvas.node;

import com.emalter.creatediagram.component.DiagramNode;
import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.logic.integration.ModIntegration;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeView {
    private final Font font;
    
    private final Map<String, Boolean> machineCache = new HashMap<>();
    private final Map<String, List<String>> catalystCache = new HashMap<>();
    private final Map<String, EmiStack> stackCache = new HashMap<>();

    private boolean isMachine(String id) { return machineCache.computeIfAbsent(id, ModIntegration.get()::isMachine); }
    private List<String> getCatalysts(String id) { return catalystCache.computeIfAbsent(id, ModIntegration.get()::getValidCatalystsForMachine); }
    private EmiStack getStack(String id) { return stackCache.computeIfAbsent(id, ModIntegration.get()::getStack); }

    public NodeView(Font font) {
        this.font = font;
    }

    public void renderNode(GuiGraphics guiGraphics, DiagramNode node, boolean isInvalid) {
        if (node.itemType().equals("creatediagram:text_comment")) return;

        int w = node.width();
        int h = node.height();
        int bgColor = isInvalid ? 0x88FF0000 : 0xFF333333;

        guiGraphics.fill(node.x(), node.y(), node.x() + w, node.y() + h, bgColor);
        guiGraphics.renderOutline(node.x(), node.y(), w, h, 0xFF888888);

        ResourceLocation resId = ResourceLocation.parse(node.itemType());
        String path = resId.getPath();

        boolean hasCatalyst = !getCatalysts(node.itemType()).isEmpty();
        boolean machineFlag = isMachine(node.itemType());

        float baseW = 40f;
        float baseH = path.equals("mechanical_mixer") || path.equals("mechanical_press") ? 60f : 40f;
        float scaleX = w / baseW;
        float scaleY = h / baseH;
        float imgScale = Math.min(scaleX, scaleY) * 2.0f;

        if (path.equals("mechanical_mixer") || path.equals("mechanical_press")) {
            EmiStack basin = getStack("create:basin");
            float bScale = (w / 40f) * 2.0f;
            float bx = node.x() + (w - (16 * bScale)) / 2f;
            float by = node.y() + (h - (16 * bScale)) / 2f + (10 * (h / 60f));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(bx, by, 0);
            guiGraphics.pose().scale(bScale, bScale, 1.0f);
            basin.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }

        EmiStack emiStack = getStack(node.itemType());
        if (path.contains("crushing_wheel")) {
            float cwScale = (w / 80f) * 2.0f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(node.x() + (w / 2f) - (18 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
            guiGraphics.pose().scale(cwScale, cwScale, 1.0f);
            emiStack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(node.x() + (w / 2f) + (2 * cwScale), node.y() + (h - (16 * cwScale)) / 2f, 1);
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

        if (!machineFlag) {
            float txtScale = Math.max(1.0f, Math.min(scaleX, scaleY) * 0.8f);
            String qtyText = emiStack.getItemStack().isEmpty() ? node.amount() + "mB" : String.valueOf(node.amount());
            int textWidth = font.width(qtyText);
            int textColor = node.amount() > 1 ? 0xFFFFFF : 0x888888;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(node.x() + w - (4 * txtScale), node.y() + (4 * txtScale), 250);
            guiGraphics.pose().scale(txtScale, txtScale, 1.0f);
            guiGraphics.drawString(font, qtyText, -textWidth, 0, textColor, true);
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
                EmiStack catStack = getStack(node.property());
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(slotX + 3, slotY + 3, 10);
                guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                catStack.render(guiGraphics, 0, 0, 0f);
                guiGraphics.pose().popPose();
            } else {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 10);
                guiGraphics.drawString(font, "?", slotX + 4, slotY + 3, 0x888888, false);
                guiGraphics.pose().popPose();
            }
            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 250);
        guiGraphics.fill(node.x() + w - 6, node.y() + h - 2, node.x() + w - 2, node.y() + h, 0xFF999999);
        guiGraphics.fill(node.x() + w - 2, node.y() + h - 6, node.x() + w, node.y() + h, 0xFF999999);
        guiGraphics.pose().popPose();
    }

    public void renderCatalystMenu(GuiGraphics guiGraphics, DiagramNode node) {
        List<String> options = getCatalysts(node.itemType());
        int menuX = node.x() + node.width();
        int menuY = node.y();
        guiGraphics.fill(menuX, menuY, menuX + 20, menuY + (options.size() * 20), 0xEE222222);
        guiGraphics.renderOutline(menuX, menuY, 20, options.size() * 20, 0xFFFFAA00);
        for (int i = 0; i < options.size(); i++) {
            EmiStack stack = getStack(options.get(i));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(menuX + 2, menuY + 2 + (i * 20), 0);
            stack.render(guiGraphics, 0, 0, 0f);
            guiGraphics.pose().popPose();
        }
    }

    public void renderPorts(GuiGraphics guiGraphics, DiagramNode node, List<OutputPort> outputs) {
        boolean isMach = isMachine(node.itemType());
        int w = node.width();
        int h = node.height();

        if (isMach) {
            int portY = node.y() + (h/2) - 6;
            guiGraphics.fill(node.x() - 6, portY, node.x() + 2, portY + 12, 0xFF222222);
            guiGraphics.renderOutline(node.x() - 6, portY, 8, 12, 0xFFAAAAAA);

            int outX = node.x() + w + 2;
            int totalOutHeight = outputs.size() * 18;
            int startOutY = node.y() + (h - totalOutHeight) / 2;

            for (int i = 0; i < outputs.size(); i++) {
                int outY = startOutY + (i * 18);
                guiGraphics.fill(outX, outY, outX + 16, outY + 16, 0xFF111111);
                guiGraphics.renderOutline(outX, outY, 16, 16, 0xFF444444);

                EmiStack outStack = getStack(outputs.get(i).itemId());
                int amount = outputs.get(i).amount();

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(outX + 4, outY + 4, 0);
                guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                outStack.render(guiGraphics, 0, 0, 0f);

                if (amount > 1) {
                    String qtyStr = outStack.getItemStack().isEmpty() ? amount + "mB" : String.valueOf(amount);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(10, 10, 200);
                    guiGraphics.pose().scale(1.0f, 1.0f, 1.0f);
                    guiGraphics.drawString(font, qtyStr, 0, 0, 0xFFFFFFFF, true);
                    guiGraphics.pose().popPose();
                }
                guiGraphics.pose().popPose();
            }
        } else if (!node.itemType().equals("creatediagram:text_comment")) {
            guiGraphics.fill(node.x() + w, node.y() + (h/2) - 6, node.x() + w + 8, node.y() + (h/2) + 6, 0xFF222222);
            guiGraphics.renderOutline(node.x() + w, node.y() + (h/2) - 6, 8, 12, 0xFFAAAAAA);
        }
    }
}