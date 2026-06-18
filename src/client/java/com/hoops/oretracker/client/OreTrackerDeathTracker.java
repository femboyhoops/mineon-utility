package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class OreTrackerDeathTracker {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:mm a");
    private static final long ATTACKER_CACHE_MS = 15000L;
    private static final long DAMAGE_WARNING_COOLDOWN_MS = 10000L;

    /*
     * Keep this false unless you are okay with guesses.
     * Nearest-player fallback is the exact thing that caused false killer reports.
     */
    private static final boolean ALLOW_NEAREST_PLAYER_FALLBACK = false;
    private static final double NEAREST_PLAYER_FALLBACK_RANGE = 6.0D;

    private static boolean tracking = false;
    private static boolean wasAlive = true;
    private static boolean registered = false;

    private static float lastHealth = -1.0f;
    private static int lastHurtTime = 0;
    private static long lastDamageWarningTime = 0L;
    private static String cachedAttacker = "Unknown";
    private static long cachedAttackerTime = 0L;

    private static String trackedPlayerName = "Unknown";
    private static String trackedDiscordUserId = "";

    private OreTrackerDeathTracker() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        OreTrackerChatLogBuffer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            handleServerDisconnect(client);
        });
    }

    public static boolean isTracking() {
        return tracking;
    }

    public static void start(String webhook) {
        start(webhook, readSavedDiscordUserId());
    }

    public static void start(String webhook, String discordUserId) {
        trackedDiscordUserId = cleanDiscordUserId(discordUserId);

        OreTrackerSavedSettings.setDiscordWebhook(webhook);
        writeSavedDiscordUserId(trackedDiscordUserId);

        Minecraft client = Minecraft.getInstance();

        trackedPlayerName = client.player == null ? "Unknown" : getRealPlayerName(client.player);

        tracking = true;
        wasAlive = true;
        lastHealth = -1.0f;
        lastHurtTime = 0;
        lastDamageWarningTime = 0L;
        cachedAttacker = "Unknown";
        cachedAttackerTime = 0L;

        OreTrackerWebhook.sendStart(webhook, now(), trackedPlayerName, trackedDiscordUserId);

        if (client.player != null) {
            lastHealth = client.player.getHealth();
            lastHurtTime = client.player.hurtTime;
            client.player.displayClientMessage(Component.literal("Ore Tracker death tracking started."), false);
        }
    }

    public static void stop(boolean sendMessage) {
        Minecraft client = Minecraft.getInstance();

        if (tracking) {
            String webhook = OreTrackerSavedSettings.getDiscordWebhook();
            String playerName = client.player == null ? trackedPlayerName : getRealPlayerName(client.player);
            OreTrackerWebhook.sendStop(webhook, now(), playerName, trackedDiscordUserId);
        }

        tracking = false;
        wasAlive = true;
        lastHealth = -1.0f;
        lastHurtTime = 0;
        lastDamageWarningTime = 0L;
        cachedAttacker = "Unknown";
        cachedAttackerTime = 0L;

        if (sendMessage && client.player != null) {
            client.player.displayClientMessage(Component.literal("Ore Tracker death tracking stopped."), false);
        }
    }

    private static void handleServerDisconnect(Minecraft client) {
        if (!tracking) {
            return;
        }

        String webhook = OreTrackerSavedSettings.getDiscordWebhook();
        String playerName = client.player == null ? trackedPlayerName : getRealPlayerName(client.player);
        String discordUserId = trackedDiscordUserId;
        String disconnectTime = now();

        tracking = false;
        wasAlive = true;
        lastHealth = -1.0f;
        lastHurtTime = 0;
        lastDamageWarningTime = 0L;
        cachedAttacker = "Unknown";
        cachedAttackerTime = 0L;

        sendDisconnectWebhook(webhook, disconnectTime, playerName, discordUserId);
    }

    private static void tick() {
        if (!tracking) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) {
            return;
        }

        boolean alive = player.isAlive() && !player.isDeadOrDying();

        if (alive) {
            cachePossibleAttacker(client, player);
        }

        if (wasAlive && !alive) {
            handleDeath(client, player);
            tracking = false;
        }

        wasAlive = alive;
    }

    private static void cachePossibleAttacker(Minecraft client, LocalPlayer player) {
        float currentHealth = player.getHealth();
        int currentHurtTime = player.hurtTime;

        if (lastHealth < 0.0f) {
            lastHealth = currentHealth;
            lastHurtTime = currentHurtTime;
            return;
        }

        boolean healthDropped = currentHealth < lastHealth;
        boolean hurtJustStarted = currentHurtTime > 0 && lastHurtTime <= 0;
        boolean tookDamage = healthDropped || hurtJustStarted;

        if (!tookDamage) {
            lastHealth = currentHealth;
            lastHurtTime = currentHurtTime;
            return;
        }

        sendDamageWarningIfReady(player, currentHealth);

        /*
         * Important:
         * Do NOT cache the nearest player here. Damage can come from fall damage,
         * fire, lava, suffocation, poison, mobs, projectiles, or server mechanics.
         * Guessing by proximity is worse than returning Unknown.
         */
        String attacker = getAttackerFromDamageSource(player);

        if (!attacker.equals("Unknown")) {
            cachedAttacker = attacker;
            cachedAttackerTime = System.currentTimeMillis();
        }

        lastHealth = currentHealth;
        lastHurtTime = currentHurtTime;
    }

    private static void sendDamageWarningIfReady(LocalPlayer player, float currentHealth) {
        long now = System.currentTimeMillis();

        if (now - lastDamageWarningTime < DAMAGE_WARNING_COOLDOWN_MS) {
            return;
        }

        lastDamageWarningTime = now;

        String webhook = OreTrackerSavedSettings.getDiscordWebhook();
        String playerName = getRealPlayerName(player);

        OreTrackerWebhook.sendDamageWarning(
                webhook,
                now(),
                playerName,
                currentHealth,
                trackedDiscordUserId
        );
    }

    private static void handleDeath(Minecraft client, LocalPlayer player) {
        String webhook = OreTrackerSavedSettings.getDiscordWebhook();
        String playerName = getRealPlayerName(player);
        String attacker = getAttackerName(client, player);
        String deathTime = now();
        String resources = ResourceSnapshot.capture(player).toDiscordField();
        String[] onlinePlayerNames = getOnlinePlayerNames(client, player);
        String selfName = cleanName(playerName);
        String discordUserId = trackedDiscordUserId;

        /*
         * Death chat can arrive a few ticks after the local death state flips.
         * Capture inventory/resources immediately, but delay the webhook briefly
         * so the visible server death line can be included in the report.
         */
        CompletableFuture.runAsync(() -> {
            String recentChat = OreTrackerChatLogBuffer.snapshotForDiscord(6);
            String finalAttacker = attacker;

            if (finalAttacker.equals("Unknown")) {
                String chatAttacker = getAttackerFromRecentChat(recentChat, onlinePlayerNames, selfName);

                if (!chatAttacker.equals("Unknown")) {
                    finalAttacker = chatAttacker + " (from recent chat)";
                }
            }

            OreTrackerWebhook.sendDeath(
                    webhook,
                    deathTime,
                    playerName,
                    finalAttacker,
                    resources,
                    discordUserId,
                    recentChat
            );
        }, CompletableFuture.delayedExecutor(1200L, TimeUnit.MILLISECONDS));

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("Ore Tracker death log queued. Tracking stopped."), false);
        }
    }

    private static String[] getOnlinePlayerNames(Minecraft client, LocalPlayer player) {
        if (client == null || client.level == null) {
            return new String[0];
        }

        String selfName = cleanName(getRealPlayerName(player));
        List<String> names = new ArrayList<>();

        for (AbstractClientPlayer other : client.level.players()) {
            if (other == null || other == player) {
                continue;
            }

            String otherName = getRealPlayerName(other);

            if (!isUsableName(otherName) || cleanName(otherName).equals(selfName)) {
                continue;
            }

            names.add(otherName);
        }

        return names.toArray(new String[0]);
    }

    private static String getAttackerFromRecentChat(String recentChat, String[] onlinePlayerNames, String selfName) {
        if (recentChat == null || recentChat.isBlank() || onlinePlayerNames == null || onlinePlayerNames.length == 0) {
            return "Unknown";
        }

        String normalizedSelfName = normalizeForNameMatch(selfName);
        String bestMatch = "Unknown";
        int bestLength = -1;

        String[] lines = recentChat.split("\\R");

        for (String line : lines) {
            String normalizedLine = " " + normalizeForNameMatch(line) + " ";

            if (!looksLikeDeathChatLine(normalizedLine)) {
                continue;
            }

            boolean mentionsSelf = !normalizedSelfName.isBlank() && normalizedLine.contains(" " + normalizedSelfName + " ");
            boolean saysYou = normalizedLine.contains(" you ");

            if (!mentionsSelf && !saysYou) {
                continue;
            }

            for (String playerName : onlinePlayerNames) {
                String normalizedPlayerName = normalizeForNameMatch(playerName);

                if (normalizedPlayerName.isBlank() || normalizedPlayerName.equals(normalizedSelfName)) {
                    continue;
                }

                boolean found = normalizedLine.contains(" " + normalizedPlayerName + " ");

                if (found && normalizedPlayerName.length() > bestLength) {
                    bestMatch = playerName;
                    bestLength = normalizedPlayerName.length();
                }
            }
        }

        return bestMatch;
    }

    private static boolean looksLikeDeathChatLine(String normalizedLine) {
        if (normalizedLine == null || normalizedLine.isBlank()) {
            return false;
        }

        String[] markers = {
                " killed ",
                " killed by ",
                " slain ",
                " slain by ",
                " shot ",
                " shot by ",
                " murdered ",
                " eliminated ",
                " finished ",
                " died ",
                " death ",
                " combat ",
                " void "
        };

        for (String marker : markers) {
            if (normalizedLine.contains(marker)) {
                return true;
            }
        }

        return false;
    }

    private static String getAttackerName(Minecraft client, LocalPlayer player) {
        String direct = getAttackerFromDamageSource(player);

        if (!direct.equals("Unknown")) {
            return direct;
        }

        String deathMessage = getAttackerFromDeathMessage(client, player);

        if (!deathMessage.equals("Unknown")) {
            return deathMessage;
        }

        if (!cachedAttacker.equals("Unknown") && System.currentTimeMillis() - cachedAttackerTime <= ATTACKER_CACHE_MS) {
            return cachedAttacker;
        }

        if (ALLOW_NEAREST_PLAYER_FALLBACK) {
            String nearest = getNearestPlayer(client, player);

            if (!nearest.equals("Unknown")) {
                return nearest;
            }
        }

        return "Unknown";
    }

    private static String getAttackerFromDamageSource(LocalPlayer player) {
        String selfName = cleanName(getRealPlayerName(player));

        Object damageSource = call(player, "getLastDamageSource");

        if (damageSource == null) {
            damageSource = call(player, "getLastHurtByMob");
        }

        if (damageSource == null) {
            damageSource = call(player, "getLastAttacker");
        }

        if (damageSource instanceof Entity entity) {
            return getEntityName(entity, selfName);
        }

        if (damageSource == null) {
            return "Unknown";
        }

        String entityName = getEntityName(call(damageSource, "getEntity"), selfName);

        if (!entityName.equals("Unknown")) {
            return entityName;
        }

        String directName = getEntityName(call(damageSource, "getDirectEntity"), selfName);

        if (!directName.equals("Unknown")) {
            return directName;
        }

        String attackerName = getEntityName(call(damageSource, "getAttacker"), selfName);

        if (!attackerName.equals("Unknown")) {
            return attackerName;
        }

        String sourceName = getEntityName(call(damageSource, "getSource"), selfName);

        if (!sourceName.equals("Unknown")) {
            return sourceName;
        }

        return "Unknown";
    }

    private static String getNearestPlayer(Minecraft client, LocalPlayer player) {
        if (client.level == null) {
            return "Unknown";
        }

        String selfName = cleanName(getRealPlayerName(player));

        AbstractClientPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (AbstractClientPlayer other : client.level.players()) {
            if (other == null || other == player) {
                continue;
            }

            String otherName = getRealPlayerName(other);

            if (cleanName(otherName).equals(selfName)) {
                continue;
            }

            double distance = other.distanceTo(player);

            if (distance < closestDistance && distance <= NEAREST_PLAYER_FALLBACK_RANGE) {
                closest = other;
                closestDistance = distance;
            }
        }

        if (closest == null) {
            return "Unknown";
        }

        return getRealPlayerName(closest);
    }

    private static String getEntityName(Object object, String selfName) {
        if (!(object instanceof Entity entity)) {
            return "Unknown";
        }

        String displayName = sanitizeVisibleName(entity.getName().getString());

        if (isUsableName(displayName) && !cleanName(displayName).equals(selfName)) {
            return displayName;
        }

        if (entity instanceof AbstractClientPlayer player) {
            String profileName = sanitizeVisibleName(player.getGameProfile().name());

            if (isUsableName(profileName) && !cleanName(profileName).equals(selfName)) {
                return profileName;
            }
        }

        String scoreboardName = sanitizeVisibleName(callString(entity, "getScoreboardName"));

        if (isUsableName(scoreboardName) && !cleanName(scoreboardName).equals(selfName)) {
            return scoreboardName;
        }

        return "Unknown";
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

    public static String getSavedDiscordUserIdForUi() {
        return trackedDiscordUserId == null ? "" : trackedDiscordUserId;
    }

    private static String readSavedDiscordUserId() {
        return trackedDiscordUserId == null ? "" : trackedDiscordUserId;
    }

    private static void writeSavedDiscordUserId(String discordUserId) {
        trackedDiscordUserId = cleanDiscordUserId(discordUserId);
    }

    private static String getRealPlayerName(AbstractClientPlayer player) {
        if (player == null) {
            return "Unknown";
        }

        try {
            String profileName = sanitizeVisibleName(player.getGameProfile().name());

            if (isUsableName(profileName)) {
                return profileName;
            }
        } catch (Exception ignored) {
        }

        String displayName = sanitizeVisibleName(player.getName().getString());

        if (isUsableName(displayName)) {
            return displayName;
        }

        return "Unknown";
    }

    private static Object call(Object object, String methodName) {
        if (object == null) {
            return null;
        }

        try {
            Method method = object.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(object);
        } catch (Exception ignored) {
        }

        try {
            Method method = object.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(object);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String callString(Object object, String methodName) {
        Object result = call(object, methodName);
        return result == null ? "" : String.valueOf(result);
    }

    private static Object callStatic(Class<?> clazz, String methodName) {
        if (clazz == null) {
            return null;
        }

        try {
            Method method = clazz.getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Exception ignored) {
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean invokeStatic(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (clazz == null) {
            return false;
        }

        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
            return true;
        } catch (Exception ignored) {
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object getField(Object object, String fieldName) {
        if (object == null || fieldName == null) {
            return null;
        }

        try {
            java.lang.reflect.Field field = object.getClass().getField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean setField(Object object, String fieldName, Object value) {
        if (object == null || fieldName == null) {
            return false;
        }

        try {
            java.lang.reflect.Field field = object.getClass().getField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
            return true;
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String getAttackerFromDeathMessage(Minecraft client, LocalPlayer player) {
        String message;
        String selfOriginal = getRealPlayerName(player);
        String selfName = cleanName(selfOriginal);

        try {
            message = player.getCombatTracker().getDeathMessage().getString();
        } catch (Exception ignored) {
            return "Unknown";
        }

        if (message == null || message.isBlank()) {
            return "Unknown";
        }

        String cleanedMessage = stripFormatting(message).trim();

        /*
         * First try to match the death message against actual online players.
         * This is much safer than pulling random text after "by" and much safer
         * than using proximity.
         */
        String onlinePlayer = getOnlinePlayerFromText(client, player, cleanedMessage);

        if (!onlinePlayer.equals("Unknown")) {
            return onlinePlayer;
        }

        String lower = cleanedMessage.toLowerCase(Locale.ROOT);

        String[] splitWords = {
                " by ",
                " whilst fighting ",
                " while fighting ",
                " from ",
                " using ",
                " to "
        };

        for (String splitter : splitWords) {
            int index = lower.lastIndexOf(splitter);

            if (index >= 0) {
                String possible = cleanedMessage.substring(index + splitter.length()).trim();
                possible = trimAttacker(possible, selfOriginal, selfName);

                if (!possible.equals("Unknown")) {
                    return possible;
                }
            }
        }

        return "Unknown";
    }

    private static String getOnlinePlayerFromText(Minecraft client, LocalPlayer player, String text) {
        if (client == null || client.level == null || text == null || text.isBlank()) {
            return "Unknown";
        }

        String normalizedText = " " + normalizeForNameMatch(text) + " ";
        String selfName = cleanName(getRealPlayerName(player));

        String bestMatch = "Unknown";
        int bestLength = -1;

        for (AbstractClientPlayer other : client.level.players()) {
            if (other == null || other == player) {
                continue;
            }

            String otherName = getRealPlayerName(other);
            String cleanedOtherName = cleanName(otherName);

            if (!isUsableName(cleanedOtherName) || cleanedOtherName.equals(selfName)) {
                continue;
            }

            String normalizedOtherName = normalizeForNameMatch(cleanedOtherName);

            if (normalizedOtherName.isBlank()) {
                continue;
            }

            boolean found = normalizedText.contains(" " + normalizedOtherName + " ");

            if (found && normalizedOtherName.length() > bestLength) {
                bestMatch = otherName;
                bestLength = normalizedOtherName.length();
            }
        }

        return bestMatch;
    }

    private static String normalizeForNameMatch(String input) {
        if (input == null) {
            return "";
        }

        return stripFormatting(input)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String trimAttacker(String input, String selfOriginal, String selfName) {
        if (input == null) {
            return "Unknown";
        }

        String value = sanitizeVisibleName(input);

        if (value.isBlank()) {
            return "Unknown";
        }

        if (value.contains(" with ")) {
            value = value.substring(0, value.indexOf(" with ")).trim();
        }

        if (value.contains(" using ")) {
            value = value.substring(0, value.indexOf(" using ")).trim();
        }

        if (value.contains("'s ")) {
            value = value.substring(0, value.indexOf("'s ")).trim();
        }

        value = sanitizeVisibleName(value);

        if (!isUsableName(value)) {
            return "Unknown";
        }

        if (value.equalsIgnoreCase(selfOriginal) || cleanName(value).equals(selfName)) {
            return "Unknown";
        }

        if (value.length() > 40) {
            value = value.substring(0, 40).trim();
        }

        return value.isBlank() ? "Unknown" : value;
    }

    private static boolean isUsableName(String input) {
        if (input == null) {
            return false;
        }

        String value = input.trim();

        if (value.isBlank()) {
            return false;
        }

        return value.matches(".*[A-Za-z0-9_].*");
    }

    private static String sanitizeVisibleName(String input) {
        if (input == null) {
            return "";
        }

        String stripped = stripFormatting(input)
                .replaceAll("[^A-Za-z0-9_ .\\-]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (!stripped.matches(".*[A-Za-z0-9_].*")) {
            return "";
        }

        return stripped;
    }

    private static String cleanName(String input) {
        return sanitizeVisibleName(input).trim().toLowerCase(Locale.ROOT);
    }

    private static String stripFormatting(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .replace("§", "")
                .replace("&", "");
    }

    private static void sendDisconnectWebhook(String webhook, String disconnectTime, String playerName, String discordUserId) {
        if (webhook == null || webhook.isBlank()) {
            return;
        }

        String mention = discordUserId == null || discordUserId.isBlank()
                ? ""
                : "<@" + discordUserId + "> ";

        String content = mention + "Ore Tracker stopped: player disconnected from the server.";

        String json = """
                {
                  "content": "%s",
                  "embeds": [
                    {
                      "title": "Ore Tracker Stopped",
                      "description": "Tracking was stopped because the player disconnected from the server.",
                      "color": 8141549,
                      "fields": [
                        {
                          "name": "Player",
                          "value": "`%s`",
                          "inline": true
                        },
                        {
                          "name": "Time",
                          "value": "`%s`",
                          "inline": true
                        },
                        {
                          "name": "Reason",
                          "value": "`Disconnected from server`",
                          "inline": false
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                escapeJson(content),
                escapeJson(playerName),
                escapeJson(disconnectTime)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
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

    private static String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private static final class ResourceSnapshot {
        private long hyperCompressed;
        private long megaCompressed;
        private long ultraCompressed;
        private long superCompressed;
        private long compressed;
        private long normal;

        private static ResourceSnapshot capture(LocalPlayer player) {
            ResourceSnapshot snapshot = new ResourceSnapshot();

            for (int slot = 0; slot <= 35; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);

                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                String name = cleanItemName(stack.getHoverName().getString());

                if (!isActualResourceStack(name)) {
                    continue;
                }

                int count = stack.getCount();

                if (hasTier(name, "hyper")) {
                    snapshot.hyperCompressed += count;
                } else if (hasTier(name, "mega")) {
                    snapshot.megaCompressed += count;
                } else if (hasTier(name, "ultra")) {
                    snapshot.ultraCompressed += count;
                } else if (hasTier(name, "super")) {
                    snapshot.superCompressed += count;
                } else if (hasTier(name, "compressed")) {
                    snapshot.compressed += count;
                } else {
                    snapshot.normal += count;
                }
            }

            return snapshot;
        }

        private String toDiscordField() {
            return """
                    `HC` %d
                    `MC` %d
                    `UC` %d
                    `SC` %d
                    `C`  %d
                    `N`  %d
                    """.formatted(
                    hyperCompressed,
                    megaCompressed,
                    ultraCompressed,
                    superCompressed,
                    compressed,
                    normal
            );
        }

        private static boolean hasTier(String name, String tier) {
            if (tier.equals("compressed")) {
                return name.contains("compressed")
                        && !name.contains("super")
                        && !name.contains("ultra")
                        && !name.contains("mega")
                        && !name.contains("hyper");
            }

            return name.contains(tier);
        }

        private static String cleanItemName(String input) {
            if (input == null) {
                return "";
            }

            return input
                    .replaceAll("(?i)§[0-9A-FK-OR]", "")
                    .replaceAll("(?i)&[0-9A-FK-OR]", "")
                    .toLowerCase(Locale.ROOT)
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        private static boolean isActualResourceStack(String name) {
            if (name.isBlank()) {
                return false;
            }

            if (!containsTrackedResourceName(name)) {
                return false;
            }

            return !containsBlockedItemType(name);
        }

        private static boolean containsBlockedItemType(String name) {
            String[] blocked = {
                    "sword",
                    "pickaxe",
                    "axe",
                    "shovel",
                    "hoe",
                    "helmet",
                    "chestplate",
                    "leggings",
                    "boots",
                    "armor",
                    "talisman",
                    "ring",
                    "necklace",
                    "wand",
                    "staff",
                    "bow",
                    "crossbow",
                    "shield",
                    "key",
                    "crate",
                    "pouch",
                    "box",
                    "voucher",
                    "token",
                    "scroll",
                    "book"
            };

            for (String word : blocked) {
                if (name.contains(word)) {
                    return true;
                }
            }

            return false;
        }

        private static boolean containsTrackedResourceName(String name) {
            String[] resources = {
                    "wood",
                    "stone",
                    "coal",
                    "iron",
                    "gold",
                    "lapis",
                    "diamond",
                    "emerald",
                    "magma",
                    "crimson",
                    "quartz",
                    "topaz",
                    "warped",
                    "celestite",
                    "soul sand",
                    "netherite",
                    "endstone",
                    "death",
                    "ethereal",
                    "nebula",
                    "amethyst",
                    "azurite",
                    "void",
                    "nullscape",
                    "glowstone",
                    "cloudium",
                    "sunstone",
                    "cerasium",
                    "soranite",
                    "starfall",
                    "crystite",
                    "aetherite",
                    "sand",
                    "prismarine",
                    "bathyal",
                    "sponge",
                    "dead coral",
                    "oxygen",
                    "thallasium",
                    "atlantium"
            };

            for (String resource : resources) {
                if (name.equals(resource)
                        || name.endsWith(" " + resource)
                        || name.contains(resource + " ore")
                        || name.contains(resource + " resource")
                        || name.contains(resource + " block")
                        || name.contains(resource + " compressed")
                        || name.contains("compressed " + resource)) {
                    return true;
                }
            }

            return false;
        }
    }
}