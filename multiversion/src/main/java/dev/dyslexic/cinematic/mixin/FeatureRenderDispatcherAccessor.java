//? if <26.2 {
/*package dev.dyslexic.cinematic.mixin;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherAccessor {
    @Mutable @Accessor("submitNodeStorage")
    void cinematic$setSubmitNodeStorage(SubmitNodeStorage storage);

    @Accessor("bufferSource")
    MultiBufferSource.BufferSource cinematic$getBufferSource();
}
*///?}
