package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//? if >=26.3 {
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
//?} else if <26.3 {
/*import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.LocalPlayer;
*///?}
import net.minecraft.client.renderer.SubmitNodeCollector;
//? if >=26.3 {
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
//?}
import org.spongepowered.asm.mixin.Mixin;

/** Scene hands own the complete hand pass, including renderers injected before the vanilla body. */
//? if >=26.3 {
@Mixin(FirstPersonHandsAndItemsRenderer.class)
//?} else if <26.3 {
/*@Mixin(ItemInHandRenderer.class)
*///?}
public abstract class FirstPersonHandsMixin {
    //? if >=26.2 {
    @WrapMethod(method = "submitHandsWithItems")
    //?} else if <26.2 {
    /*@WrapMethod(method = "renderHandsWithItems")
    *///?}
    private void cinematic$sceneHands(float partialTick, PoseStack pose, SubmitNodeCollector collector,
            //? if >=26.3 {
            PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, Operation<Void> original) {
        if (!Scene.active()) original.call(partialTick, pose, collector, player, hands);
            //?} else if <26.3 {
            /*LocalPlayer player, int light, Operation<Void> original) {
        if (!Scene.active()) original.call(partialTick, pose, collector, player, light);
            *///?}
    }
}
