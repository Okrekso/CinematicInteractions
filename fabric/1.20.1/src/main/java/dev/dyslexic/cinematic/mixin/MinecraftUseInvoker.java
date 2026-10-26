package dev.dyslexic.cinematic.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The use key as the player presses it, for ClientOnlySmoke. NeoForge's copy calls it by reflection with its Mojang name,
 * which the release JAR on Fabric does not have (its game runs with intermediary names); the invoker is remapped.
 */
@Mixin(Minecraft.class)
public interface MinecraftUseInvoker {
    @Invoker("startUseItem")
    void cinematic$startUseItem();
}
