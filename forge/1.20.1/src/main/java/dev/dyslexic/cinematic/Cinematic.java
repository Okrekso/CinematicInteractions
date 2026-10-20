package dev.dyslexic.cinematic;

import com.mojang.logging.LogUtils;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.StationEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Cinematic.MODID)
public final class Cinematic {
    public static final String MODID = "cinematic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cinematic() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Network.register();
        MinecraftForge.EVENT_BUS.register(StationEvents.class);
        // the client half lives in classes a dedicated server never loads
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.dyslexic.cinematic.client.CinematicClient.init(modBus));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
