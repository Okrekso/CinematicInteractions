package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shader renderers can call the hand renderer directly, bypassing GameRenderer's ordinary hand pass. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void cinematic$sceneHands(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }
}
