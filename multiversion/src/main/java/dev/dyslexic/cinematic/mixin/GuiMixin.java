package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
@Mixin(net.minecraft.client.gui.Hud.class)
//?} else if >=26.1 <26.2 {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?} else if <26.1 {
/*@Mixin(Gui.class)
*///?}
public abstract class GuiMixin {
    //? if >=26.1 {
    @Inject(method = {"extractCrosshair", "extractHotbarAndDecorations", "extractEffects", "extractCameraOverlays", "extractOverlayMessage", "extractTitle", "extractSelectedItemName", "extractScoreboardSidebar", "extractDemoOverlay", "extractSleepOverlay"},
    //?} else if <26.1 {
    /*@Inject(method = {"renderCrosshair", "renderHotbarAndDecorations", "renderEffects", "renderCameraOverlays", "renderOverlayMessage", "renderTitle", "renderExperienceLevel", "renderSelectedItemName", "renderScoreboardSidebar", "renderDemoOverlay", "renderSleepOverlay"},
    *///?}
            at = @At("HEAD"), cancellable = true)
    private void cinematic$hide(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
