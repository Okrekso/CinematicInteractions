package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * The developer section without Cloth Config, as NeoForge's own settings screen shows it (DeveloperConfiguration there):
 * the developer mode switch and one profile selector for every built-in station, kept in the layout JSON files.
 */
final class DeveloperSettingsScreen extends OptionsSubScreen {
    DeveloperSettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("cinematic.configuration.developer"));
    }

    @Override
    protected void addOptions() {
        CycleButton<Boolean> mode = CycleButton.onOffBuilder(Settings.DEVELOPER_MODE.get())
                .withTooltip(on -> Tooltip.create(Component.translatable("cinematic.configuration.developerMode.tooltip")))
                .create(0, 0, Button.DEFAULT_WIDTH * 2 + 10, Button.DEFAULT_HEIGHT, Component.translatable("cinematic.configuration.developerMode"),
                        (button, on) -> {
                            Settings.DEVELOPER_MODE.set(on);
                            Settings.SPEC.save();
                        });
        list.addSmall(mode, null);
        for (StationKind kind : StationKind.values()) {
            // add-on stations have no layout profiles of their own; one full-width row each, the names are long
            if (kind != StationKind.ADDON) list.addSmall(selector(kind), null);
        }
    }

    private static CycleButton<Integer> selector(StationKind kind) {
        Component tooltip = Component.translatable("cinematic.settings.profiles.tooltip");
        CycleButton<Integer> selector = CycleButton.<Integer>builder(LayoutProfiles::value)
                .withValues(0, 1, 2).withInitialValue(LayoutProfiles.selected(kind))
                .create(0, 0, Button.DEFAULT_WIDTH * 2 + 10, Button.DEFAULT_HEIGHT, LayoutProfiles.name(kind), (button, profile) -> {
                    int previous = LayoutProfiles.selected(kind);
                    if (!LayoutProfiles.select(kind, profile)) {
                        button.setValue(previous);
                        button.setTooltip(Tooltip.create(SpatialEditor.text("save_error")));
                    }
                });
        selector.setTooltip(Tooltip.create(tooltip));
        return selector;
    }
}
