package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.CinematicClient;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void cinematic$pageTarget(CallbackInfoReturnable<com.mojang.blaze3d.pipeline.RenderTarget> cir) {
        if (dev.dyslexic.cinematic.client.BookPages.paintingTarget != null)
            cir.setReturnValue(dev.dyslexic.cinematic.client.BookPages.paintingTarget);
    }

    /** Where NeoForge's RenderFrameEvent.Pre fires: after this frame's ticks, right before the frame is drawn. */
    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void cinematic$frame(CallbackInfo ci) { Scene.frame(); }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cinematic$screen(Screen next, CallbackInfo ci) { if (CinematicClient.screenOpening(next)) ci.cancel(); }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cinematic$use(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void cinematic$used(CallbackInfo ci) { dev.dyslexic.cinematic.ClientBridge.swallowSwing = false; }
    /** A use made outside startUseItem (another mod, the showcase) leaves no swing to skip past its own tick. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void cinematic$tick(CallbackInfo ci) { dev.dyslexic.cinematic.ClientBridge.swallowSwing = false; }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$attack(CallbackInfoReturnable<Boolean> cir) { if (Scene.active()) cir.setReturnValue(false); }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$break(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
