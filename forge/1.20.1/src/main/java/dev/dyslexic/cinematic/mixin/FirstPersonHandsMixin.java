package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scene hands own the entire first-person pass, including renderers that other mods inject before Forge's hand event.
 * Forge 1.20.1 does not ship MixinExtras, so this is a cancellable HEAD injection rather than a wrapped method (Forge's
 * own hand event is cancelled as well, {@code CinematicClient.onRenderHand}).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void cinematic$hideHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light,
            CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }
}
