package com.emalter.creatediagram.client.diagram.canvas.text.utility;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RichTextEngine {
    
    /* TODO: Riabilitare la Regex per il Rich Text in futuro
    private static final Pattern RICH_TEXT_PATTERN = Pattern.compile("(@[a-zA-Z0-9_]{1,16})|(:[a-zA-Z0-9_:]+:)");
    */

    public static void renderRichText(GuiGraphics guiGraphics, Font font, String text, int startX, int startY, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return;

        /* TODO: Riabilitare il motore di rendering per icone e tag in futuro
        int currentX = startX;
        int currentY = startY;
        int iconSize = font.lineHeight;

        Matcher matcher = RICH_TEXT_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            String prefix = text.substring(lastEnd, matcher.start());
            int[] pos = drawWrappedText(guiGraphics, font, prefix, currentX, currentY, startX, maxWidth, color);
            currentX = pos[0];
            currentY = pos[1];

            String match = matcher.group();

            if (currentX + iconSize > startX + maxWidth && currentX > startX) {
                currentX = startX;
                currentY += font.lineHeight + 2;
            }

            if (match.startsWith("@")) {
                drawPlayerHead(guiGraphics, match.substring(1), currentX, currentY, iconSize);
                currentX += iconSize + 1;
            } else if (match.startsWith(":")) {
                drawEmoji(guiGraphics, font, match.substring(1, match.length() - 1), currentX, currentY, iconSize);
                currentX += iconSize + 1;
            }

            lastEnd = matcher.end();
        }

        drawWrappedText(guiGraphics, font, text.substring(lastEnd), currentX, currentY, startX, maxWidth, color);
        return;
        */

        // Comportamento attuale: stampa solo il testo normale mandandolo a capo
        drawWrappedText(guiGraphics, font, text, startX, startY, startX, maxWidth, color);
    }

    private static int[] drawWrappedText(GuiGraphics guiGraphics, Font font, String text, int currentX, int currentY, int startX, int maxWidth, int color) {
        if (text.isEmpty()) return new int[]{currentX, currentY};
        String[] words = text.split("(?<=[ \\n])|(?=[ \\n])");

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (word.equals("\n")) {
                currentX = startX;
                currentY += font.lineHeight + 2;
                continue;
            }

            int wordWidth = font.width(word);
            if (currentX + wordWidth > startX + maxWidth && currentX > startX) {
                currentX = startX;
                currentY += font.lineHeight + 2;
                if (word.equals(" ")) continue;
            }

            guiGraphics.drawString(font, word, currentX, currentY, color, false);
            currentX += wordWidth;
        }
        return new int[]{currentX, currentY};
    }

    /* TODO: Riabilitare i metodi di rendering grafico in futuro
    private static void drawPlayerHead(GuiGraphics guiGraphics, String playerName, int x, int y, int size) { ... }
    private static void drawEmoji(GuiGraphics guiGraphics, Font font, String emojiName, int x, int y, int size) { ... }
    */
}