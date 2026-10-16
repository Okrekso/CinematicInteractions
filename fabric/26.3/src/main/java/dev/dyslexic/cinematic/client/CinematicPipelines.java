package dev.dyslexic.cinematic.client;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;

/** Register both passes, then explicitly assign their shader programs through Iris's optional public API. */
public final class CinematicPipelines {
    private CinematicPipelines() {}

    public static final RenderPipeline HIGHLIGHT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/highlight"))
                    .withCull(false).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1, 10)).build());
    private static final java.util.Map<RenderPipeline, RenderPipeline> FADING = new java.util.IdentityHashMap<>();

    /** Preserve the original item shader, including tint, foil and custom material definitions. */
    static RenderPipeline fading(RenderPipeline original) {
        return FADING.computeIfAbsent(original, source -> {
            var values = new java.util.HashMap<>(source.getShaderDefines().values());
            values.put("ALPHA_CUTOUT", "0.001");
            var defines = new net.minecraft.client.renderer.ShaderDefines(values, source.getShaderDefines().flags());
            var snippet = new RenderPipeline.Snippet(source.getShaders(), java.util.Optional.of(defines),
                    java.util.Optional.of(source.getBindGroupLayouts()),
                    source.getColorTargetStates().toArray(ColorTargetState[]::new), source.getColorTargetStates().size(),
                    java.util.Optional.ofNullable(source.getDepthStencilState()), java.util.Optional.of(source.getPolygonMode()),
                    java.util.Optional.of(false), source.getVertexFormatBindings().toArray(com.mojang.renderpearl.api.vertex.VertexFormat[]::new),
                    java.util.Optional.of(source.getPrimitiveTopology()), source.pushConstantSize());
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
            RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
                    .withLocation(Cinematic.id("pipeline/glow"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .build());

    public static void initialize() {
        if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatibility.initialize();
    }
}
