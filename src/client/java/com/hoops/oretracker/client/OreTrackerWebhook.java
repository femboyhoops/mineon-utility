package com.hoops.oretracker.client;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OreTrackerWebhook {
    private static final Gson GSON = new Gson();

    private OreTrackerWebhook() {
    }

    public static void sendStart(String webhookUrl, String time, String trackedPlayer) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Ore Tracker Started");
        embed.put("description", "Starting tracking.");
        embed.put("color", 0x8B5CF6);

        embed.put("fields", List.of(
                field("Player", "`" + safe(trackedPlayer) + "`", true),
                field("Started At", "`" + safe(time) + "`", true),
                field("Status", "`Tracking Active`", true)
        ));

        sendEmbed(webhookUrl, embed);
    }

    public static void sendStop(String webhookUrl, String time, String trackedPlayer) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Ore Tracker Stopped");
        embed.put("description", "Tracking was stopped manually.");
        embed.put("color", 0xF59E0B);

        embed.put("fields", List.of(
                field("Player", "`" + safe(trackedPlayer) + "`", true),
                field("Stopped At", "`" + safe(time) + "`", true),
                field("Status", "`Tracking Inactive`", true)
        ));

        sendEmbed(webhookUrl, embed);
    }

    public static void sendDeath(String webhookUrl, String time, String trackedPlayer, String attacker, String resourcesSummary) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Ore Tracker Death Log");
        embed.put("description", "Tracking has stopped automatically.");
        embed.put("color", 0xEF4444);

        embed.put("fields", List.of(
                field("Player", "`" + safe(trackedPlayer) + "`", true),
                field("Time of Death", "`" + safe(time) + "`", true),
                field("Killed By", "`" + safe(attacker) + "`", true),
                field("Resources", resourcesSummary == null || resourcesSummary.isBlank() ? "`None detected`" : resourcesSummary, false)
        ));

        sendEmbed(webhookUrl, embed);
    }

    public static void sendPlain(String webhookUrl, String content) {
        if (!isValidWebhook(webhookUrl)) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", "Ore Tracker");
        payload.put("content", content);

        sendPayload(webhookUrl.trim(), payload);
    }

    private static void sendEmbed(String webhookUrl, Map<String, Object> embed) {
        if (!isValidWebhook(webhookUrl)) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", "Ore Tracker");
        payload.put("embeds", List.of(embed));

        sendPayload(webhookUrl.trim(), payload);
    }

    private static Map<String, Object> field(String name, String value, boolean inline) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        return value
                .replace("`", "'")
                .replace("@", "@\u200B")
                .trim();
    }

    private static boolean isValidWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }

        String trimmed = webhookUrl.trim();

        return trimmed.startsWith("https://discord.com/api/webhooks/")
                || trimmed.startsWith("https://discordapp.com/api/webhooks/");
    }

    private static void sendPayload(String webhookUrl, Map<String, Object> payload) {
        Thread thread = new Thread(() -> sendBlocking(webhookUrl, payload), "OreTracker-DiscordWebhook");
        thread.setDaemon(true);
        thread.start();
    }

    private static void sendBlocking(String webhookUrl, Map<String, Object> payload) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "OreTracker");
            connection.setDoOutput(true);

            byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            connection.getResponseCode();
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
