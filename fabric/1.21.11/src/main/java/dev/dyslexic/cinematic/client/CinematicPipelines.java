package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;

/** Register both passes, then explicitly assign their shader programs through Iris's optional public API. */
public final class CinematicPipelines {
    private CinematicPipelines() {}

    public static final RenderPipeline HIGHLIGHT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/highlight"))
                    .withCull(false).withDepthWrite(false).withDepthBias(-1, -10).build());
    public static final RenderPipeline FADING_ITEM = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/fading_item"))
                    .withSampler("Sampler1").withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false).withDepthWrite(false).build());
    /** The pipeline an item fading in or out is drawn with (1.21.11 has one translucent entity pipeline for all of them). */
    static RenderPipeline fading(RenderPipeline original) {
        return FADING_ITEM;
    }

    /** The eyes pipeline as it blended before 1.21.2: added light (see {@link Fx#glow}). */
    public static final RenderPipeline GLOW = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/glow"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withSampler("Sampler0").withBlend(BlendFunction.ADDITIVE).withDepthWrite(false)
                    .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
                            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                    .build());

    /** Pixel enchantment circles: blended as they are drawn, tested against the scene's depth but not writing it. */
    public static final RenderPipeline ENCHANT_CIRCLE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/enchant_circle"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withSampler("Sampler0").withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false)
                    .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
                            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                    .build());

    public static void initialize() {
        if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatibility.initialize();
    }
}
