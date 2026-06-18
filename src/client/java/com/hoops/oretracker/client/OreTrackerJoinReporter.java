package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class OreTrackerJoinReporter {
    /*
     * Paste your webhook between the quotes.
     *
     * It must look like:
     * https://discord.com/api/webhooks/WEBHOOK_ID/WEBHOOK_TOKEN
     *
     * Do not include angle brackets, markdown, spaces, or extra quotes.
     */
    private static final String JOIN_REPORT_WEBHOOK = "https://discord.com/api/webhooks/1517271481003016314/ebJCrMZrVM_irZwFz7V7nFEjgdb2r6y99RyTFbPqIMugHxjY53OMEyKPCImrC8nFWUAC";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:mm a");

    private static final long REPORT_DELAY_MS = 2500L;
    private static final long REPORT_TIMEOUT_MS = 15000L;

    private static final boolean DEBUG_MESSAGES = false;

    private static boolean registered = false;
    private static boolean pendingReport = false;
    private static boolean sentThisConnection = false;

    private static long joinedAtMs = 0L;

    private OreTrackerJoinReporter() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            pendingReport = true;
            sentThisConnection = false;
            joinedAtMs = System.currentTimeMillis();

            debug("Join reporter queued.");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pendingReport = false;
            sentThisConnection = false;
            joinedAtMs = 0L;
        });

        ClientTickEvents.END_CLIENT_TICK.register(OreTrackerJoinReporter::tick);
    }

    private static void tick(Minecraft client) {
        if (!pendingReport || sentThisConnection) {
            return;
        }

        long now = System.currentTimeMillis();

        if (joinedAtMs <= 0L) {
            joinedAtMs = now;
        }

        if (now - joinedAtMs < REPORT_DELAY_MS) {
            return;
        }

        if (now - joinedAtMs > REPORT_TIMEOUT_MS) {
            pendingReport = false;
            debug("Join reporter timed out before player loaded.");
            return;
        }

        if (client == null || client.player == null || client.level == null) {
            return;
        }

        String playerName = getPlayerName(client.player);
        String time = LocalDateTime.now().format(TIME_FORMAT);

        pendingReport = false;
        sentThisConnection = true;

        sendJoinWebhook(playerName, time);
    }

    private static void sendJoinWebhook(String playerName, String time) {
        String webhook = normalizeWebhook(JOIN_REPORT_WEBHOOK);

        if (!isDiscordWebhookUrl(webhook)) {
            debug("Join reporter skipped: invalid webhook. Length="
                    + webhook.length()
                    + ", startsWithDiscordWebhook="
                    + isDiscordWebhookUrl(webhook));
            return;
        }

        debug("Join reporter sending. Webhook length=" + webhook.length());

        String json = """
                {
                  "content": "Ore Tracker join report: `%s` joined a server using the mod.",
                  "embeds": [
                    {
                      "title": "Ore Tracker Join Report",
                      "description": "A player joined a server with Ore Tracker installed.",
                      "color": 8141549,
                      "fields": [
                        {
                          "name": "IGN",
                          "value": "`%s`",
                          "inline": true
                        },
                        {
                          "name": "Time",
                          "value": "`%s`",
                          "inline": true
                        }
                      ]
                    }
                  ],
                  "allowed_mentions": {
                    "parse": []
                  }
                }
                """.formatted(
                escapeJson(playerName),
                escapeJson(playerName),
                escapeJson(time)
        );

        Thread thread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhook))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    debug("Join report sent. HTTP " + status);
                } else {
                    debug("Join report failed. HTTP " + status + " Body: " + response.body());
                }
            } catch (Exception exception) {
                debug("Join report error: "
                        + exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage());
            }
        }, "OreTracker-JoinReporter");

        thread.setDaemon(true);
        thread.start();
    }

    private static String normalizeWebhook(String input) {
        if (input == null) {
            return "";
        }

        return input.trim();
    }

    private static boolean isDiscordWebhookUrl(String webhook) {
        if (webhook == null || webhook.isBlank()) {
            return false;
        }

        return webhook.startsWith("https://discord.com/api/webhooks/")
                || webhook.startsWith("https://discordapp.com/api/webhooks/");
    }

    private static String getPlayerName(LocalPlayer player) {
        if (player == null) {
            return "Unknown";
        }

        try {
            String profileName = sanitize(player.getGameProfile().name());

            if (!profileName.isBlank()) {
                return profileName;
            }
        } catch (Exception ignored) {
        }

        try {
            String displayName = sanitize(player.getName().getString());

            if (!displayName.isBlank()) {
                return displayName;
            }
        } catch (Exception ignored) {
        }

        return "Unknown";
    }

    private static String sanitize(String input) {
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

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static void debug(String message) {
        System.out.println("[Ore Tracker Join Reporter] " + message);

        if (!DEBUG_MESSAGES) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[Ore Tracker] " + message), false);
        }
    }
}