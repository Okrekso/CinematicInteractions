package dev.dyslexic.cinematic;

import com.mojang.logging.LogUtils;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.StationEvents;
//? if fabric {
/*import net.fabricmc.api.ModInitializer;
*///?}
import net.minecraft.resources.ResourceLocation;
//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
//?} else if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?}
import org.slf4j.Logger;

//? if neoforge {
@Mod(Cinematic.MODID)
public final class Cinematic {
//?} else if fabric {
/*public final class Cinematic implements ModInitializer {
*///?} else if forge {
/*@Mod(Cinematic.MODID)
public final class Cinematic {
*///?}
    public static final String MODID = "cinematic";
    public static final Logger LOGGER = LogUtils.getLogger();

    //? if neoforge {
    public Cinematic(IEventBus modBus, ModContainer container) {
        modBus.addListener(Network::register);
        NeoForge.EVENT_BUS.register(StationEvents.class);
    //?} else if fabric {
    /*@Override
    public void onInitialize() {
        Network.register();
        StationEvents.register();
    *///?} else if forge {
    /*public Cinematic() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Network.register();
        MinecraftForge.EVENT_BUS.register(StationEvents.class);
        // the client half lives in classes a dedicated server never loads
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.dyslexic.cinematic.client.CinematicClient.init(modBus));
    *///?}
    }

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
        //?} else if <1.21 {
        /*return new ResourceLocation(MODID, path);
        *///?}
    }
}
