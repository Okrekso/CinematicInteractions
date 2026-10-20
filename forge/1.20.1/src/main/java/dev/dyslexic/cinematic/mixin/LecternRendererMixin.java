package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.StationDisplayLayouts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the player reads at a lectern, the scene draws the open book itself: the lectern's own book is left out. */
@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true)
    private void cinematic$readBook(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            CallbackInfo ci) {
        if (LecternStation.drawsBookAt(lectern.getBlockPos())) {
            ci.cancel();
        } else if (StationDisplayLayouts.customLectern()) {
            pose.pushPose();
            StationDisplayLayouts.lectern(lectern, pose);
        }
    }

    // RETURN, not TAIL: the shipped bytecode returns early for a lectern without a book (the recompiled dev class
    // has one return), and a missed pop leaves the level's pose stack unbalanced: "Pose stack not empty"
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("RETURN"))
    private void cinematic$restoreLayout(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (StationDisplayLayouts.customLectern()) pose.popPose();
    }
}
