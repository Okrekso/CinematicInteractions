package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The mod's settings: a search box over four tabs (general, stations, camera, advanced), every option a row that names
 * what it does, with its value on the right and a reset arrow once it is changed. What the option under the mouse
 * does is spelled out in a box under the list. Changes are kept here until Done (or Esc); Cancel forgets them.
 */
public final class SettingsScreen extends Screen {
    private static final int ROW_WIDTH = 310, CONTROL = 120, NOTE = 0xFFA8A296, HEADER = 0xFFE0C080;

    private enum Tab { GENERAL, STATIONS, CAMERA, ADVANCED }

    /** The stations as they are grouped on their tab, with the item that stands for each. */
    private static final Object[][] GROUPS = {
            {"crafting", StationKind.TABLE, StationKind.CRAFTER, StationKind.STONECUTTER, StationKind.LOOM, StationKind.CARTOGRAPHY},
            {"cooking", StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE, StationKind.CAMPFIRE},
            {"smithing", StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.SMITHING},
            {"magic", StationKind.ENCHANTING, StationKind.BREWING, StationKind.BEACON},
            {"other", StationKind.LECTERN, StationKind.JUKEBOX, StationKind.CHISELED_BOOKSHELF}};

    /** Pace, view and effects set at once; any other mix reads as custom. */
    private enum Preset {
        CINEMATIC(80, 80, 100, 100, true, true, true, true),
        BALANCED(100, 100, 100, 100, true, true, true, true),
        FAST(160, 160, 70, 60, false, true, true, true),
        MINIMAL(200, 200, 0, 0, false, false, false, false),
        CUSTOM(0, 0, 0, 0, false, false, false, false);

        final int approach, leave, lean, zoom;
        final boolean letterbox, living, shake, flash;

        Preset(int approach, int leave, int lean, int zoom, boolean letterbox, boolean living, boolean shake, boolean flash) {
            this.approach = approach;
            this.leave = leave;
            this.lean = lean;
            this.zoom = zoom;
            this.letterbox = letterbox;
            this.living = living;
            this.shake = shake;
            this.flash = flash;
        }

        Component title() {
            return Component.translatable("cinematic.settings.preset." + name().toLowerCase(Locale.ROOT));
        }
    }

