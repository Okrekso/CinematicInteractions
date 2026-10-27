package dev.dyslexic.cinematic.client.guide;

import dev.dyslexic.cinematic.Cinematic;
import javax.annotation.Nullable;
//? if neoforge {
import net.neoforged.fml.ModList;
//?} else if fabric {
/*import net.fabricmc.loader.api.FabricLoader;
*///?} else if forge {
/*import net.minecraftforge.fml.ModList;
*///?}

/** The installed recipe viewer, if any. Only JEI is supported; without it the field guide simply does not appear. */
public final class RecipeViewers {
    private RecipeViewers() {}

    private static boolean failed;

    /** The viewer once its runtime is ready (after the world has loaded its recipes), else null. */
    @Nullable
    public static RecipeViewer get() {
        //? if neoforge {
        if (failed || !ModList.get().isLoaded("jei")) {
        //?} else if fabric {
        /*if (failed || !FabricLoader.getInstance().isModLoaded("jei")) {
        *///?} else if forge {
        /*if (failed || !ModList.get().isLoaded("jei")) {
        *///?}
            return null;
        }
        try {
            // the adapter names JEI's classes: it is loaded only when JEI is installed
            return dev.dyslexic.cinematic.client.compat.jei.JeiRecipeViewer.current();
        } catch (LinkageError | RuntimeException e) {
            failed = true;
            Cinematic.LOGGER.warn("[Cinematic] JEI integration unavailable; the scenes go on without the field guide", e);
            return null;
        }
    }
}
