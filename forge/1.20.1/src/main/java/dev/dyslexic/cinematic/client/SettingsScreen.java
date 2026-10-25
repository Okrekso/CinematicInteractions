package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The settings screen without Cloth Config. NeoForge 1.21.1 has a settings screen of its own that the mod builds on;
 * Forge 1.20.1 has none, so this is a plain one of vanilla widgets with the same sections, words and tooltips as the
 * Cloth screen: scenes, interface, camera, multiplayer and developer (with the layout profile of every station), each
 * a scrolling list of two columns as vanilla's own option lists. Every change is saved at once.
 */
public final class SettingsScreen extends Screen {
    private static final String[] SECTIONS = {"scenes", "interface", "camera", "multiplayer", "developer"};
    private static final int NOTE = 0xA8A296;

    private final Screen parent;
    private int section;
    private List<FormattedCharSequence> note = List.of();

    public SettingsScreen(Screen parent) {
        super(Component.translatable("cinematic.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int tabWidth = Math.min(96, (width - 20) / SECTIONS.length);
        int tabsLeft = width / 2 - tabWidth * SECTIONS.length / 2;
        for (int i = 0; i < SECTIONS.length; i++) {
            int index = i;
            Button tab = Button.builder(sectionTitle(i), b -> {
                section = index;
                rebuildWidgets();
            }).bounds(tabsLeft + i * tabWidth, 30, tabWidth - 2, 20).build();
            tab.active = i != section;
            addRenderableWidget(tab);
        }
        note = font.split(Component.translatable(noteKey()), Math.min(width - 40, 420));
        int top = 58 + note.size() * 10 + 4;
        Rows rows = new Rows(minecraft, width, height, top, height - 34);
        List<AbstractWidget> widgets = build();
        boolean two = width >= 340;
        for (int i = 0; i < widgets.size(); i += two ? 2 : 1) {
            rows.add(widgets.get(i), two && i + 1 < widgets.size() ? widgets.get(i + 1) : null, width, two);
        }
        addRenderableWidget(rows);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 75, height - 28, 150, 20).build());
    }

    private Component sectionTitle(int i) {
        return Component.translatable(switch (SECTIONS[i]) {
            case "multiplayer", "developer" -> "cinematic.configuration." + SECTIONS[i];
            default -> "cinematic.settings." + SECTIONS[i];
        });
    }

    private String noteKey() {
        return switch (SECTIONS[section]) {
            case "scenes" -> "cinematic.settings.scenes.note";
            case "interface" -> "cinematic.settings.interface.note";
            case "camera" -> "cinematic.settings.camera.note";
            case "multiplayer" -> "cinematic.settings.multiplayer.note";
            default -> "cinematic.settings.developer.note";
        };
    }

    /** The widgets of the open section, in order (the list places them). */
    private List<AbstractWidget> build() {
        List<AbstractWidget> rows = new ArrayList<>();
        Function<Boolean, Component> onOff = on -> value(on, "cinematic.settings.on", "cinematic.settings.off");
        Function<Boolean, Component> showHide = on -> value(on, "cinematic.settings.show", "cinematic.settings.hide");
        switch (SECTIONS[section]) {
            case "scenes" -> {
                rows.add(toggle("heightLogic", Settings.HEIGHT_LOGIC, on -> value(on, "cinematic.settings.height.window", "cinematic.settings.height.notice")));
                for (StationKind kind : StationKind.values()) {
                    rows.add(toggle(Settings.key(kind), Settings.SCENES.get(kind),
                            on -> value(on, "cinematic.settings.scene.on", kind == StationKind.CAMPFIRE ? "cinematic.settings.off" : "cinematic.settings.scene.off")));
                }
            }
            case "interface" -> {
                rows.add(toggle("controlHints", Settings.CONTROL_HINTS, showHide));
                AbstractWidget invite = toggle("campfireInvite", Settings.CAMPFIRE_INVITE, showHide);
                // there is nothing to invite to with the campfire scene off
                invite.active = Settings.scene(StationKind.CAMPFIRE);
                rows.add(invite);
            }
            case "camera" -> {
                IntSlider approach = percent("approachSpeed", Settings.APPROACH_SPEED, 50, 200);
                IntSlider window = new IntSlider("quickWindow", Settings.QUICK_WINDOW, 5, 30,
                        v -> Component.translatable("cinematic.settings.seconds", v));
                CycleButton<Integer> quick = CycleButton.<Integer>builder(v -> Component.translatable("cinematic.settings.quickMode." + v))
                        .withValues(0, 1, 2).withInitialValue(Settings.quickMode())
                        .create(0, 0, 150, 20, Component.translatable("cinematic.configuration.quickMode"), (b, v) -> {
                            Settings.set(Settings.QUICK_MODE, v);
                            approach.active = v != 2;
                            window.active = v == 1;
                        });
                quick.setTooltip(Tooltip.create(Component.translatable("cinematic.configuration.quickMode.tooltip")));
                approach.active = Settings.quickMode() != 2;
                window.active = Settings.quickMode() == 1;
                rows.add(approach);
                rows.add(quick);
                rows.add(window);
                rows.add(percent("leaveSpeed", Settings.LEAVE_SPEED, 50, 200));
                CycleButton<Integer> arms = CycleButton.<Integer>builder(v -> Component.translatable("cinematic.settings.armStyle." + v))
                        .withValues(0, 1, 2).withInitialValue(Settings.armStyle())
                        .create(0, 0, 150, 20, Component.translatable("cinematic.configuration.armStyle"),
                                (b, v) -> Settings.set(Settings.ARM_STYLE, v));
                arms.setTooltip(Tooltip.create(Component.translatable("cinematic.configuration.armStyle.tooltip")));
                rows.add(arms);
                rows.add(percent("leanStrength", Settings.LEAN_STRENGTH, 0, 100));
                rows.add(percent("sceneZoom", Settings.SCENE_ZOOM, 0, 100));
                rows.add(toggle("lookToggles", Settings.LOOK_TOGGLES,
                        on -> Component.translatable(on ? "cinematic.settings.look.toggle" : "cinematic.settings.look.hold").withStyle(ChatFormatting.WHITE)));
                rows.add(toggle("letterbox", Settings.LETTERBOX, onOff));
                rows.add(toggle("livingCamera", Settings.LIVING_CAMERA, onOff));
                rows.add(toggle("cameraShake", Settings.CAMERA_SHAKE, onOff));
                rows.add(toggle("screenFlash", Settings.SCREEN_FLASH, onOff));
            }
            case "multiplayer" -> {
                AbstractWidget clientOnly = toggle("clientOnlyMode", Settings.CLIENT_ONLY_MODE, onOff);
                if (minecraft != null && minecraft.level != null) {
                    // it takes effect on the next connection: changed from the main menu only
                    clientOnly.active = false;
                    clientOnly.setTooltip(Tooltip.create(Component.translatable("cinematic.settings.clientOnlyMode.locked")));
                }
                rows.add(clientOnly);
            }
            default -> {
                rows.add(toggle("developerMode", Settings.DEVELOPER_MODE, onOff));
                for (StationKind kind : StationKind.values()) {
                    if (!LayoutProfiles.supports(kind)) continue;
                    CycleButton<Integer> selector = CycleButton.<Integer>builder(LayoutProfiles::value)
                            .withValues(0, 1, 2).withInitialValue(LayoutProfiles.selected(kind))
                            .create(0, 0, 150, 20, LayoutProfiles.name(kind), (button, profile) -> {
                                int previous = LayoutProfiles.selected(kind);
                                if (!LayoutProfiles.select(kind, profile)) {
                                    button.setValue(previous);
                                    button.setTooltip(Tooltip.create(SpatialEditor.text("save_error")));
                                }
                            });
                    selector.setTooltip(Tooltip.create(Component.translatable("cinematic.settings.profiles.tooltip")));
                    rows.add(selector);
                }
            }
        }
        return rows;
    }

