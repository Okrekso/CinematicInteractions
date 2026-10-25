package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.KneeLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The right boot and legging are drawn around the bent knee by KneeLayer. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void cinematic$hideStraightArmor(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
            EquipmentSlot slot, int light, HumanoidModel<?> model, CallbackInfo ci) {
        if ((slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET)
                && entity instanceof AbstractClientPlayer player && KneeLayer.sitting(player)) {
            model.rightLeg.visible = false;
        }
    }
}
