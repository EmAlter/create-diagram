package com.emalter.creatediagram.client.diagram.canvas.text.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
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

        // 1. FAST-PATH LOCALE: Il tuo giocatore.
        // Legge la skin direttamente dall'entità client, intercettando 
        // le iniezioni di Modrinth o mod per skin offline.
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && localPlayer.getName().getString().equalsIgnoreCase(playerName)) {
            ResourceLocation localSkin = localPlayer.getSkin().texture();
            CACHE.put(playerName, localSkin);
            return localSkin;
        }

        // 2. FAST-PATH LAN/SERVER: Altri giocatori presenti nel mondo.
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            for (PlayerInfo info : conn.getOnlinePlayers()) {
                if (info.getProfile().getName().equalsIgnoreCase(playerName)) {
                    ResourceLocation lanSkin = info.getSkin().texture();
                    CACHE.put(playerName, lanSkin);
                    return lanSkin;
                }
            }
        }

        // 3. Calcola il Fallback (Steve o Alex)
        ResourceLocation defaultSkin = net.minecraft.client.resources.DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(playerName.getBytes())).texture();

        // 4. Blocco antispam API
        if (PENDING_LOOKUPS.contains(playerName)) {
            return defaultSkin;
        }
        PENDING_LOOKUPS.add(playerName);

        // 5. Ricerca asincrona su Mojang (solo per chi non è nel mondo)
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
                        try {
                            ResourceLocation fetchedSkin = Minecraft.getInstance()
                                    .getSkinManager()
                                    .getInsecureSkin(filledProfile)
                                    .texture();
                            CACHE.put(playerName, fetchedSkin);
                        } catch (Exception ex) {
                            CACHE.put(playerName, defaultSkin);
                        } finally {
                            PENDING_LOOKUPS.remove(playerName);
                        }
                    });
                } else {
                    CACHE.put(playerName, defaultSkin);
                    PENDING_LOOKUPS.remove(playerName);
                }
            } catch (Exception e) {
                CACHE.put(playerName, defaultSkin);
                PENDING_LOOKUPS.remove(playerName);
            }
        });

        return defaultSkin;
    }
}