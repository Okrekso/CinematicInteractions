//? if fabric {
/*package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dyslexic.cinematic.client.CinematicClient;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* RenderPlayerEvent.Pre / Post: someone sitting by a campfire is drawn lower, on the ground. ^/
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @WrapMethod(method = RENDER)
    private void cinematic$hideLocalBodyInScene(AbstractClientPlayer player, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        // Yes Steve Model (and other body renderers) can draw at PlayerRenderer's HEAD, before a cancellable
        // injection. Wrapping the whole method hides only our own first-person body and its held item.
        if (player != Minecraft.getInstance().player || !FirstPersonBodyCompat.hideLocalBody()) {
            original.call(player, yaw, partialTick, pose, buffers, light);
        }
    }

    @Inject(method = RENDER, at = @At("HEAD"))
    private void cinematic$seat(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            CallbackInfo ci) {
        if (CinematicClient.seat(player)) {
            pose.pushPose();
            pose.translate(0, -CinematicClient.sittingDrop(), 0);
        }
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void cinematic$unseat(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            CallbackInfo ci) {
        if (CinematicClient.unseat(player)) {
            pose.popPose();
        }
    }
}
*///?} else if forge {
/*package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* Stops the local vanilla body from covering a first-person station scene. ^/
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void cinematic$hideLocalBodyInScene(AbstractClientPlayer player, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player && FirstPersonBodyCompat.hideLocalBody()) ci.cancel();
    }
}
*///?}
