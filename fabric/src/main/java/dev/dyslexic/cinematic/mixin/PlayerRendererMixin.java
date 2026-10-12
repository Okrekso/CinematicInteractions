package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.PropLayer;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void cinematic$layer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((PlayerRenderer) (Object) this).addLayer(new PropLayer((PlayerRenderer) (Object) this));
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void cinematic$seat(AbstractClientPlayer player, float yaw, float tick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE) { pose.pushPose(); pose.translate(0, -0.62, 0); }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void cinematic$unseat(AbstractClientPlayer player, float yaw, float tick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE) pose.popPose();
    }
}
