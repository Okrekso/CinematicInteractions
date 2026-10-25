package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * Manual opt-in check of the server list's note on the client-only mode ({@link ServerListHint}), on a real title screen:
 * -Dcinematic.serverHintSmoke=true, and with -Dcinematic.serverHintSmoke.server=host:port a join after it.
 * Screenshots go to the game folder's screenshots as serverhint_*.png; the log ends in [ServerHintSmoke] PASS or FAIL.
 */
public final class ServerHintSmoke {
    private ServerHintSmoke() {}

    private static final boolean ENABLED = Boolean.getBoolean("cinematic.serverHintSmoke");
    private static final String SERVER = System.getProperty("cinematic.serverHintSmoke.server", "");
    private static final int[] SCALES = {2, 4};

    private record Step(int delay, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean started;
    private static int index;
    private static int wait;
    private static int passed;
    private static int failed;
    private static int joinWait;

    public static void onScreen(Screen screen) {
        if (!ENABLED || started || !(screen instanceof TitleScreen)) return;
        started = true;
        script();
    }

    public static void tick() {
        if (!ENABLED || !started || index >= STEPS.size()) return;
        // the title screen is up before the resources have loaded: their overlay would cover every shot
        if (Minecraft.getInstance().getOverlay() != null) return;
        if (++wait < STEPS.get(index).delay()) return;
        wait = 0;
        Step step = STEPS.get(index++);
        try {
            step.action().run();
        } catch (RuntimeException e) {
            fail("step " + index + " threw " + e);
        }
    }

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void script() {
        Minecraft mc = Minecraft.getInstance();
        // 1. the first time with the mode off: the note opens over the list
        step(10, () -> {
            set(false, false);
            openList();
        });
        step(15, () -> {
            check("1 first open with the mode off shows the note", mc.screen instanceof ClientOnlyHintScreen, screenName());
            shot("1_note");
        });
        for (int scale : SCALES) {
            step(2, () -> {
                mc.options.guiScale().set(scale);
                mc.resizeDisplay();
            });
            step(10, () -> {
                check("1 note at GUI scale " + scale + " keeps its buttons on screen", buttonsInside(mc.screen), screenName());
                shot("1_note_scale" + scale);
            });
        }
        step(2, () -> {
            mc.options.guiScale().set(0);
            mc.resizeDisplay();
        });
        // 2. its first button turns the existing setting on, saved before any connection
        step(10, () -> press(mc.screen, "cinematic.serverHint.enable"));
        step(10, () -> {
            check("2 the button turns the client-only mode on", Settings.clientOnlyMode(), "mode=" + Settings.clientOnlyMode());
            check("2 the setting is saved in the settings file", savedMode() == Boolean.TRUE, "file mode=" + savedMode());
            check("2 back at the server list", mc.screen instanceof JoinMultiplayerScreen, screenName());
            check("2 the state line says on", stateLine(mc.screen).contains(on()), stateLine(mc.screen));
            shot("2_list_enabled");
        });
        // 3. the list opened again: no note (the mode is on, and the note was answered)
        step(4, ServerHintSmoke::openList);
        step(15, () -> {
            check("3 reopened list shows no note", mc.screen instanceof JoinMultiplayerScreen, screenName());
            shot("3_reopened");
        });
        // 4. the mode already on, the note never answered: still no note
        step(4, () -> {
            set(true, false);
            openList();
        });
        step(15, () -> {
            check("4 with the mode already on there is no note", mc.screen instanceof JoinMultiplayerScreen, screenName());
            check("4 the state line says on", stateLine(mc.screen).contains(on()), stateLine(mc.screen));
            shot("4_mode_on");
        });
        // 5. "Continue": the mode stays off, the list comes back and the note is not shown again
        step(4, () -> {
            set(false, false);
            openList();
        });
        step(15, () -> press(mc.screen, "cinematic.serverHint.continue"));
        step(10, () -> {
            check("5 continue leaves the mode off", !Settings.clientOnlyMode() && savedMode() == Boolean.FALSE, "mode=" + Settings.clientOnlyMode());
            check("5 continue returns to the list", mc.screen instanceof JoinMultiplayerScreen, screenName());
            check("5 the state line says off", stateLine(mc.screen).contains(off()), stateLine(mc.screen));
            shot("5_after_continue");
        });
        step(4, ServerHintSmoke::openList);
        step(15, () -> check("5 answered once: no note on the next visit", mc.screen instanceof JoinMultiplayerScreen, screenName()));
        // 6. the state line opens the setting; the line follows the change
        step(4, () -> pressLine(mc.screen));
        step(10, () -> {
            check("6 the state line opens the multiplayer setting", mc.screen instanceof MultiplayerSettingsScreen, screenName());
            shot("6_setting");
            press(mc.screen, "cinematic.configuration.clientOnlyMode");
        });
        step(6, () -> {
            check("6 the setting turns the mode on", Settings.clientOnlyMode(), "mode=" + Settings.clientOnlyMode());
            press(mc.screen, "gui.done");
        });
        step(10, () -> {
            check("6 back at the list, the line follows the setting", mc.screen instanceof JoinMultiplayerScreen
                    && stateLine(mc.screen).contains(on()), screenName() + " " + stateLine(mc.screen));
            shot("6_line_updated");
        });
        // 7. the mod's settings screen still opens as before
        step(4, () -> mc.setScreen(CinematicClient.settingsScreen(new TitleScreen())));
        step(10, () -> {
            check("7 the settings screen opens", mc.screen != null && !(mc.screen instanceof TitleScreen), screenName());
            shot("7_settings");
            mc.setScreen(new TitleScreen());
        });
        // 8. a normal join from the list, with the mode the note set
        if (!SERVER.isEmpty()) {
            step(10, ServerHintSmoke::openList);
            step(10, () -> ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(SERVER),
                    new ServerData("Server hint smoke", SERVER, ServerData.Type.OTHER), false, null));
            step(1, ServerHintSmoke::awaitJoin);
        } else {
            step(4, ServerHintSmoke::finish);
        }
    }

