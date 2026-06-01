package com.emalter.creatediagram.client.diagram.canvas.text.utility;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmojiRegistry {
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    private static boolean loaded = false;

    private static void init() {
        if (loaded) return;

        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            
            ResourceLocation rl = null;
            
            try {
                rl = ResourceLocation.parse("creatediagram:pixel_twemoji.json");
            } catch (Exception e) {
                rl = ResourceLocation.tryParse("creatediagram:pixel_twemoji.json");
            }
            
            if (rl == null) {
                System.err.println("[Create Diagram] Critical Error: ResourceLocation for pixel_twemoji.json is null!");
                loaded = true;
                return;
            }

            // A questo punto rl è sicuramente valido
            resourceManager.getResource(rl).ifPresent(resource -> {
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    json.entrySet().forEach(entry -> {
                        String rawKey = entry.getKey();
                        // Rimuove i ':' iniziali e finali dal JSON per isolare l'alias puro
                        String alias = rawKey.replaceAll("^:|:$", "");
                        String unicodeChar = entry.getValue().getAsString();

                        EMOJI_MAP.put(alias.toLowerCase(), unicodeChar);
                    });
                } catch (Exception e) {
                    System.err.println("[Create Diagram] Error parsing emoji pixel_twemoji.json: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("[Create Diagram] Initializing error of ResourceManager: " + e.getMessage());
        }

        loaded = true;
    }

    public static String getUnicodeChar(String alias) {
        init();
        return EMOJI_MAP.get(alias.toLowerCase());
    }

    public static List<String> getSuggestions(String query) {
        init();
        String lowerQuery = query.toLowerCase();
        return EMOJI_MAP.keySet().stream()
                .filter(k -> k.startsWith(lowerQuery))
                .map(k -> ":" + k + ":")
                .limit(20)
                .toList();
    }
}