package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cinematic$noOutline(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void cinematic$scene(PoseStack pose, LevelRenderState state, SubmitNodeStorage collector, CallbackInfo ci) {
        SceneRenderer.renderInto(state, collector);
    }
}
