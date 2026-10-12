package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.PropLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A smith swinging the hammer at an anvil holds nothing else in that hand, as others see them ({@code PropLayer}). */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void cinematic$hammerInHand(net.minecraft.client.renderer.entity.state.LivingEntityRenderState entity, net.minecraft.client.resources.model.BakedModel model, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose,
            MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (arm == HumanoidArm.RIGHT && (entity instanceof net.minecraft.client.renderer.entity.state.PlayerRenderState player ? PropLayer.hammer(player.id) : 0) > 0.001) {
            ci.cancel();
        }
    }
}
