package dev.dyslexic.cinematic.mixin;
//? if <26.2 {

/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.2 {
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
//?}
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    //? if >=26.2 {
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Inject(method="submitFeatures", at=@At("HEAD"))
    private void cinematic$outline(LevelRenderState state, SubmitNodeCollector collector, boolean outline, CallbackInfo ci) {
        if (Scene.active()) state.blockOutlineRenderState = null;
    //?} else if <26.2 {
    /*@Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cinematic$noOutline(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    *///?}
    }
    //? if >=26.2 {
    @Inject(method="submitFeatures", at=@At("TAIL"))
    private void cinematic$scene(LevelRenderState state, SubmitNodeCollector collector, boolean outline, CallbackInfo ci) {
        SceneRenderer.append(state, submitNodeStorage);
    //?} else if <26.2 {

    /*@Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void cinematic$scene(PoseStack pose, LevelRenderState state, SubmitNodeStorage collector, CallbackInfo ci) {
        SceneRenderer.renderInto(state, collector);
    *///?}
    }
}
