package com.emalter.creatediagram.client.diagram.canvas.text.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkinCache {
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_LOOKUPS = Collections.synchronizedSet(new HashSet<>());

    private PlayerSkinCache() {}

    public static ResourceLocation getSkinTexture(String playerName) {
        if (CACHE.containsKey(playerName)) {
            return CACHE.get(playerName);
        }

        // 1. Fallback immediato (Steve/Alex)
        ResourceLocation defaultSkin = net.minecraft.client.resources.DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(playerName.getBytes())).texture();

        CompletableFuture.runAsync(() -> {
            try {
                java.net.URL url = java.net.URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName).toURL();

                java.io.InputStreamReader reader = new java.io.InputStreamReader(url.openStream());
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                String uuidStr = json.get("id").getAsString();

                String formattedUuid = uuidStr.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                );
                UUID uuid = UUID.fromString(formattedUuid);
                
                com.mojang.authlib.yggdrasil.ProfileResult result =
                        Minecraft.getInstance().getMinecraftSessionService().fetchProfile(uuid, true);

                if (result != null) {
                    GameProfile filledProfile = result.profile();
                    Minecraft.getInstance().execute(() -> {
                        ResourceLocation fetchedSkin = Minecraft.getInstance()
                                .getSkinManager()
                                .getInsecureSkin(filledProfile)
                                .texture();
                        CACHE.put(playerName, fetchedSkin);
                        PENDING_LOOKUPS.remove(playerName);
                    });
                } else {
                    PENDING_LOOKUPS.remove(playerName);
                }
            } catch (Exception e) {
                PENDING_LOOKUPS.remove(playerName);
            }
        });

        return CACHE.get(playerName);
    }
}