package com.emalter.creatediagram.client.tooltip;

import com.emalter.creatediagram.component.OutputPort;
import com.emalter.creatediagram.logic.RecipeType;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TooltipManager {

    private TooltipManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // --- 1. TOOLTIP
    public static List<Component> getBaseTooltip(EmiStack stack) {
        if (stack == null || stack.isEmpty()) return List.of(Component.literal("Unknown Item"));
        return new ArrayList<>(stack.getTooltipText());
    }

    // --- 2. TOOLTIP OUTPUT
    public static List<Component> getOutputTooltip(EmiStack stack, OutputPort out) {
        List<Component> tooltip = getBaseTooltip(stack);

        String cleanProcessName = out.processName().replaceAll("(?i)\\bbulk\\b[\\s_]*", "");
        if (!cleanProcessName.isEmpty()) {
            cleanProcessName = cleanProcessName.substring(0, 1).toUpperCase() + cleanProcessName.substring(1);
        }

        Component processPrefix = Component.literal("Process: ").withStyle(ChatFormatting.GRAY);
        tooltip.add(processPrefix.copy().append(getChromaText(cleanProcessName)));

        float rawChance = out.chance();
        if (rawChance > 1.0f) rawChance = rawChance / 100.0f;
        int chancePercent = Math.round(rawChance * 100f);
        if (chancePercent < 0) chancePercent = 0;
        if (chancePercent > 100) chancePercent = 100;

        tooltip.add(Component.literal(chancePercent + "%").withStyle(chancePercent == 100 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        
        if (out.recipeType() == RecipeType.INFINITE) {
            tooltip.add(Component.literal("∞ Infinite").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }

        return tooltip;
    }

    // --- 3. TOOLTIP CATEGORIES ---
    public static List<Component> getCategoryTooltip(String categoryName, String namespace) {
        List<Component> tooltip = new ArrayList<>();
        
        String modName = net.neoforged.fml.ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace.substring(0, 1).toUpperCase() + namespace.substring(1));

        tooltip.add(Component.literal(modName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Click to filter").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return tooltip;
    }

    // --- UTILITY CHROMA ---
    public static MutableComponent getChromaText(String text) {
        long time = System.currentTimeMillis();
        MutableComponent chromaComp = Component.empty();

        for (int j = 0; j < text.length(); j++) {
            float offset = j * 0.05f;
            float hue = ((time % 2000L) / 2000.0f) - offset;
            int letterColor = Mth.hsvToRgb(Math.abs(hue % 1.0f), 1.0F, 1.0F);
            chromaComp.append(Component.literal(String.valueOf(text.charAt(j))).withStyle(s -> s.withColor(letterColor)));
        }
        return chromaComp;
    }

    // --- UTILITY RENDERING ---
    public static void renderTooltip(GuiGraphics gui, Font font, List<Component> tooltip, int mouseX, int mouseY) {
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 400);
        gui.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        gui.pose().popPose();
    }
}