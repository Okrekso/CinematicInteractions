package dev.dyslexic.cinematic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.ScreenEvent;

/** A one-time server note and a persistent link to the client-only setting. */
public final class ServerListHint {
    private ServerListHint() {}

    static final int ACCENT = 0x8CC4EE;
    private static final int MUTED = 0xA8A8A8;

    static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = event.getScreen();
        if (!(screen instanceof JoinMultiplayerScreen list)) return;
        MutableComponent text = Component.empty().append(Component.literal("Cinematic").withStyle(Style.EMPTY.withColor(ACCENT)))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(MUTED)))
                .append(Component.translatable("cinematic.serverHint.state",
                        Component.translatable(Settings.clientOnlyMode() ? "cinematic.settings.on" : "cinematic.settings.off"))
                        .withStyle(Style.EMPTY.withColor(MUTED)));
        int width = mc.font.width(text);
        int x = list.width - width - 8;
        int y = 20;
        if (x < list.width / 2 + mc.font.width(list.getTitle()) / 2 + 8) {
            x = (list.width - width) / 2;
            y = 6;
        }
        PlainTextButton line = new PlainTextButton(x, y, width, mc.font.lineHeight, text,
                button -> mc.setScreen(new MultiplayerSettingsScreen(list)), mc.font);
        line.setTooltip(Tooltip.create(Component.translatable("cinematic.serverHint.state.tooltip")));
        event.addListener(line);
        if (!Settings.clientOnlyMode() && !Settings.SERVER_HINT_SEEN.get()) {
            mc.tell(() -> {
                if (mc.screen == list) mc.setScreen(new ClientOnlyHintScreen(list));
            });
        }
    }

    static void seen() {
        Settings.SERVER_HINT_SEEN.set(true);
        Settings.SPEC.save();
    }
}
