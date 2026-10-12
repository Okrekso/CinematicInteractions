package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Showcase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void cinematic$initialized(Minecraft mc, int width, int height, CallbackInfo ci) { Showcase.onScreen((Screen) (Object) this); }
}
