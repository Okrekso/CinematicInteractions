package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The world's frame, where Fabric's world render events used to be (1.21.9 has none): its matrices for picking and the
 * scene, and no block selection box over a scene (as NeoForge's RenderHighlightEvent.Block).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
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
    }
}
