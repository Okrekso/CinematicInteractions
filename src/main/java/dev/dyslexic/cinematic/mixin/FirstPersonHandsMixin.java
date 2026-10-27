package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
//? if !forge {
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//?}
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
//? if forge {
/*import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/** Scene hands own the entire first-person pass, including renderers injected before NeoForge's hand event. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    //? if !forge {
    @WrapMethod(method = "renderHandsWithItems")
    private void cinematic$hideHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers,
            LocalPlayer player, int light, Operation<Void> original) {
        // A cancellable HEAD injection alone can still run after another mod's HEAD renderer.
        if (!Scene.active()) original.call(partialTick, pose, buffers, player, light);
    //?} else if forge {
    /*@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void cinematic$hideHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light,
            CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    *///?}
    }
}
