package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Where scene geometry goes. 1.21.11 draws the scene straight into the game's buffers after the level's features
 * ({@link SceneRenderer}), so this is those buffers under the name the later ports' code uses.
 */
public interface SceneBuffers extends MultiBufferSource {
    @Override
    VertexConsumer getBuffer(RenderType type);

    /** The same buffers seen as scene buffers. */
    static SceneBuffers of(MultiBufferSource source) {
        return source instanceof SceneBuffers scene ? scene : source::getBuffer;
    }
}
