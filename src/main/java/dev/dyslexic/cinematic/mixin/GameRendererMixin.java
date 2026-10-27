//? if fabric {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^* ViewportEvent.ComputeFov: the scene narrows the configured field of view (not the hand's, which is not drawn). ^/
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cinematic$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (Scene.active() && useFovSetting) {
            cir.setReturnValue(CameraRig.fov(cir.getReturnValue()));
        }
    }
}
*///?}
