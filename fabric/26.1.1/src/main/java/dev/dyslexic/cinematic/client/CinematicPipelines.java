package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;

/** Register both passes, then explicitly assign their shader programs through Iris's optional public API. */
public final class CinematicPipelines {
    private CinematicPipelines() {}

    // 26.1.x uses forward depth; the reversed-depth comparison from 26.2 makes scene overlays cross the book.
    public static final RenderPipeline HIGHLIGHT = RenderPipelines.register(
            RenderPipeline.builder(copy(RenderPipelines.DEBUG_QUADS, RenderPipelines.DEBUG_QUADS.getShaderDefines()))
                    .withLocation(Cinematic.id("pipeline/highlight"))
                    .withCull(false).withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1, -10)).build());
    private static final java.util.Map<RenderPipeline, RenderPipeline> FADING = new java.util.IdentityHashMap<>();

    /** Preserve the original item shader, including tint, foil and custom material definitions. */
    static RenderPipeline fading(RenderPipeline original) {
        return FADING.computeIfAbsent(original, source -> {
            var values = new java.util.HashMap<>(source.getShaderDefines().values());
            values.put("ALPHA_CUTOUT", "0.001");
            var defines = new net.minecraft.client.renderer.ShaderDefines(values, source.getShaderDefines().flags());
            var snippet = copy(source, defines);
            var depth = source.getDepthStencilState();
            var builder = RenderPipeline.builder(snippet).withLocation(Cinematic.id("pipeline/fading_item_" + FADING.size()))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
            if (depth != null) builder.withDepthStencilState(new DepthStencilState(depth.depthTest(), false, depth.depthBiasScaleFactor(), depth.depthBiasConstant()));
            RenderPipeline result = RenderPipelines.register(builder.build());
            if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatibility.copyBinding(source, result);
            return result;
        });
    }
    /** The eyes pipeline as it blended before 1.21.2: added light (see {@link Fx#glow}). */
    public static final RenderPipeline GLOW = RenderPipelines.register(
            RenderPipeline.builder(copy(RenderPipelines.EYES, RenderPipelines.EYES.getShaderDefines()))
                    .withLocation(Cinematic.id("pipeline/glow"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build());

    private static RenderPipeline.Snippet copy(RenderPipeline source, net.minecraft.client.renderer.ShaderDefines defines) {
        return new RenderPipeline.Snippet(java.util.Optional.of(source.getVertexShader()),
                java.util.Optional.of(source.getFragmentShader()), java.util.Optional.of(defines),
                java.util.Optional.of(source.getSamplers()), java.util.Optional.of(source.getUniforms()),
                java.util.Optional.of(source.getColorTargetState()), java.util.Optional.ofNullable(source.getDepthStencilState()),
                java.util.Optional.of(source.getPolygonMode()), java.util.Optional.of(source.isCull()),
                java.util.Optional.of(source.getVertexFormat()), java.util.Optional.of(source.getVertexFormatMode()));
    }

    public static void initialize() {
        if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatibility.initialize();
    }
}
