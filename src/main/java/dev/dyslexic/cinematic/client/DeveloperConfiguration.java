package dev.dyslexic.cinematic.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Adds the same profile selectors to NeoForge's screen when Cloth Config is absent. */
final class DeveloperConfiguration extends ConfigurationScreen.ConfigurationSectionScreen {
    private DeveloperConfiguration(Context context, Component title) { super(context, title); }
    static Screen create(ModContainer mod, Screen parent) {
        return new ConfigurationScreen(mod, parent, (owner, type, config, title) -> new DeveloperConfiguration(
                Context.top(config.getModId(), owner, config, (context, key, element) -> element), title));
    }
    @Override protected Element createSection(String key, UnmodifiableConfig spec, UnmodifiableConfig values) {
        if (spec.isEmpty()) return null;
        Component label = getTranslationComponent(key);
        return new Element(label, getTooltipComponent(key, null), Button.builder(Component.translatable("cinematic.settings.open_section", label),
                button -> minecraft.setScreen(sectionCache.computeIfAbsent(key, ignored -> new DeveloperConfiguration(
                        Context.section(context, this, values.entrySet(), spec.valueMap(), key), label))))
                .width(Button.DEFAULT_WIDTH).build(), false);
    }
    @Override protected Collection<? extends Element> createSyntheticValues() {
        if (!context.keylist().equals(List.of("developer"))) return List.of();
        List<Element> elements = new ArrayList<>();
        for (StationKind kind : StationKind.values()) {
            Component name = LayoutProfiles.name(kind);
            Component tooltip = Component.translatable("cinematic.settings.profiles.tooltip");
            CycleButton<Integer> selector = CycleButton.<Integer>builder(LayoutProfiles::value)
                    .withValues(0, 1, 2).withInitialValue(LayoutProfiles.selected(kind)).displayOnlyValue()
                    .create(0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, name, (button, profile) -> {
                        int previous = LayoutProfiles.selected(kind);
                        if (!LayoutProfiles.select(kind, profile)) {
                            button.setValue(previous);
                            button.setTooltip(Tooltip.create(SpatialEditor.text("save_error")));
                        }
                    });
            selector.setTooltip(Tooltip.create(tooltip));
            elements.add(new Element(name, tooltip, selector, false));
        }
        return elements;
    }
}
