package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.dyslexic.cinematic.client.BookPages;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Redirect only the synchronous GUI pass that paints a book page into its own texture. */
@Mixin(RenderStateShard.OutputStateShard.class)
public abstract class RenderOutputMixin {
    @Inject(method = "getRenderTarget", at = @At("HEAD"), cancellable = true)
    private void cinematic$pageTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (BookPages.paintingTarget != null) cir.setReturnValue(BookPages.paintingTarget);
    }
}
