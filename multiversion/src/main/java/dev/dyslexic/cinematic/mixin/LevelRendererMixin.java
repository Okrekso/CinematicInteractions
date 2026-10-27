package dev.dyslexic.cinematic.mixin;
//? if >=26.1 <26.2 {

/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else if <26.1 {

/*import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
*///?}
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneRenderer;
//? if <26.1 {
/*import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
*///?}
import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
//?} else if >=26.1 <26.2 {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?} else if <26.1 {
/*import org.joml.Matrix4f;
import org.joml.Vector4f;
*///?}
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
    }
    @Inject(method="submitFeatures", at=@At("TAIL"))
    private void cinematic$scene(LevelRenderState state, SubmitNodeCollector collector, boolean outline, CallbackInfo ci) {
        SceneRenderer.append(state, submitNodeStorage);
    //?} else if >=26.1 <26.2 {
    /*@Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cinematic$noOutline(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void cinematic$scene(PoseStack pose, LevelRenderState state, SubmitNodeStorage collector, CallbackInfo ci) {
        SceneRenderer.renderInto(state, collector);
    *///?} else if <26.1 {
    /*@Inject(method = "renderLevel", at = @At("HEAD"))
    private void cinematic$begin(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean outline, Camera camera, Matrix4f view,
            Matrix4f projection, Matrix4f cullingProjection, GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo ci) {
        SceneRenderer.beginLevel(camera, view, projection);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void cinematic$end(CallbackInfo ci) {
        SceneRenderer.endLevel();
    }

    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cinematic$noOutline(CallbackInfo ci) {
        if (Scene.active()) {
            ci.cancel();
        }
    *///?}
    }
}
