package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.LecternStation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the player reads at a lectern, the scene draws the open book itself: the lectern's own book is left out. */
@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void cinematic$readBook(LecternRenderState lectern, PoseStack pose, SubmitNodeCollector features, CameraRenderState camera, CallbackInfo ci) {
        if (LecternStation.drawsBookAt(lectern.blockPos)) {
            ci.cancel();
        }
    }
}