    private static CycleButton<Boolean> toggle(String key, ForgeConfigSpec.BooleanValue setting, Function<Boolean, Component> value) {
        String base = "cinematic.configuration." + key;
        CycleButton<Boolean> button = CycleButton.<Boolean>builder(value::apply).withValues(true, false).withInitialValue(setting.get())
                // Forge writes the file on every change (autosave): no second save on top, which its watcher could read half-written
                .create(0, 0, 150, 20, Component.translatable(base), (b, on) -> Settings.set(setting, on));
        button.setTooltip(Tooltip.create(Component.translatable(base + ".tooltip")));
        return button;
    }

    /** A percentage, shown with its sign; as made (100%) it is drawn plain, changed it is drawn warm. */
    private static IntSlider percent(String key, ForgeConfigSpec.IntValue setting, int min, int max) {
        int def = setting.getDefault();
        return new IntSlider(key, setting, min, max, v -> Component.literal(v + "%").withStyle(v == def ? ChatFormatting.WHITE : ChatFormatting.GOLD));
    }

    /** What a switch does in its current position: green when on, muted when off. */
    private static Component value(boolean on, String onKey, String offKey) {
        MutableComponent text = Component.translatable(on ? onKey : offKey);
        return text.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    /** The section's widgets, two to a row, scrolled like vanilla's option lists. */
    private static final class Rows extends ContainerObjectSelectionList<Rows.Row> {
        Rows(Minecraft minecraft, int width, int height, int top, int bottom) {
            super(minecraft, width, height, top, bottom, 24);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void add(AbstractWidget left, AbstractWidget right, int screenWidth, boolean two) {
            int full = two ? 150 : Math.min(310, screenWidth - 20);
            left.setWidth(full);
            left.setX(two ? screenWidth / 2 - 155 : screenWidth / 2 - full / 2);
            if (right != null) {
                right.setWidth(150);
                right.setX(screenWidth / 2 + 5);
            }
            addEntry(new Row(right == null ? List.of(left) : List.of(left, right)));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + 166;
        }

        static final class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final List<AbstractWidget> widgets;

            Row(List<AbstractWidget> widgets) {
                this.widgets = widgets;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering,
                    float partialTick) {
                for (AbstractWidget widget : widgets) {
                    widget.setY(top);
                    widget.render(graphics, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return widgets;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return widgets;
            }
        }
    }

    /** A whole-number setting on a slider, saved as it moves. */
    private static final class IntSlider extends AbstractSliderButton {
        private final ForgeConfigSpec.IntValue setting;
        private final int min, max;
        private final Component name;
        private final Function<Integer, Component> text;

        IntSlider(String key, ForgeConfigSpec.IntValue setting, int min, int max, Function<Integer, Component> text) {
            super(0, 0, 150, 20, Component.empty(), (Mth.clamp(setting.get(), min, max) - min) / (double) (max - min));
            this.setting = setting;
            this.min = min;
            this.max = max;
            this.name = Component.translatable("cinematic.configuration." + key);
            this.text = text;
            setTooltip(Tooltip.create(Component.translatable("cinematic.configuration." + key + ".tooltip")));
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty().append(name).append(": ").append(text.apply(current())));
        }

        @Override
        protected void applyValue() {
            if (setting.get() != current()) {
                Settings.set(setting, current());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int y = 56;
        for (FormattedCharSequence line : note) {
            graphics.drawCenteredString(font, line, width / 2, y, NOTE);
            y += 10;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
