package com.hoops.oretracker.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class OreTrackerClient implements ClientModInitializer {
    private static final String[] CONFIG_CLASS_NAMES = {
            "com.hoops.oretracker.client.OreTrackerConfig",
            "com.hoops.oretracker.OreTrackerConfig"
    };

    @Override
    public void onInitializeClient() {
        OreTrackerSavedSettings.load();
        reloadConfig(false);

        applySavedHudSettings();

        OreTrackerHud.register();
        OreTrackerDeathTracker.register();

        registerCommands();
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("oretracker").executes(context -> openMainScreen()));
            dispatcher.register(literal("ot").executes(context -> openMainScreen()));

            dispatcher.register(literal("track").executes(context -> openTrackingScreen()));

            dispatcher.register(literal("stoptracking").executes(context -> {
                OreTrackerDeathTracker.stop(true);
                return 1;
            }));

            dispatcher.register(literal("otclear").executes(context -> {
                clearTracker();
                sendMessage("Ore Tracker cleared.");
                return 1;
            }));

            dispatcher.register(literal("ottoggle").executes(context -> {
                toggleHud();
                return 1;
            }));

            dispatcher.register(literal("otcompact").executes(context -> {
                toggleCompactMode();
                return 1;
            }));

            dispatcher.register(literal("otmode")
                    .then(argument("mode", StringArgumentType.word())
                            .executes(context -> {
                                String rawMode = StringArgumentType.getString(context, "mode");
                                String mode = normalizeHudMode(rawMode);

                                if (mode == null) {
                                    sendMessage("Invalid HUD mode. Use: detailed, compact, or minimal.");
                                    return 0;
                                }

                                if (!setHudModeState(mode, true)) {
                                    sendMessage("Could not update HUD mode.");
                                    return 0;
                                }

                                sendMessage("HUD mode set to " + mode + ".");
                                return 1;
                            })));

            dispatcher.register(literal("otopacity")
                    .then(argument("percent", IntegerArgumentType.integer(25, 100))
                            .executes(context -> {
                                int opacity = IntegerArgumentType.getInteger(context, "percent");

                                if (!setHudOpacityState(opacity, true)) {
                                    sendMessage("Could not update HUD opacity.");
                                    return 0;
                                }

                                sendMessage("HUD opacity set to " + opacity + "%.");
                                return 1;
                            })));

            dispatcher.register(literal("otscale")
                    .then(argument("percent", IntegerArgumentType.integer(75, 150))
                            .executes(context -> {
                                int scale = IntegerArgumentType.getInteger(context, "percent");

                                if (!setHudScaleState(scale, true)) {
                                    sendMessage("Could not update HUD scale.");
                                    return 0;
                                }

                                sendMessage("HUD scale set to " + scale + "%.");
                                return 1;
                            })));

            dispatcher.register(literal("otnumbers")
                    .then(argument("mode", StringArgumentType.word())
                            .executes(context -> {
                                String rawMode = StringArgumentType.getString(context, "mode");
                                String mode = normalizeNumberMode(rawMode);

                                if (mode == null) {
                                    sendMessage("Invalid number format. Use: short, normal, or full.");
                                    return 0;
                                }

                                if (!setNumberFormatState(mode, true)) {
                                    sendMessage("Could not update number format.");
                                    return 0;
                                }

                                sendMessage("Number format set to " + mode + ".");
                                return 1;
                            })));

            dispatcher.register(literal("otmove").executes(context -> openMoveScreen()));

            dispatcher.register(literal("otreload").executes(context -> {
                reloadConfig(true);
                return 1;
            }));

            dispatcher.register(literal("otpos")
                    .then(argument("position", StringArgumentType.word())
                            .executes(context -> {
                                String rawPosition = StringArgumentType.getString(context, "position");
                                String position = normalizeHudPosition(rawPosition);

                                if (position == null) {
                                    sendMessage("Invalid HUD position. Use: top_left, top_right, bottom_left, or bottom_right.");
                                    return 0;
                                }

                                if (!setHudPositionState(position, true)) {
                                    sendMessage("Could not update HUD position.");
                                    return 0;
                                }

                                sendMessage("HUD position set to " + position + ".");
                                return 1;
                            })));

            dispatcher.register(literal("otdebug").executes(context -> {
                debugInventory();
                return 1;
            }));
        });
    }

    private static int openMainScreen() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new OreTrackerScreen()));
        return 1;
    }

    private static int openTrackingScreen() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new OreTrackerWebhookScreen()));
        return 1;
    }

    private static int openMoveScreen() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new OreTrackerMoveScreen()));
        return 1;
    }

    private static void clearTracker() {
        boolean handled = invokeStaticAny(
                OreTrackerState.class,
                new String[]{
                        "clear",
                        "clearTracking",
                        "clearTracker",
                        "clearTrackedMine",
                        "clearSelectedMine",
                        "clearSelection",
                        "stopTracking",
                        "resetTracking",
                        "reset"
                },
                new Class<?>[]{}
        );

        if (!handled) {
            invokeStaticAnyClass(
                    new String[]{
                            "com.hoops.oretracker.client.OreTrackerData",
                            "com.hoops.oretracker.OreTrackerData"
                    },
                    new String[]{
                            "clear",
                            "clearTracking",
                            "clearTracker",
                            "clearTrackedMine",
                            "resetTracking",
                            "reset"
                    },
                    new Class<?>[]{}
            );
        }
    }

    private static void toggleHud() {
        boolean next = !OreTrackerState.isHudVisible();

        if (!setHudVisibleState(next, true)) {
            sendMessage("Could not toggle HUD.");
            return;
        }

        sendMessage(OreTrackerState.isHudVisible() ? "Ore Tracker HUD shown." : "Ore Tracker HUD hidden.");
    }

    private static void toggleCompactMode() {
        String current = OreTrackerState.getHudMode().name().toLowerCase(Locale.ROOT);
        String next = current.equals("compact") ? "detailed" : "compact";

        if (!setHudModeState(next, true)) {
            sendMessage("Could not toggle compact HUD mode.");
            return;
        }

        sendMessage("HUD mode set to " + next + ".");
    }

    private static void applySavedHudSettings() {
        OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();

        if (settings.hudMode != null) {
            setHudModeState(settings.hudMode, false);
        }

        if (settings.hudPosition != null) {
            setHudPositionState(settings.hudPosition, false);
        }

        if (settings.numberFormat != null) {
            setNumberFormatState(settings.numberFormat, false);
        }

        setHudOpacityState(settings.hudOpacity, false);
        setHudScaleState(settings.hudScale, false);
        setHudVisibleState(settings.hudVisible, false);
    }

    private static boolean setHudModeState(String mode, boolean save) {
        String normalized = normalizeHudMode(mode);

        if (normalized == null) {
            return false;
        }

        boolean changed = setEnumState(
                "getHudMode",
                new String[]{"setHudMode", "setMode"},
                new String[]{"hudMode", "mode", "currentHudMode", "displayMode"},
                normalized.toUpperCase(Locale.ROOT)
        );

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.hudMode = normalized;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    private static boolean setHudPositionState(String position, boolean save) {
        String normalized = normalizeHudPosition(position);

        if (normalized == null) {
            return false;
        }

        boolean changed = setEnumState(
                "getHudPosition",
                new String[]{"setHudPosition", "setPosition"},
                new String[]{"hudPosition", "position", "currentHudPosition"},
                normalized.toUpperCase(Locale.ROOT)
        );

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.hudPosition = normalized;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    private static boolean setNumberFormatState(String mode, boolean save) {
        String normalized = normalizeNumberMode(mode);

        if (normalized == null) {
            return false;
        }

        boolean changed = setEnumState(
                "getNumberFormatMode",
                new String[]{"setNumberFormatMode", "setNumberFormat", "setNumbersMode", "setNumberMode"},
                new String[]{"numberFormatMode", "numberFormat", "numbersMode", "numberMode"},
                normalized.toUpperCase(Locale.ROOT)
        );

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.numberFormat = normalized;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    private static boolean setHudOpacityState(int opacity, boolean save) {
        int clamped = Math.max(25, Math.min(100, opacity));

        boolean changed = invokeStaticAny(
                OreTrackerState.class,
                new String[]{
                        "setHudOpacityPercent",
                        "setHudOpacity",
                        "setOpacityPercent",
                        "setOpacity"
                },
                new Class<?>[]{int.class},
                clamped
        );

        if (!changed) {
            changed = setIntField(new String[]{
                    "hudOpacityPercent",
                    "hudOpacity",
                    "opacityPercent",
                    "opacity"
            }, clamped);
        }

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.hudOpacity = clamped;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    private static boolean setHudScaleState(int scale, boolean save) {
        int clamped = Math.max(75, Math.min(150, scale));

        boolean changed = invokeStaticAny(
                OreTrackerState.class,
                new String[]{
                        "setHudScalePercent",
                        "setHudScale",
                        "setScalePercent",
                        "setScale"
                },
                new Class<?>[]{int.class},
                clamped
        );

        if (!changed) {
            changed = setIntField(new String[]{
                    "hudScalePercent",
                    "hudScale",
                    "scalePercent",
                    "scale"
            }, clamped);
        }

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.hudScale = clamped;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    private static boolean setHudVisibleState(boolean visible, boolean save) {
        boolean changed = invokeStaticAny(
                OreTrackerState.class,
                new String[]{
                        "setHudVisible",
                        "setVisible",
                        "setHudEnabled",
                        "setEnabled"
                },
                new Class<?>[]{boolean.class},
                visible
        );

        if (!changed) {
            changed = setBooleanField(new String[]{
                    "hudVisible",
                    "visible",
                    "hudEnabled",
                    "enabled"
            }, visible);
        }

        if (changed && save) {
            OreTrackerSavedSettings.Settings settings = OreTrackerSavedSettings.get();
            settings.hudVisible = visible;
            OreTrackerSavedSettings.save();
        }

        return changed;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean setEnumState(String getterName, String[] setterNames, String[] fieldNames, String enumConstantName) {
        try {
            Method getter = OreTrackerState.class.getDeclaredMethod(getterName);
            getter.setAccessible(true);

            Object currentValue = getter.invoke(null);

            if (!(currentValue instanceof Enum<?> currentEnum)) {
                return false;
            }

            Class enumClass = currentEnum.getDeclaringClass();
            Object newValue = Enum.valueOf(enumClass, enumConstantName);

            for (String setterName : setterNames) {
                if (invokeStatic(OreTrackerState.class, setterName, new Class<?>[]{enumClass}, newValue)) {
                    return true;
                }

                if (invokeStatic(OreTrackerState.class, setterName, new Class<?>[]{String.class}, enumConstantName.toLowerCase(Locale.ROOT))) {
                    return true;
                }

                if (invokeStatic(OreTrackerState.class, setterName, new Class<?>[]{String.class}, enumConstantName)) {
                    return true;
                }
            }

            if (setFieldByNames(fieldNames, enumClass, newValue)) {
                return true;
            }

            return setFirstFieldByType(enumClass, newValue);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean setIntField(String[] names, int value) {
        if (setFieldByNames(names, int.class, value)) {
            return true;
        }

        if (setFieldByNames(names, Integer.class, value)) {
            return true;
        }

        return false;
    }

    private static boolean setBooleanField(String[] names, boolean value) {
        if (setFieldByNames(names, boolean.class, value)) {
            return true;
        }

        if (setFieldByNames(names, Boolean.class, value)) {
            return true;
        }

        return false;
    }

    private static boolean setFieldByNames(String[] names, Class<?> expectedType, Object value) {
        for (String name : names) {
            try {
                Field field = OreTrackerState.class.getDeclaredField(name);

                if (!field.getType().equals(expectedType)) {
                    continue;
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                field.set(null, value);
                return true;
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean setFirstFieldByType(Class<?> expectedType, Object value) {
        try {
            for (Field field : OreTrackerState.class.getDeclaredFields()) {
                if (!field.getType().equals(expectedType)) {
                    continue;
                }

                if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                field.set(null, value);
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static void reloadConfig(boolean notify) {
        boolean handled = invokeStaticAnyClass(
                CONFIG_CLASS_NAMES,
                new String[]{
                        "reload",
                        "load",
                        "loadWorlds",
                        "loadConfig"
                },
                new Class<?>[]{}
        );

        if (notify) {
            sendMessage(handled ? "Ore Tracker config reloaded." : "Ore Tracker reload attempted.");
        }
    }

    private static void debugInventory() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        sendMessage("Ore Tracker inventory debug:");

        int shown = 0;

        for (int slot = 0; slot <= 35; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String name = stack.getHoverName().getString();
            int count = stack.getCount();

            sendMessage("Slot " + slot + ": " + count + "x " + name);
            shown++;

            if (shown >= 20) {
                sendMessage("Showing first 20 non-empty slots only.");
                break;
            }
        }

        if (shown == 0) {
            sendMessage("No items found in inventory slots 0-35.");
        }
    }

    private static String normalizeHudMode(String input) {
        String value = clean(input);

        return switch (value) {
            case "detailed", "detail", "full" -> "detailed";
            case "compact" -> "compact";
            case "minimal", "mini" -> "minimal";
            default -> null;
        };
    }

    private static String normalizeNumberMode(String input) {
        String value = clean(input);

        return switch (value) {
            case "short", "s" -> "short";
            case "normal", "n" -> "normal";
            case "full", "f" -> "full";
            default -> null;
        };
    }

    private static String normalizeHudPosition(String input) {
        String value = clean(input);

        return switch (value) {
            case "top_left", "topleft", "tl" -> "top_left";
            case "top_right", "topright", "tr" -> "top_right";
            case "bottom_left", "bottomleft", "bl" -> "bottom_left";
            case "bottom_right", "bottomright", "br" -> "bottom_right";
            default -> null;
        };
    }

    private static String clean(String input) {
        if (input == null) {
            return "";
        }

        return input.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean invokeStaticAny(Class<?> clazz, String[] methodNames, Class<?>[] parameterTypes, Object... args) {
        for (String methodName : methodNames) {
            if (invokeStatic(clazz, methodName, parameterTypes, args)) {
                return true;
            }
        }

        return false;
    }

    private static boolean invokeStaticAnyClass(String[] classNames, String[] methodNames, Class<?>[] parameterTypes, Object... args) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);

                if (invokeStaticAny(clazz, methodNames, parameterTypes, args)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean invokeStatic(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}