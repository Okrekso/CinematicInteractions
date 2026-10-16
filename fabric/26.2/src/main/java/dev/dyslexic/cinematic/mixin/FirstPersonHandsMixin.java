package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;

/** Scene hands own the complete hand pass, including renderers injected before the vanilla body. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    @WrapMethod(method = "submitHandsWithItems")
    private void cinematic$sceneHands(float partialTick, PoseStack pose, SubmitNodeCollector collector,
            LocalPlayer player, int light, Operation<Void> original) {
        if (!Scene.active()) original.call(partialTick, pose, collector, player, light);
    }
}
