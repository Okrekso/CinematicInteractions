package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;

/** Include early third-party hand renderers in the pass owned by the scene. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
    @WrapMethod(method = "renderHandsWithItems")
    private void cinematic$sceneHands(float partialTick, PoseStack pose, SubmitNodeCollector collector,
            LocalPlayer player, int light, Operation<Void> original) {
        if (!Scene.active()) original.call(partialTick, pose, collector, player, light);
    }
}
