package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class OreTrackerBossFarm {
    private static final long RIGHT_CLICK_INTERVAL_MS = 1000L;
    private static final long INPUT_ARM_DELAY_MS = 650L;
    private static final float CAMERA_MOVE_THRESHOLD = 0.08f;

    private static boolean registered = false;
    private static boolean enabled = false;

    private static long lastRightClickTime = 0L;
    private static long inputArmedAt = 0L;

    private static float startingYaw = 0.0f;
    private static float startingPitch = 0.0f;

    private OreTrackerBossFarm() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("bossfarm").executes(context -> {
                toggle();
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(OreTrackerBossFarm::tick);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void stop() {
        Minecraft client = Minecraft.getInstance();
        stopInternal(client, false);
    }

    private static void toggle() {
        Minecraft client = Minecraft.getInstance();

        if (enabled) {
            stopInternal(client, true);
            return;
        }

        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        enabled = true;
        lastRightClickTime = 0L;
        inputArmedAt = System.currentTimeMillis() + INPUT_ARM_DELAY_MS;

        startingYaw = client.player.getYRot();
        startingPitch = client.player.getXRot();

        client.player.displayClientMessage(Component.literal("Boss Farm enabled."), false);
    }

    private static void stopInternal(Minecraft client, boolean sendMessage) {
        boolean wasEnabled = enabled;

        enabled = false;
        lastRightClickTime = 0L;
        inputArmedAt = 0L;
        startingYaw = 0.0f;
        startingPitch = 0.0f;

        if (sendMessage && wasEnabled && client != null && client.player != null) {
            client.player.displayClientMessage(Component.literal("Boss Farm disabled."), false);
        }
    }

    private static void tick(Minecraft client) {
        if (!enabled) {
            return;
        }

        if (client == null || client.player == null || client.level == null || client.gameMode == null) {
            stopInternal(client, false);
            return;
        }

        if (shouldStopFromManualInput(client)) {
            stopInternal(client, true);
            return;
        }

        /*
         * If right-click opens a GUI/menu, close it immediately.
         */
        if (client.screen != null) {
            try {
                client.player.closeContainer();
            } catch (Exception ignored) {
            }

            client.setScreen(null);
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastRightClickTime < RIGHT_CLICK_INTERVAL_MS) {
            return;
        }

        lastRightClickTime = now;

        try {
            /*
             * RIGHT CLICK ONLY.
             *
             * Do not call client.player.swing(...).
             * That creates a visible hand swing and makes it look like left-clicking.
             */
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        } catch (Exception ignored) {
        }
    }

    private static boolean shouldStopFromManualInput(Minecraft client) {
        long now = System.currentTimeMillis();

        if (now < inputArmedAt) {
            return false;
        }

        if (client.player == null) {
            return true;
        }

        float currentYaw = client.player.getYRot();
        float currentPitch = client.player.getXRot();

        boolean cameraMoved =
                Math.abs(currentYaw - startingYaw) > CAMERA_MOVE_THRESHOLD
                        || Math.abs(currentPitch - startingPitch) > CAMERA_MOVE_THRESHOLD;

        if (cameraMoved) {
            return true;
        }

        long windowHandle = getWindowHandle(client);

        if (windowHandle == 0L) {
            return false;
        }

        return isAnyKeyboardKeyPressed(windowHandle) || isAnyMouseButtonPressed(windowHandle);
    }

    private static long getWindowHandle(Minecraft client) {
        if (client == null) {
            return 0L;
        }

        Object window;

        try {
            window = client.getWindow();
        } catch (Exception ignored) {
            return 0L;
        }

        if (window == null) {
            return 0L;
        }

        String[] methodNames = {
                "getWindow",
                "getWindowHandle",
                "getHandle"
        };

        for (String methodName : methodNames) {
            try {
                Method method = window.getClass().getMethod(methodName);
                method.setAccessible(true);

                Object result = method.invoke(window);

                if (result instanceof Number number) {
                    return number.longValue();
                }
            } catch (Exception ignored) {
            }

            try {
                Method method = window.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);

                Object result = method.invoke(window);

                if (result instanceof Number number) {
                    return number.longValue();
                }
            } catch (Exception ignored) {
            }
        }

        String[] fieldNames = {
                "window",
                "handle",
                "windowHandle"
        };

        for (String fieldName : fieldNames) {
            try {
                Field field = window.getClass().getField(fieldName);
                field.setAccessible(true);

                Object result = field.get(window);

                if (result instanceof Number number) {
                    return number.longValue();
                }
            } catch (Exception ignored) {
            }

            try {
                Field field = window.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Object result = field.get(window);

                if (result instanceof Number number) {
                    return number.longValue();
                }
            } catch (Exception ignored) {
            }
        }

        return 0L;
    }

    private static boolean isAnyKeyboardKeyPressed(long windowHandle) {
        for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
            try {
                if (GLFW.glfwGetKey(windowHandle, key) == GLFW.GLFW_PRESS) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean isAnyMouseButtonPressed(long windowHandle) {
        for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_LAST; button++) {
            try {
                if (GLFW.glfwGetMouseButton(windowHandle, button) == GLFW.GLFW_PRESS) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}