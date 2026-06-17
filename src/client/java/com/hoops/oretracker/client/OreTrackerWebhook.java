package com.hoops.oretracker.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class OreTrackerWebhook {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private OreTrackerWebhook() {
    }

    public static void sendStart(String webhook, String time, String playerName) {
        sendStart(webhook, time, playerName, "");
    }

    public static void sendStart(String webhook, String time, String playerName, String discordUserId) {
        sendEmbed(
                webhook,
                discordUserId,
                "Ore Tracker Started",
                "Tracking is now active for **" + safe(playerName) + "**.",
                0x2ECC71,
                field("Time", safe(time), false)
        );
    }

    public static void sendStop(String webhook, String time, String playerName) {
        sendStop(webhook, time, playerName, "");
    }

    public static void sendStop(String webhook, String time, String playerName, String discordUserId) {
        sendEmbed(
                webhook,
                discordUserId,
                "Ore Tracker Stopped",
                "Tracking has been stopped for **" + safe(playerName) + "**.",
                0x95A5A6,
                field("Time", safe(time), false)
        );
    }

    public static void sendDeath(String webhook, String time, String playerName, String attacker, String resources) {
        sendDeath(webhook, time, playerName, attacker, resources, "");
    }

    public static void sendDeath(String webhook, String time, String playerName, String attacker, String resources, String discordUserId) {
        sendDeath(webhook, time, playerName, attacker, resources, discordUserId, "");
    }

    public static void sendDeath(String webhook, String time, String playerName, String attacker, String resources, String discordUserId, String recentChat) {
        List<String> fields = new ArrayList<>();
        fields.add(field("Killed By", safe(attacker), true));
        fields.add(field("Time", safe(time), true));
        fields.add(field("Resources Carried", safe(resources), false));

        if (recentChat != null && !recentChat.isBlank()) {
            fields.add(field("Recent Chat Context", recentChat, false));
        }

        sendEmbed(
                webhook,
                discordUserId,
                "Death Logged",
                "**" + safe(playerName) + "** died while tracking.",
                0xE74C3C,
                fields.toArray(new String[0])
        );
    }

    public static void sendDamageWarning(String webhook, String time, String playerName, float health, String discordUserId) {
        String healthText = String.format(Locale.ROOT, "%.1f / 20.0", Math.max(0.0f, health));

        sendEmbed(
                webhook,
                discordUserId,
                "Mining Alignment Warning",
                "Damage was detected while tracking. Your aim may have been knocked off line, so your automine target should be checked.",
                0xA855F7,
                field("Player", safe(playerName), true),
                field("Health", healthText, true),
                field("Time", safe(time), true)
        );
    }

    private static void sendEmbed(String webhook, String discordUserId, String title, String description, int color, String... fields) {
        if (webhook == null || webhook.isBlank()) {
            return;
        }

        String cleanedDiscordUserId = cleanDiscordUserId(discordUserId);
        String content = cleanedDiscordUserId.isBlank() ? "" : "<@" + cleanedDiscordUserId + ">";
        String allowedUsers = cleanedDiscordUserId.isBlank() ? "[]" : "[\"" + cleanedDiscordUserId + "\"]";

        StringBuilder fieldJson = new StringBuilder();

        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }

            if (fieldJson.length() > 0) {
                fieldJson.append(',');
            }

            fieldJson.append(field);
        }

        String payload = """
                {
                  "username": "Ore Tracker",
                  "content": "%s",
                  "allowed_mentions": {
                    "parse": [],
                    "users": %s
                  },
                  "embeds": [
                    {
                      "title": "%s",
                      "description": "%s",
                      "color": %d,
                      "fields": [%s]
                    }
                  ]
                }
                """.formatted(
                escapeJson(content),
                allowedUsers,
                escapeJson(title),
                escapeJson(description),
                color,
                fieldJson
        );

        sendJson(webhook, payload);
    }

    private static String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escapeJson(name) + "\",\"value\":\"" + escapeJson(clampFieldValue(value == null || value.isBlank() ? "Unknown" : value)) + "\",\"inline\":" + inline + "}";
    }

    private static String clampFieldValue(String value) {
        if (value == null) {
            return "Unknown";
        }

        if (value.length() <= 1000) {
            return value;
        }

        return value.substring(0, 997).trim() + "...";
    }

    private static void sendJson(String webhook, String payload) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(webhook))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        });
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        return value;
    }

    private static String cleanDiscordUserId(String input) {
        if (input == null) {
            return "";
        }

        String cleaned = input.trim().replaceAll("[^0-9]", "");

        if (cleaned.length() < 17 || cleaned.length() > 20) {
            return "";
        }

        return cleaned;
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(input.length() + 16);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        return escaped.toString();
    }
}
