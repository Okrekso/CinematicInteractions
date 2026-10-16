package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;

/** Scene hands own the entire first-person pass, including renderers injected before NeoForge's hand event. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    @WrapMethod(method = "renderHandsWithItems")
    private void cinematic$hideHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers,
            LocalPlayer player, int light, Operation<Void> original) {
        // A cancellable HEAD injection alone can still run after another mod's HEAD renderer.
        if (!Scene.active()) original.call(partialTick, pose, buffers, player, light);
    }
}
