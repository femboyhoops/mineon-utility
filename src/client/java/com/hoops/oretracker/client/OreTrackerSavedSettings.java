package com.hoops.oretracker.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OreTrackerSavedSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("oretracker")
            .resolve("client-settings.json");

    private static Settings settings = new Settings();

    private OreTrackerSavedSettings() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }

            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            Settings loaded = GSON.fromJson(json, Settings.class);

            if (loaded != null) {
                settings = loaded;
            }

            if (settings.discordWebhook == null) {
                settings.discordWebhook = "";
            }

            save();
        } catch (Exception ignored) {
            settings = new Settings();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static Settings get() {
        return settings;
    }

    public static String getDiscordWebhook() {
        return settings.discordWebhook == null ? "" : settings.discordWebhook;
    }

    public static void setDiscordWebhook(String webhook) {
        settings.discordWebhook = webhook == null ? "" : webhook.trim();
        save();
    }

    public static final class Settings {
        public String discordWebhook = "";

        // These are ready for your existing HUD/customization settings.
        // Hook your /otmode, /otopacity, /otscale, /otnumbers, /otpos commands into these later.
        public String hudMode = "detailed";
        public String hudPosition = "top_left";
        public String numberFormat = "normal";
        public int hudOpacity = 95;
        public int hudScale = 100;
        public boolean hudVisible = true;
    }
}
