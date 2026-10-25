package dev.dyslexic.cinematic.mixin;

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
