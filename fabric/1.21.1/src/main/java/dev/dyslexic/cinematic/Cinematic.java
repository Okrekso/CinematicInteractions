package dev.dyslexic.cinematic;

import com.mojang.logging.LogUtils;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.StationEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class Cinematic implements ModInitializer {
    public static final String MODID = "cinematic";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        Network.register();
        StationEvents.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