    private final Screen parent;
    /** Changed values not yet written to the settings file. */
    private final Map<ClientConfig.Value<?>, Object> draft = new HashMap<>();
    private final Map<StationKind, Integer> profiles = new EnumMap<>(StationKind.class);
    private final boolean inWorld;
    private Tab tab = Tab.GENERAL;
    /** The search as typed, and as matched (trimmed, lower case). */
    private String searchText = "", query = "";
    private EditBox search;
    private Rows rows;
    private final List<Button> tabButtons = new ArrayList<>();
    /** The list is built again on the next tick (after a change that moves other controls). */
    private boolean dirty;
    @Nullable
    private Component described;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("cinematic.settings.title"));
        this.parent = parent;
        this.inWorld = Minecraft.getInstance().level != null;
    }

    // ---- the draft ---------------------------------------------------------------------------------

    private boolean bool(ClientConfig.BooleanValue value) {
        return (Boolean) draft.getOrDefault(value, value.get());
    }

    private int number(ClientConfig.IntValue value) {
        return (Integer) draft.getOrDefault(value, value.get());
    }

    private void put(ClientConfig.Value<?> value, Object to) {
        draft.put(value, to);
    }

    private int profile(StationKind kind) {
        return profiles.getOrDefault(kind, LayoutProfiles.selected(kind));
    }

    @SuppressWarnings("unchecked")
    private void apply() {
        for (Map.Entry<ClientConfig.Value<?>, Object> change : draft.entrySet()) {
            ClientConfig.Value<Object> value = (ClientConfig.Value<Object>) change.getKey();
            if (!value.get().equals(change.getValue())) {
                value.set(change.getValue());
            }
        }
        Settings.SPEC.save();
        profiles.forEach(LayoutProfiles::select);
    }

    private Preset preset() {
        for (Preset preset : Preset.values()) {
            if (preset != Preset.CUSTOM && number(Settings.APPROACH_SPEED) == preset.approach && number(Settings.LEAVE_SPEED) == preset.leave
                    && number(Settings.LEAN_STRENGTH) == preset.lean && number(Settings.SCENE_ZOOM) == preset.zoom
                    && bool(Settings.LETTERBOX) == preset.letterbox && bool(Settings.LIVING_CAMERA) == preset.living
                    && bool(Settings.CAMERA_SHAKE) == preset.shake && bool(Settings.SCREEN_FLASH) == preset.flash) {
                return preset;
            }
        }
        return Preset.CUSTOM;
    }

    private void choose(Preset preset) {
        put(Settings.APPROACH_SPEED, preset.approach);
        put(Settings.LEAVE_SPEED, preset.leave);
        put(Settings.LEAN_STRENGTH, preset.lean);
        put(Settings.SCENE_ZOOM, preset.zoom);
        put(Settings.LETTERBOX, preset.letterbox);
        put(Settings.LIVING_CAMERA, preset.living);
        put(Settings.CAMERA_SHAKE, preset.shake);
        put(Settings.SCREEN_FLASH, preset.flash);
    }

    // ---- the options -------------------------------------------------------------------------------

    /** One row: its name and what it does (searched alike), its control, and how to put it back as made. */
    private abstract class Option {
        final Tab on;
        final Component label;
        final Component description;
        BooleanSupplier enabled = () -> true;
        /** Shown only with developer mode on (the layout profiles). */
        boolean developer;

        Option(Tab on, Component label, Component description) {
            this.on = on;
            this.label = label;
            this.description = description;
        }

        abstract AbstractWidget control();

        abstract boolean changed();

        abstract void reset();

        boolean matches(String q) {
            return (label.getString() + " " + description.getString()).toLowerCase(Locale.ROOT).contains(q);
        }
    }

    private Option toggle(Tab on, String key, ClientConfig.BooleanValue value, Function<Boolean, Component> text) {
        return toggle(on, Component.translatable("cinematic.configuration." + key), Component.translatable("cinematic.configuration." + key + ".tooltip"),
                value, text);
    }

    private Option toggle(Tab on, Component label, Component description, ClientConfig.BooleanValue value, Function<Boolean, Component> text) {
        return new Option(on, label, description) {
            @Override
            AbstractWidget control() {
                return CycleButton.<Boolean>builder(text::apply, bool(value)).withValues(true, false).displayOnlyValue()
                        .create(0, 0, CONTROL, 20, label, (button, to) -> {
                            put(value, to);
                            // (the layout profiles show only with developer mode on)
                            dirty = value == Settings.DEVELOPER_MODE;
                        });
            }

            @Override
            boolean changed() {
                return bool(value) != value.getDefault();
            }

            @Override
            void reset() {
                put(value, value.getDefault());
            }
        };
    }

    private Option slider(Tab on, String key, ClientConfig.IntValue value, int min, int max, Function<Integer, Component> text) {
        return new Option(on, Component.translatable("cinematic.configuration." + key), Component.translatable("cinematic.configuration." + key + ".tooltip")) {
            @Override
            AbstractWidget control() {
                return new IntSlider(value, min, max, text);
            }

            @Override
            boolean changed() {
                return number(value) != value.getDefault();
            }

            @Override
            void reset() {
                put(value, value.getDefault());
            }
        };
    }

    private Option presetOption() {
        return new Option(Tab.CAMERA, Component.translatable("cinematic.settings.preset"), Component.translatable("cinematic.settings.preset.tooltip")) {
            @Override
            AbstractWidget control() {
                return new Button.Plain(0, 0, CONTROL, 20, Component.empty(), button -> {
                    Preset now = preset();
                    choose(Preset.values()[now == Preset.CUSTOM ? Preset.BALANCED.ordinal() : (now.ordinal() + 1) % Preset.CUSTOM.ordinal()]);
                    dirty = true;
                }, narration -> narration.get()) {
                    @Override
                    public Component getMessage() {
                        Preset now = preset();
                        return now.title().copy().withStyle(now == Preset.CUSTOM ? ChatFormatting.GOLD : ChatFormatting.WHITE);
                    }
                };
            }

            @Override
            boolean changed() {
                return preset() != Preset.BALANCED;
            }

            @Override
            void reset() {
                choose(Preset.BALANCED);
            }
        };
    }

    private Option profileOption(StationKind kind) {
        Option option = new Option(Tab.ADVANCED, LayoutProfiles.name(kind), Component.translatable("cinematic.settings.profiles.tooltip")) {
            @Override
            AbstractWidget control() {
                return CycleButton.<Integer>builder(LayoutProfiles::value, profile(kind)).withValues(0, 1, 2).displayOnlyValue()
                        .create(0, 0, CONTROL, 20, label, (button, to) -> profiles.put(kind, to));
            }

            @Override
            boolean changed() {
                return profile(kind) != 0;
            }

            @Override
            void reset() {
                profiles.put(kind, 0);
            }
        };
        option.developer = true;
        return option;
    }

    /** Every option in the order it is shown, each tab's under its headers (a header is a null option and its title). */
    private List<Object> options() {
        Function<Boolean, Component> onOff = on -> value(on, "cinematic.settings.on", "cinematic.settings.off");
        Function<Boolean, Component> showHide = on -> value(on, "cinematic.settings.show", "cinematic.settings.hide");
        List<Object> all = new ArrayList<>();
        // general
        all.add(toggle(Tab.GENERAL, "heightLogic", Settings.HEIGHT_LOGIC,
                on -> on ? Component.translatable("cinematic.settings.reach.window").withStyle(ChatFormatting.WHITE)
                        : Component.translatable("cinematic.settings.reach.notice").withStyle(ChatFormatting.WHITE)));
        all.add(toggle(Tab.GENERAL, "controlHints", Settings.CONTROL_HINTS, showHide));
        Option invite = toggle(Tab.GENERAL, "campfireInvite", Settings.CAMPFIRE_INVITE, showHide);
        // there is nothing to invite to with the campfire scene off
        invite.enabled = () -> bool(Settings.SCENES.get(StationKind.CAMPFIRE));
        all.add(invite);
        all.add(toggle(Tab.GENERAL, "lookToggles", Settings.LOOK_TOGGLES,
                on -> Component.translatable(on ? "cinematic.settings.look.toggle" : "cinematic.settings.look.hold").withStyle(ChatFormatting.WHITE)));
        // stations
        for (Object[] group : GROUPS) {
            all.add(new Header(Tab.STATIONS, Component.translatable("cinematic.settings.group." + group[0])));
            for (int i = 1; i < group.length; i++) {
                StationKind kind = (StationKind) group[i];
                all.add(new StationOption(kind));
            }
        }
        // camera
        all.add(presetOption());
        all.add(toggle(Tab.CAMERA, "reduceMotion", Settings.REDUCE_MOTION, onOff));
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.pace")));
        all.add(slider(Tab.CAMERA, "approachSpeed", Settings.APPROACH_SPEED, 50, 200, SettingsScreen::percent));
        all.add(slider(Tab.CAMERA, "leaveSpeed", Settings.LEAVE_SPEED, 50, 200, SettingsScreen::percent));
        all.add(toggle(Tab.CAMERA, "quickReturn", Settings.QUICK_RETURN, onOff));
        Option window = slider(Tab.CAMERA, "quickWindow", Settings.QUICK_WINDOW, 5, 30, v -> Component.translatable("cinematic.settings.seconds", v));
        window.enabled = () -> bool(Settings.QUICK_RETURN);
        all.add(window);
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.view")));
        all.add(slider(Tab.CAMERA, "leanStrength", Settings.LEAN_STRENGTH, 0, 100, SettingsScreen::percent));
        all.add(slider(Tab.CAMERA, "sceneZoom", Settings.SCENE_ZOOM, 0, 100, SettingsScreen::percent));
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.effects")));
        for (String key : new String[]{"letterbox", "livingCamera", "cameraShake", "screenFlash"}) {
            ClientConfig.BooleanValue value = switch (key) {
                case "letterbox" -> Settings.LETTERBOX;
                case "livingCamera" -> Settings.LIVING_CAMERA;
                case "cameraShake" -> Settings.CAMERA_SHAKE;
                default -> Settings.SCREEN_FLASH;
            };
            Option effect = toggle(Tab.CAMERA, key, value, onOff);
            // reduce motion has them all off, whatever they are set to
            effect.enabled = () -> !bool(Settings.REDUCE_MOTION);
            all.add(effect);
        }
        // advanced
        Option clientOnly = toggle(Tab.ADVANCED, Component.translatable("cinematic.configuration.clientOnlyMode"),
                inWorld ? Component.translatable("cinematic.settings.clientOnlyMode.locked")
                        : Component.translatable("cinematic.configuration.clientOnlyMode.tooltip"),
                Settings.CLIENT_ONLY_MODE, onOff);
        // it takes effect on the next connection: changed from the main menu only
        clientOnly.enabled = () -> !inWorld;
        all.add(clientOnly);
        all.add(toggle(Tab.ADVANCED, "developerMode", Settings.DEVELOPER_MODE, onOff));
        all.add(new Header(Tab.ADVANCED, Component.translatable("cinematic.settings.profiles"), true));
        for (StationKind kind : StationKind.values()) {
            all.add(profileOption(kind));
        }
        return all;
    }

    /** A station's scene on or off, shown as its block with its name in a grid of two. */
    private final class StationOption extends Option {
        final StationKind kind;

        StationOption(StationKind kind) {
            super(Tab.STATIONS, LayoutProfiles.name(kind), Component.translatable("cinematic.configuration." + Settings.key(kind) + ".tooltip"));
            this.kind = kind;
        }

        @Override
        AbstractWidget control() {
            return new StationButton(this);
        }

        @Override
        boolean changed() {
            return !bool(Settings.SCENES.get(kind));
        }

        @Override
        void reset() {
            put(Settings.SCENES.get(kind), true);
        }
    }

    /** A title over the options after it (shown only with developer mode on, for the profiles). */
    private record Header(Tab on, Component title, boolean developer) {
        Header(Tab on, Component title) {
            this(on, title, false);
        }
    }

    // ---- layout ------------------------------------------------------------------------------------

    @Override
    protected void init() {
        tabButtons.clear();
        search = new EditBox(font, width / 2 - 100, 20, 200, 18, Component.translatable("cinematic.settings.search"));
        search.setHint(Component.translatable("cinematic.settings.search").withStyle(ChatFormatting.DARK_GRAY));
        // (set before the responder: the list is not there yet)
        search.setValue(searchText);
        search.setResponder(text -> {
            searchText = text;
            query = text.trim().toLowerCase(Locale.ROOT);
            fill(true);
        });
        addRenderableWidget(search);
        int tabWidth = Math.min(90, (width - 20) / Tab.values().length);
        int tabsLeft = width / 2 - tabWidth * Tab.values().length / 2;
        for (Tab t : Tab.values()) {
            Button button = Button.builder(tabTitle(t), b -> {
                tab = t;
                search.setValue("");
                fill(true);
            }).bounds(tabsLeft + t.ordinal() * tabWidth, 42, tabWidth - 2, 20).build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }
        rows = new Rows(minecraft, width, Math.max(40, height - 66 - 94), 66);
        addRenderableWidget(rows);
        int footer = height - 26;
        addRenderableWidget(Button.builder(Component.translatable("cinematic.settings.reset_all"), b -> {
            for (Object o : options()) {
                // (not what is locked or out of sight: client-only mode in a world, the profiles without developer mode)
                if (o instanceof Option option && option.enabled.getAsBoolean() && (!option.developer || bool(Settings.DEVELOPER_MODE))) {
                    option.reset();
                }
            }
            dirty = true;
        }).bounds(width / 2 - ROW_WIDTH / 2, footer, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            draft.clear();
            profiles.clear();
            minecraft.setScreen(parent);
        }).bounds(width / 2 + ROW_WIDTH / 2 - 204, footer, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 + ROW_WIDTH / 2 - 100, footer, 100, 20).build());
        fill(false);
    }

    /** The rows of the open tab, or of every tab that has a match for the search. */
    private void fill(boolean keepScroll) {
        double scroll = keepScroll ? rows.scrollAmount() : 0;
        boolean searching = !query.isEmpty();
        for (Button button : tabButtons) {
            button.active = searching || tabButtons.indexOf(button) != tab.ordinal();
        }
        List<Row> list = new ArrayList<>();
        Tab heading = null;
        List<AbstractWidget> pair = new ArrayList<>();
        boolean developer = bool(Settings.DEVELOPER_MODE);
        int found = 0;
        if (!searching && tab == Tab.STATIONS) {
            list.add(new Row(Row.Style.BAR, Component.translatable("cinematic.settings.stations.note"), null,
                    Button.builder(Component.translatable("cinematic.settings.all_on"), b -> stations(true)).size(78, 20).build(),
                    Button.builder(Component.translatable("cinematic.settings.all_off"), b -> stations(false)).size(78, 20).build()));
        }
        for (Object o : options()) {
            if (o instanceof Header header) {
                if (!searching && header.on() == tab && (developer || !header.developer())) {
                    flush(pair, list);
                    list.add(new Row(Row.Style.HEADER, header.title(), null, null, null));
                }
                continue;
            }
            Option option = (Option) o;
            if (searching ? !option.matches(query) : option.on != tab || option.developer && !developer) continue;
            found++;
            if (searching && heading != option.on) {
                flush(pair, list);
                heading = option.on;
                list.add(new Row(Row.Style.HEADER, tabTitle(option.on), null, null, null));
            }
            AbstractWidget control = option.control();
            if (control instanceof StationButton) {
                pair.add(control);
                if (pair.size() == 2) flush(pair, list);
                continue;
            }
            flush(pair, list);
            Button reset = Button.builder(Component.literal("↺"), b -> {
                option.reset();
                dirty = true;
            }).size(20, 20).build();
            reset.setTooltip(Tooltip.create(Component.translatable("cinematic.settings.reset")));
            list.add(new Row(Row.Style.OPTION, option.label, option, control, reset));
        }
        flush(pair, list);
        if (!searching && tab == Tab.ADVANCED && !developer) {
            list.add(new Row(Row.Style.NOTE, Component.translatable("cinematic.settings.profiles.hidden"), null, null, null));
        }
        if (searching) {
            list.addFirst(new Row(Row.Style.NOTE, found == 0 ? Component.translatable("cinematic.settings.no_results", searchText.trim())
                    : Component.translatable("cinematic.settings.results", found), null, null, null));
        }
        rows.set(list);
        rows.setScrollAmount(scroll);
    }

    private void flush(List<AbstractWidget> pair, List<Row> list) {
        if (!pair.isEmpty()) {
            list.add(new Row(Row.Style.PAIR, null, null, pair.get(0), pair.size() > 1 ? pair.get(1) : null));
            pair.clear();
        }
    }

    private void stations(boolean on) {
        for (StationKind kind : Settings.SCENES.keySet()) {
            put(Settings.SCENES.get(kind), on);
        }
        dirty = true;
    }

    private static Component tabTitle(Tab t) {
        return Component.translatable("cinematic.settings.tab." + t.name().toLowerCase(Locale.ROOT));
    }

    /** Developer mode on in the draft, so the layout profiles show (for the automated walkthrough). */
    void showDeveloper() {
        put(Settings.DEVELOPER_MODE, true);
        fill(true);
    }

    @Override
    public void tick() {
        if (dirty) {
            dirty = false;
            fill(true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        described = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 7, 0xFFFFFFFF);
        // what the option under the mouse does
        int top = height - 88, box = 56, left = width / 2 - ROW_WIDTH / 2;
        graphics.fill(left, top, left + ROW_WIDTH, top + box, 0x90000000);
        graphics.renderOutline(left, top, ROW_WIDTH, box, 0x40FFFFFF);
        Component text = described != null ? described : Component.translatable("cinematic.settings.describe");
        int y = top + 5;
        for (FormattedCharSequence line : font.split(text, ROW_WIDTH - 12)) {
            if (y > top + box - 10) break;
            graphics.drawString(font, line, left + 6, y, described != null ? 0xFFE0E0E0 : NOTE, false);
            y += 10;
        }
    }

    @Override
    public void onClose() {
        apply();
        minecraft.setScreen(parent);
    }

    /** What a switch does in its current position: green when on, muted when off. */
    private static Component value(boolean on, String onKey, String offKey) {
        return Component.translatable(on ? onKey : offKey).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component percent(int v) {
        return Component.literal(v + "%");
    }

    private static ItemLike icon(StationKind kind) {
        return switch (kind) {
            case TABLE -> Items.CRAFTING_TABLE;
            case FURNACE -> Items.FURNACE;
            case SMOKER -> Items.SMOKER;
            case BLAST_FURNACE -> Items.BLAST_FURNACE;
            case ENCHANTING -> Items.ENCHANTING_TABLE;
            case ANVIL -> Items.ANVIL;
            case GRINDSTONE -> Items.GRINDSTONE;
            case SMITHING -> Items.SMITHING_TABLE;
            case STONECUTTER -> Items.STONECUTTER;
            case LOOM -> Items.LOOM;
            case CARTOGRAPHY -> Items.CARTOGRAPHY_TABLE;
            case BREWING -> Items.BREWING_STAND;
            case CRAFTER -> Items.CRAFTER;
            case CAMPFIRE -> Items.CAMPFIRE;
            case LECTERN -> Items.LECTERN;
            case BEACON -> Items.BEACON;
            case JUKEBOX -> Items.JUKEBOX;
            case CHISELED_BOOKSHELF -> Items.CHISELED_BOOKSHELF;
        };
    }

    // ---- widgets -----------------------------------------------------------------------------------

    /** A station's block, its name and On or Off; a click switches its scene. */
    private final class StationButton extends Button.Plain {
        final StationOption option;
        private final ItemStack stack;

        StationButton(StationOption option) {
            super(0, 0, 150, 20, Component.empty(), button -> {
                ClientConfig.BooleanValue value = Settings.SCENES.get(option.kind);
                put(value, !bool(value));
            }, narration -> narration.get());
            this.option = option;
            this.stack = new ItemStack(icon(option.kind));
        }

        @Override
        public Component getMessage() {
            boolean on = bool(Settings.SCENES.get(option.kind));
            return Component.empty().append(option.label).append(" · ").append(value(on, "cinematic.settings.on", "cinematic.settings.off"));
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderDefaultSprite(graphics);
            graphics.renderItem(stack, getX() + 3, getY() + 2);
            graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE)
                    .acceptScrollingWithDefaultCenter(getMessage(), getX() + 22, getX() + getWidth() - 3, getY(), getY() + getHeight());
        }
    }

    /** A whole number on a slider, kept in the draft as it moves. */
    private final class IntSlider extends AbstractSliderButton {
        private final ClientConfig.IntValue setting;
        private final int min, max;
        private final Function<Integer, Component> text;

        IntSlider(ClientConfig.IntValue setting, int min, int max, Function<Integer, Component> text) {
            super(0, 0, CONTROL, 20, Component.empty(), (Mth.clamp(number(setting), min, max) - min) / (double) (max - min));
            this.setting = setting;
            this.min = min;
            this.max = max;
            this.text = text;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            int v = current();
            setMessage(text.apply(v).copy().withStyle(v == setting.getDefault() ? ChatFormatting.WHITE : ChatFormatting.GOLD));
        }

        @Override
        protected void applyValue() {
            put(setting, current());
        }
    }

    /** The scrolling list of rows, as wide as the row and with its scroll bar just beside it. */
    private static final class Rows extends ContainerObjectSelectionList<Row> {
        Rows(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 24);
        }

        void set(List<Row> list) {
            replaceEntries(list);
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        protected int scrollBarX() {
            return width / 2 + ROW_WIDTH / 2 + 6;
        }
    }

    /** One row of the list. */
    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        enum Style {
            /** A title over the rows after it. */
            HEADER,
            /** A line of explanation. */
            NOTE,
            /** An option: its name, its control and its reset arrow. */
            OPTION,
            /** Two stations side by side. */
            PAIR,
            /** A line of explanation with two buttons after it (the stations' all on / all off). */
            BAR
        }

        private final Style style;
        @Nullable
        private final Component label;
        @Nullable
        private final Option option;
        @Nullable
        private final AbstractWidget first, second;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        Row(Style style, @Nullable Component label, @Nullable Option option, @Nullable AbstractWidget first, @Nullable AbstractWidget second) {
            this.style = style;
            this.label = label;
            this.option = option;
            this.first = first;
            this.second = second;
            if (first != null) widgets.add(first);
            if (second != null) widgets.add(second);
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = getContentX(), top = getContentY(), width = getContentWidth();
            switch (style) {
                case HEADER -> graphics.drawString(font, label, left + 4, top + 10, HEADER, false);
                case NOTE -> graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - 8), left + 4, top + 8, NOTE, false);
                case OPTION -> {
                    boolean enabled = option.enabled.getAsBoolean();
                    graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - CONTROL - 32), left + 4, top + 6,
                            enabled ? 0xFFFFFFFF : 0xFF808080, false);
                    // (another option may switch this one on or off: the campfire its invitation, reduce motion the effects)
                    first.active = enabled;
                    first.setPosition(left + width - CONTROL - 24, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    second.setPosition(left + width - 20, top);
                    second.visible = enabled && option.changed();
                    second.render(graphics, mouseX, mouseY, partialTick);
                    if (hovering) described = option.description;
                }
                case BAR -> {
                    graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - 170), left + 4, top + 6, NOTE, false);
                    first.setPosition(left + width - 160, top);
                    second.setPosition(left + width - 78, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    second.render(graphics, mouseX, mouseY, partialTick);
                }
                case PAIR -> {
                    first.setPosition(left, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    if (first.isHovered()) described = ((StationButton) first).option.description;
                    if (second != null) {
                        second.setPosition(left + width - second.getWidth(), top);
                        second.render(graphics, mouseX, mouseY, partialTick);
                        if (second.isHovered()) described = ((StationButton) second).option.description;
                    }
                }
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
