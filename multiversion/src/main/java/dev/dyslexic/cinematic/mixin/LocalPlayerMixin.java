package dev.dyslexic.cinematic.mixin;

//? if <26.3 {
/*import dev.dyslexic.cinematic.ClientBridge;
*///?}
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void cinematic$steer(CallbackInfo ci) { Scene.steer((LocalPlayer) (Object) this); }
    //? if >=26.2 <26.3 {

    /*@Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void cinematic$noSwingIntoScene(CallbackInfo ci) {
        if (ClientBridge.swallowSwing) {
            ClientBridge.swallowSwing = false;
            ci.cancel();
        }
    }

    *///?}
}
