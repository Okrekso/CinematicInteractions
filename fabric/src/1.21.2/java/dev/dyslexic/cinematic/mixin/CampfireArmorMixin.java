package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.KneeLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The right boot and legging are drawn around the bent knee by KneeLayer. */
@Mixin(HumanoidArmorLayer.class)
public abstract class CampfireArmorMixin {
    /** The player whose armor is being drawn (renderArmorPiece is not given it), or -1. */
    @Unique
    private int cinematic$player = -1;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"))
    private void cinematic$whose(PoseStack pose, MultiBufferSource buffers, int light, HumanoidRenderState state, float yaw, float pitch,
            CallbackInfo ci) {
        cinematic$player = state instanceof PlayerRenderState player ? player.id : -1;
    }

    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void cinematic$hideStraightArmor(PoseStack pose, MultiBufferSource buffers, ItemStack stack, EquipmentSlot slot, int light,
            HumanoidModel<?> model, CallbackInfo ci) {
        if ((slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) && cinematic$player >= 0 && KneeLayer.sitting(cinematic$player)) {
            model.rightLeg.visible = false;
        }
    }
}
