package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class OreTrackerDeathTracker {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:mm a");

    private static boolean tracking = false;
    private static boolean wasAlive = true;
    private static boolean registered = false;

    private static float lastHealth = -1.0f;
    private static String cachedAttacker = "Unknown";
    private static long cachedAttackerTime = 0L;

    private OreTrackerDeathTracker() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static boolean isTracking() {
        return tracking;
    }

    public static void start(String webhook) {
        OreTrackerSavedSettings.setDiscordWebhook(webhook);

        tracking = true;
        wasAlive = true;
        lastHealth = -1.0f;
        cachedAttacker = "Unknown";
        cachedAttackerTime = 0L;

        OreTrackerWebhook.sendStart(webhook, now());

        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            lastHealth = client.player.getHealth();
            client.player.displayClientMessage(Component.literal("Ore Tracker death tracking started."), false);
        }
    }

    public static void stop(boolean sendMessage) {
        tracking = false;
        wasAlive = true;
        lastHealth = -1.0f;
        cachedAttacker = "Unknown";
        cachedAttackerTime = 0L;

        Minecraft client = Minecraft.getInstance();

        if (sendMessage && client.player != null) {
            client.player.displayClientMessage(Component.literal("Ore Tracker death tracking stopped."), false);
        }
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

        if (lastHealth < 0.0f) {
            lastHealth = currentHealth;
            return;
        }

        boolean tookDamage = currentHealth < lastHealth || player.hurtTime > 0;

        if (!tookDamage) {
            lastHealth = currentHealth;
            return;
        }

        String attacker = getAttackerFromDamageSource(player);

        if (attacker.equals("Unknown")) {
            attacker = getNearestPlayer(client, player);
        }

        if (!attacker.equals("Unknown")) {
            cachedAttacker = attacker;
            cachedAttackerTime = System.currentTimeMillis();
        }

        lastHealth = currentHealth;
    }

    private static void handleDeath(Minecraft client, LocalPlayer player) {
        String webhook = OreTrackerSavedSettings.getDiscordWebhook();
        String attacker = getAttackerName(client, player);
        ResourceSnapshot resources = ResourceSnapshot.capture(player);

        OreTrackerWebhook.sendDeath(
                webhook,
                now(),
                attacker,
                resources.toDiscordField()
        );

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("Ore Tracker death log sent. Tracking stopped."), false);
        }
    }

    private static String getAttackerName(Minecraft client, LocalPlayer player) {
        String direct = getAttackerFromDamageSource(player);

        if (!direct.equals("Unknown")) {
            return direct;
        }

        String deathMessage = getAttackerFromDeathMessage(player);

        if (!deathMessage.equals("Unknown")) {
            return deathMessage;
        }

        if (!cachedAttacker.equals("Unknown") && System.currentTimeMillis() - cachedAttackerTime <= 15000L) {
            return cachedAttacker;
        }

        String nearest = getNearestPlayer(client, player);

        if (!nearest.equals("Unknown")) {
            return nearest;
        }

        return "Unknown";
    }

    private static String getAttackerFromDamageSource(LocalPlayer player) {
        String selfName = cleanName(player.getName().getString());

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

        String selfName = cleanName(player.getName().getString());

        AbstractClientPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (AbstractClientPlayer other : client.level.players()) {
            if (other == null || other == player) {
                continue;
            }

            String otherName = other.getName().getString();

            if (cleanName(otherName).equals(selfName)) {
                continue;
            }

            double distance = other.distanceTo(player);

            if (distance < closestDistance && distance <= 16.0D) {
                closest = other;
                closestDistance = distance;
            }
        }

        if (closest == null) {
            return "Unknown";
        }

        return closest.getName().getString();
    }

    private static String getEntityName(Object object, String selfName) {
        if (!(object instanceof Entity entity)) {
            return "Unknown";
        }

        String name = entity.getName().getString();

        if (name == null || name.isBlank()) {
            return "Unknown";
        }

        if (cleanName(name).equals(selfName)) {
            return "Unknown";
        }

        return name;
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

    private static String getAttackerFromDeathMessage(LocalPlayer player) {
        String message;
        String selfOriginal = player.getName().getString();
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

    private static String trimAttacker(String input, String selfOriginal, String selfName) {
        if (input == null) {
            return "Unknown";
        }

        String value = input
                .replace(".", "")
                .replace("!", "")
                .trim();

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

        if (value.equalsIgnoreCase(selfOriginal) || cleanName(value).equals(selfName)) {
            return "Unknown";
        }

        if (value.length() > 40) {
            value = value.substring(0, 40).trim();
        }

        return value.isBlank() ? "Unknown" : value;
    }

    private static String cleanName(String input) {
        return stripFormatting(input).trim().toLowerCase(Locale.ROOT);
    }

    private static String stripFormatting(String input) {
        if (input == null) {
            return "";
        }

        return input.replaceAll("§.", "");
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
                    .replaceAll("§.", "")
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