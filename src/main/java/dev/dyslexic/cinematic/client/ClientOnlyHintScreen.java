package dev.dyslexic.cinematic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if <1.21 {
/*import net.minecraft.network.chat.Style;
*///?}

/**
 * A short note over the server list, laid out as Minecraft's own confirm screens: what the client-only mode is for, a
 * button that turns the existing setting on (saved at once, before any connection) and one that just goes on. Either
 * way it is answered and does not come back ({@link ServerListHint}); joining a server is never held up by it.
 */
final class ClientOnlyHintScreen extends Screen {
    //? if >=1.21 {
    /** The message wraps to this many GUI pixels at most, so its lines stay short enough to read at a glance. */
    private static final int TEXT_WIDTH = 300;
    private static final int TITLE_GAP = 14;
    private static final int BUTTONS_GAP = 20;
    private static final int BUTTON_SPACING = 8;
    private static final int MESSAGE_COLOR = 0xD8D8D8;

    //?}
    private final Screen list;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private int top;

    ClientOnlyHintScreen(Screen list) {
        //? if >=1.21 {
        // from an empty root: a part appended to a coloured one would take on its colour
        super(Component.empty().append(Component.literal("Cinematic Interactions").withColor(ServerListHint.ACCENT))
                .append(Component.literal(" · ").withColor(0xA8A8A8))
        //?} else if <1.21 {
        /*super(Component.empty().append(Component.literal("Cinematic Interactions")
                        .withStyle(Style.EMPTY.withColor(ServerListHint.ACCENT)))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(0xA8A8A8)))
        *///?}
                .append(Component.translatable("cinematic.serverHint.title")));
        this.list = list;
    }

    //? if >=1.21 {
    @Override
    protected void init() {
        message = MultiLineLabel.create(font, Component.translatable("cinematic.serverHint.message"), Math.min(width - 40, TEXT_WIDTH));
    //?} else if <1.21 {
    /*@Override protected void init() {
        message = MultiLineLabel.create(font, Component.translatable("cinematic.serverHint.message"), Math.min(width - 40, 300));
    *///?}
        Component enable = Component.translatable("cinematic.serverHint.enable");
        Component carryOn = Component.translatable("cinematic.serverHint.continue");
        int buttonWidth = Math.max(150, Math.max(font.width(enable), font.width(carryOn)) + 20);
        //? if >=1.21 {
        boolean sideBySide = buttonWidth * 2 + BUTTON_SPACING <= width - 20;
        int buttonsHeight = sideBySide ? 20 : 20 * 2 + 4;
        int block = font.lineHeight + TITLE_GAP + message.getLineCount() * font.lineHeight + BUTTONS_GAP + buttonsHeight;
        //?} else if <1.21 {
        /*boolean sideBySide = buttonWidth * 2 + 8 <= width - 20;
        int block = font.lineHeight + 14 + message.getLineCount() * font.lineHeight + 20 + (sideBySide ? 20 : 44);
        *///?}
        top = Math.max(10, (height - block) / 2);
        //? if >=1.21 {
        int buttonsTop = top + font.lineHeight + TITLE_GAP + message.getLineCount() * font.lineHeight + BUTTONS_GAP;
        //?} else if <1.21 {
        /*int buttonsTop = top + font.lineHeight + 14 + message.getLineCount() * font.lineHeight + 20;
        *///?}
        Button on = Button.builder(enable, button -> {
            Settings.CLIENT_ONLY_MODE.set(true);
            ServerListHint.seen();
            minecraft.setScreen(list);
        //? if >=1.21 {
        }).width(buttonWidth).build();
        Button off = Button.builder(carryOn, button -> onClose()).width(buttonWidth).build();
        if (sideBySide) {
            on.setPosition(width / 2 - buttonWidth - BUTTON_SPACING / 2, buttonsTop);
            off.setPosition(width / 2 + BUTTON_SPACING / 2, buttonsTop);
        } else {
            on.setPosition((width - buttonWidth) / 2, buttonsTop);
            off.setPosition((width - buttonWidth) / 2, buttonsTop + 24);
        }
        //?} else if <1.21 {
        /*}).bounds(sideBySide ? width / 2 - buttonWidth - 4 : (width - buttonWidth) / 2, buttonsTop, buttonWidth, 20).build();
        Button off = Button.builder(carryOn, button -> onClose())
                .bounds(sideBySide ? width / 2 + 4 : (width - buttonWidth) / 2, buttonsTop + (sideBySide ? 0 : 24), buttonWidth, 20).build();
        *///?}
        addRenderableWidget(on);
        addRenderableWidget(off);
    }

    //? if >=1.21 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    //?} else if <1.21 {
    /*@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.render(graphics, mouseX, mouseY, tick);
    *///?}
        graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
        //? if >=1.21 {
        message.renderCentered(graphics, width / 2, top + font.lineHeight + TITLE_GAP, font.lineHeight, MESSAGE_COLOR);
        //?} else if <1.21 {
        /*message.renderCentered(graphics, width / 2, top + font.lineHeight + 14, font.lineHeight, 0xD8D8D8);
        *///?}
    }

    //? if >=1.21 {
    /** "Continue", and Esc: the mode stays as it is, the server list comes back. */
    @Override
    public void onClose() {
    //?} else if <1.21 {
    /*@Override public void onClose() {
    *///?}
        ServerListHint.seen();
        minecraft.setScreen(list);
    //? if >=1.21 {
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(title).append(". ").append(Component.translatable("cinematic.serverHint.message"));
    //?}
    }
}
