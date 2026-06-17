package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class OreTrackerChatLogBuffer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_STORED_LINES = 40;
    private static final int MAX_LINE_LENGTH = 180;
    private static final int MAX_DISCORD_FIELD_LENGTH = 1000;
    private static final long MAX_LINE_AGE_MS = 90_000L;

    private static final Deque<ChatLine> LINES = new ArrayDeque<>();
    private static boolean registered = false;

    private OreTrackerChatLogBuffer() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            add(message);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                add(message);
            }
        });
    }

    public static String snapshotForDiscord(int maxLines) {
        int limit = Math.max(1, Math.min(maxLines, 10));
        long cutoff = System.currentTimeMillis() - MAX_LINE_AGE_MS;
        List<ChatLine> snapshot = new ArrayList<>();

        synchronized (LINES) {
            for (ChatLine line : LINES) {
                if (line.createdAtMs >= cutoff) {
                    snapshot.add(line);
                }
            }
        }

        if (snapshot.isEmpty()) {
            return "";
        }

        int start = Math.max(0, snapshot.size() - limit);
        StringBuilder builder = new StringBuilder("```text\n");

        for (int i = start; i < snapshot.size(); i++) {
            ChatLine line = snapshot.get(i);
            String entry = "[" + line.timeText + "] " + line.message;

            if (builder.length() + entry.length() + 5 > MAX_DISCORD_FIELD_LENGTH) {
                break;
            }

            builder.append(entry).append('\n');
        }

        builder.append("```");

        if (builder.length() > MAX_DISCORD_FIELD_LENGTH) {
            return builder.substring(0, MAX_DISCORD_FIELD_LENGTH - 6) + "\n...```";
        }

        return builder.toString();
    }

    private static void add(Component component) {
        if (component == null) {
            return;
        }

        add(component.getString());
    }

    private static void add(String rawMessage) {
        String message = cleanChatLine(rawMessage);

        if (message.isBlank()) {
            return;
        }

        ChatLine line = new ChatLine(
                System.currentTimeMillis(),
                LocalTime.now().format(TIME_FORMAT),
                message
        );

        synchronized (LINES) {
            LINES.addLast(line);

            while (LINES.size() > MAX_STORED_LINES) {
                LINES.removeFirst();
            }
        }
    }

    private static String cleanChatLine(String input) {
        if (input == null) {
            return "";
        }

        String cleaned = input
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .replace("§", "")
                .replace("`", "'")
                .replace("@everyone", "@ everyone")
                .replace("@here", "@ here")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() > MAX_LINE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LINE_LENGTH - 3).trim() + "...";
        }

        return cleaned;
    }

    private record ChatLine(long createdAtMs, String timeText, String message) {
    }
}
