package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class OreTrackerDiscordPrompt {
    private static final String DISCORD_INVITE = "https://discord.gg/4xKrR44vAC";

    private static final Path CONFIG_PATH = Path.of("config", "oretracker-discord-prompt.properties");

    private static boolean registered = false;
    private static boolean pendingShow = false;

    private OreTrackerDiscordPrompt() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!isProbablyMultiplayerServer(client)) {
                return;
            }

            pendingShow = true;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pendingShow = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!pendingShow) {
                return;
            }

            if (client == null || client.player == null || client.level == null) {
                return;
            }

            if (client.screen != null) {
                return;
            }

            String playerName = getPlayerName(client.player);

            if (playerName.isBlank()) {
                return;
            }

            if (hasPlayerSeenPrompt(playerName)) {
                pendingShow = false;
                return;
            }

            markPlayerSeenPrompt(playerName);

            pendingShow = false;
            client.setScreen(new OreTrackerDiscordPromptScreen(DISCORD_INVITE));
        });
    }

    private static boolean hasPlayerSeenPrompt(String playerName) {
        Properties properties = loadProperties();
        String key = getPlayerKey(playerName);

        return Boolean.parseBoolean(properties.getProperty(key, "false"));
    }

    private static void markPlayerSeenPrompt(String playerName) {
        Properties properties = loadProperties();
        String key = getPlayerKey(playerName);

        properties.setProperty(key, "true");
        saveProperties(properties);
    }

    private static String getPlayerKey(String playerName) {
        return "shown." + sanitizeKey(playerName);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        if (!Files.exists(CONFIG_PATH)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (Exception ignored) {
        }

        return properties;
    }

    private static void saveProperties(Properties properties) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "Ore Tracker Discord Prompt Settings");
            }
        } catch (Exception ignored) {
        }
    }

    private static String getPlayerName(LocalPlayer player) {
        if (player == null) {
            return "";
        }

        try {
            String profileName = sanitizeVisibleName(player.getGameProfile().name());

            if (!profileName.isBlank()) {
                return profileName;
            }
        } catch (Exception ignored) {
        }

        try {
            String displayName = sanitizeVisibleName(player.getName().getString());

            if (!displayName.isBlank()) {
                return displayName;
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private static String sanitizeVisibleName(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .replaceAll("[^A-Za-z0-9_ .\\-]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sanitizeKey(String input) {
        if (input == null) {
            return "unknown";
        }

        String sanitized = input
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-.]", "_")
                .replaceAll("_+", "_")
                .trim();

        if (sanitized.isBlank()) {
            return "unknown";
        }

        return sanitized;
    }

    private static boolean isProbablyMultiplayerServer(Minecraft client) {
        try {
            Method method = Minecraft.class.getDeclaredMethod("getCurrentServer");
            method.setAccessible(true);
            return method.invoke(client) != null;
        } catch (Exception ignored) {
        }

        try {
            Method method = Minecraft.class.getDeclaredMethod("isSingleplayer");
            method.setAccessible(true);

            Object result = method.invoke(client);

            if (result instanceof Boolean singleplayer) {
                return !singleplayer;
            }
        } catch (Exception ignored) {
        }

        return true;
    }
}