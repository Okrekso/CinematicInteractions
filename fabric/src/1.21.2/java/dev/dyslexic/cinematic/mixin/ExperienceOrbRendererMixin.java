package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Experience from a grindstone flies from the block straight into the player's eyes, which in a scene are right
 * behind the camera: the last stretch of that flight would fill the frame with a green blot. During a scene an
 * orb that close to the camera is not drawn; it is still picked up as usual.
 */
@Mixin(ExperienceOrbRenderer.class)
public abstract class ExperienceOrbRendererMixin {
    private static final double HIDE_WITHIN_SQR = 1.0;

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ExperienceOrbRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void cinematic$notInTheLens(net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState orb, PoseStack pose, MultiBufferSource buffers, int light,
            CallbackInfo ci) {
        if (Scene.active() && new net.minecraft.world.phys.Vec3(orb.x, orb.y, orb.z).distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
                < HIDE_WITHIN_SQR) {
            ci.cancel();
        }
    }
}
