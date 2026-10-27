//? if fabric {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Showcase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /^* Showcase drives MouseHandler coordinates itself; desktop movement must not overwrite its input. ^/
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void cinematic$showcaseCursor(CallbackInfo ci) { if (Showcase.ENABLED) ci.cancel(); }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void cinematic$turn(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void cinematic$scroll(CallbackInfo ci) {
        if (Scene.active() && Minecraft.getInstance().screen == null) ci.cancel();
    }
}
*///?} else if forge {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * While the scene drives the view the mouse does not turn the player (NeoForge has an event for this; Forge 1.20.1 does
 * not). The movement gathered since the last frame is dropped before vanilla turns by it, so none of it is applied later.
 ^/
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void cinematic$sceneHoldsTheView(CallbackInfo ci) {
        if (Scene.active()) {
            accumulatedDX = 0;
            accumulatedDY = 0;
            smoothTurnX.reset();
            smoothTurnY.reset();
        }
    }
}
*///?}
