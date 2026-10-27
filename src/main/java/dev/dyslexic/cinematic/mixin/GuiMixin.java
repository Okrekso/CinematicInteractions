//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = {"renderCrosshair", "renderHotbarAndDecorations", "renderEffects", "renderCameraOverlays", "renderOverlayMessage", "renderTitle", "renderExperienceLevel", "renderSelectedItemName", "renderScoreboardSidebar", "renderDemoOverlay", "renderSleepOverlay"},
            at = @At("HEAD"), cancellable = true)
    private void cinematic$hide(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * The HUD parts hidden in a scene. 1.20.1 draws the action bar message and the title inside Gui.render itself, so those
 * two stay; chat, subtitles, the player list and the debug screen stay on purpose, as on the other loaders.
 ^/
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = {"renderCrosshair", "renderHotbar", "renderEffects", "renderExperienceBar", "renderJumpMeter", "renderSelectedItemName",
            "renderDemoOverlay", "renderPlayerHealth", "renderVehicleHealth", "renderTextureOverlay", "renderSpyglassOverlay", "renderVignette",
            "renderPortalOverlay", "displayScoreboardSidebar"},
            at = @At("HEAD"), cancellable = true)
    private void cinematic$hide(CallbackInfo ci) { if (Scene.active()) ci.cancel(); }
}
*///?}
