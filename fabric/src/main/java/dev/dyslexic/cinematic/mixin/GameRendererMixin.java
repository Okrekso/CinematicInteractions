package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cinematic$fov(Camera camera, float tick, boolean configured, CallbackInfoReturnable<Double> cir) {
        if (Scene.active() && configured) cir.setReturnValue(CameraRig.fov(cir.getReturnValue()));
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void cinematic$hands(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
