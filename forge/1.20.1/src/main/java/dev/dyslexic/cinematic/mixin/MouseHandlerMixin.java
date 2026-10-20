package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the scene drives the view the mouse does not turn the player (NeoForge has an event for this; Forge 1.20.1 does
 * not). The movement gathered since the last frame is dropped before vanilla turns by it, so none of it is applied later.
 */
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