    /** Waits for the join (up to 30 s), then checks it and leaves the server back to the list. */
    private static void awaitJoin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (++joinWait > 600) {
                fail("8 the join did not finish: " + screenName());
                finish();
                return;
            }
            index--;
            return;
        }
        check("8 joined the server from the list", true, "level=" + mc.level.dimension().location());
        check("8 the connection took the mode set before it", ClientOnly.modeForConnection(), "mode=" + ClientOnly.modeForConnection());
        step(40, () -> {
            shot("8_joined");
            mc.level.disconnect();
            mc.disconnect(new JoinMultiplayerScreen(new TitleScreen()));
        });
        step(20, () -> {
            check("8 back at the list after leaving, no note", mc.screen instanceof JoinMultiplayerScreen, screenName());
            finish();
        });
    }

    private static void finish() {
        Cinematic.LOGGER.info("[ServerHintSmoke] {}: {} checks passed, {} failed", failed == 0 ? "PASS" : "FAIL", passed, failed);
        Minecraft.getInstance().stop();
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private static void set(boolean mode, boolean seen) {
        Settings.CLIENT_ONLY_MODE.set(mode);
        Settings.SERVER_HINT_SEEN.set(seen);
        Settings.SPEC.save();
    }

    private static void openList() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    /** The mode as the settings file holds it (null when unreadable). */
    private static Boolean savedMode() {
        try {
            var path = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("cinematic-client.json");
            var json = com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            return json.has("multiplayer.clientOnlyMode") ? json.get("multiplayer.clientOnlyMode").getAsBoolean() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<AbstractWidget> widgets(Screen screen) {
        List<AbstractWidget> result = new ArrayList<>();
        if (screen != null) for (var child : screen.children()) if (child instanceof AbstractWidget widget) result.add(widget);
        return result;
    }

    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        for (AbstractWidget widget : widgets(screen)) {
            if (widget instanceof AbstractButton button && button.getMessage().getString().startsWith(label)) {
                button.onPress();
                return;
            }
        }
        fail("no button \"" + label + "\" on " + screenName());
    }

    private static void pressLine(Screen screen) {
        for (AbstractWidget widget : widgets(screen)) {
            if (widget instanceof PlainTextButton line && line.getMessage().getString().startsWith("Cinematic")) {
                line.onPress();
                return;
            }
        }
        fail("no state line on " + screenName());
    }

    private static String stateLine(Screen screen) {
        for (AbstractWidget widget : widgets(screen)) {
            if (widget instanceof PlainTextButton line && line.getMessage().getString().startsWith("Cinematic")) {
                boolean inside = line.getX() >= 0 && line.getRight() <= screen.width && line.getY() >= 0;
                return line.getMessage().getString() + (inside ? "" : " (off screen)");
            }
        }
        return "(no line)";
    }

    private static boolean buttonsInside(Screen screen) {
        List<AbstractWidget> list = widgets(screen);
        if (list.size() < 2) return false;
        for (AbstractWidget widget : list) {
            if (widget.getX() < 0 || widget.getRight() > screen.width || widget.getY() < 0 || widget.getBottom() > screen.height) return false;
        }
        return true;
    }

    private static String on() {
        return Component.translatable("cinematic.settings.on").getString();
    }

    private static String off() {
        return Component.translatable("cinematic.settings.off").getString();
    }

    private static String screenName() {
        Screen screen = Minecraft.getInstance().screen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "serverhint_" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void check(String name, boolean ok, String details) {
        if (ok) passed++;
        else failed++;
        Cinematic.LOGGER.info("[ServerHintSmoke] check {} {}: {}", ok ? "passed" : "FAILED", name, details);
    }

    private static void fail(String reason) {
        failed++;
        Cinematic.LOGGER.error("[ServerHintSmoke] FAILED {}", reason);
    }
}
