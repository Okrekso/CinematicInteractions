package example.addon;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;

/** Adapt these hooks in an actual add-on mod; the example block ID is intentionally not registered here. */
public final class ExampleAddonRegistration {
    private ExampleAddonRegistration() {}

    private static final ResourceLocation SCENE = ResourceLocation.fromNamespaceAndPath("exampleaddon", "crafting_scene");

    public static void registerCommon() {
        AddonStations.register(new AddonStations.Definition(SCENE,
                ResourceLocation.fromNamespaceAndPath("exampleaddon", "custom_crafting_table"),
                CraftingMenu.class,
                state -> List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)));
    }

    public static void registerClient() {
        AddonScenes.register(SCENE, (pos, state, player, side) -> new ExampleAddonScene(pos, side));
    }
}
