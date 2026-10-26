package dev.dyslexic.cinematic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Vanilla-style prompt shown once when the server list is first opened. */
final class ClientOnlyHintScreen extends Screen {
    private final Screen list;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private int top;

    ClientOnlyHintScreen(Screen list) {
        super(Component.empty().append(Component.literal("Cinematic Interactions")
                        .withStyle(Style.EMPTY.withColor(ServerListHint.ACCENT)))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(0xA8A8A8)))
                .append(Component.translatable("cinematic.serverHint.title")));
        this.list = list;
    }

    @Override protected void init() {
        message = MultiLineLabel.create(font, Component.translatable("cinematic.serverHint.message"), Math.min(width - 40, 300));
        Component enable = Component.translatable("cinematic.serverHint.enable");
        Component carryOn = Component.translatable("cinematic.serverHint.continue");
        int buttonWidth = Math.max(150, Math.max(font.width(enable), font.width(carryOn)) + 20);
        boolean sideBySide = buttonWidth * 2 + 8 <= width - 20;
        int block = font.lineHeight + 14 + message.getLineCount() * font.lineHeight + 20 + (sideBySide ? 20 : 44);
        top = Math.max(10, (height - block) / 2);
        int buttonsTop = top + font.lineHeight + 14 + message.getLineCount() * font.lineHeight + 20;
        Button on = Button.builder(enable, button -> {
            Settings.CLIENT_ONLY_MODE.set(true);
            ServerListHint.seen();
            minecraft.setScreen(list);
        }).bounds(sideBySide ? width / 2 - buttonWidth - 4 : (width - buttonWidth) / 2, buttonsTop, buttonWidth, 20).build();
        Button off = Button.builder(carryOn, button -> onClose())
                .bounds(sideBySide ? width / 2 + 4 : (width - buttonWidth) / 2, buttonsTop + (sideBySide ? 0 : 24), buttonWidth, 20).build();
        addRenderableWidget(on);
        addRenderableWidget(off);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.render(graphics, mouseX, mouseY, tick);
        graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
        message.renderCentered(graphics, width / 2, top + font.lineHeight + 14, font.lineHeight, 0xD8D8D8);
    }

    @Override public void onClose() {
        ServerListHint.seen();
        minecraft.setScreen(list);
    }
}
