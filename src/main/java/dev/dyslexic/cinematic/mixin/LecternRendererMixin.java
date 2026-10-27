package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.StationDisplayLayouts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the player reads at a lectern, the scene draws the open book itself: the lectern's own book is left out. */
@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V";

    /** Whether this call pushed the layout's pose, so the pose stack is put back exactly as it was found. */
    @Unique
    private boolean cinematic$pushed;

    @Inject(method = RENDER, at = @At("HEAD"), cancellable = true)
    private void cinematic$readBook(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
            CallbackInfo ci) {
        cinematic$pushed = false;
        if (LecternStation.drawsBookAt(lectern.getBlockPos())) {
            ci.cancel();
        } else if (StationDisplayLayouts.customLectern()) {
            pose.pushPose();
            cinematic$pushed = true;
            StationDisplayLayouts.lectern(lectern, pose);
        }
    }

    // RETURN, not TAIL: the game's own bytecode returns early for a lectern without a book, and TAIL is only the last
    // return. A pose pushed above and not popped there ends the frame with "Pose stack not empty".
    @Inject(method = RENDER, at = @At("RETURN"))
    private void cinematic$restoreLayout(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay, CallbackInfo ci) {
        if (cinematic$pushed) {
            cinematic$pushed = false;
            pose.popPose();
        }
    }
}
