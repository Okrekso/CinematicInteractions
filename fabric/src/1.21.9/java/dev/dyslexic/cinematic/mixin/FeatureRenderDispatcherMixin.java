package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.SceneRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Once the world's entities and block entities are drawn, the scene draws its own (SceneRenderer.afterFeatures). */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {
    @Inject(method = "renderAllFeatures", at = @At("TAIL"))
    private void cinematic$scene(CallbackInfo ci) {
        SceneRenderer.afterFeatures();
    }
}
