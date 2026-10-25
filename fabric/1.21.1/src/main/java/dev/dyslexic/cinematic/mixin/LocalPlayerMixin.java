package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.CinematicClient;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    public Input input;

    /** PlayerTickEvent.Post: the end of Player.tick, before the client sends where the player is and looks. */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.AFTER))
    private void cinematic$steer(CallbackInfo ci) {
        CinematicClient.onPlayerTick((LocalPlayer) (Object) this);
    }

    /** MovementInputUpdateEvent: right after the keys were read into this tick's input. */
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void cinematic$input(CallbackInfo ci) {
        Scene.movementInput(input);
    }

    /** The swing of a use that started a scene (Fabric shows it for SUCCESS, NeoForge's CONSUME has none). */
    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void cinematic$noSwingIntoScene(CallbackInfo ci) {
        if (ClientBridge.swallowSwing) {
            ClientBridge.swallowSwing = false;
            ci.cancel();
        }
    }
}
