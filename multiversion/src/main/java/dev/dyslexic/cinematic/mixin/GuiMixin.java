package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
@Mixin(net.minecraft.client.gui.Hud.class)
//?} else if <26.2 {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?}
public abstract class GuiMixin {
    @Inject(method = {"extractCrosshair", "extractHotbarAndDecorations", "extractEffects", "extractCameraOverlays", "extractOverlayMessage", "extractTitle", "extractSelectedItemName", "extractScoreboardSidebar", "extractDemoOverlay", "extractSleepOverlay"},
            at = @At("HEAD"), cancellable = true)
    private void cinematic$hide(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
