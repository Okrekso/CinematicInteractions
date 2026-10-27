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

    // 26.1.x uses forward depth; the reversed-depth comparison from 26.2 makes scene overlays cross the book.
    public static final RenderPipeline HIGHLIGHT = RenderPipelines.register(
            //? if >=26.3 {
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            //?} else if <26.3 {
            /*RenderPipeline.builder(copy(RenderPipelines.DEBUG_QUADS, RenderPipelines.DEBUG_QUADS.getShaderDefines()))
            *///?}
                    .withLocation(Cinematic.id("pipeline/highlight"))
                    //? if >=26.2 {
                    .withCull(false).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1, 10)).build());
                    //?} else if <26.2 {
                    /*.withCull(false).withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1, -10)).build());
                    *///?}
    private static final java.util.Map<RenderPipeline, RenderPipeline> FADING = new java.util.IdentityHashMap<>();

    /** Preserve the original item shader, including tint, foil and custom material definitions. */
    static RenderPipeline fading(RenderPipeline original) {
        return FADING.computeIfAbsent(original, source -> {
            var values = new java.util.HashMap<>(source.getShaderDefines().values());
            values.put("ALPHA_CUTOUT", "0.001");
            var defines = new net.minecraft.client.renderer.ShaderDefines(values, source.getShaderDefines().flags());
            //? if >=26.3 {
            var snippet = new RenderPipeline.Snippet(source.getShaders(), java.util.Optional.of(defines),
                    java.util.Optional.of(source.getBindGroupLayouts()),
                    source.getColorTargetStates().toArray(ColorTargetState[]::new), source.getColorTargetStates().size(),
                    java.util.Optional.ofNullable(source.getDepthStencilState()), java.util.Optional.of(source.getPolygonMode()),
                    java.util.Optional.of(false), source.getVertexFormatBindings().toArray(com.mojang.renderpearl.api.vertex.VertexFormat[]::new),
                    java.util.Optional.of(source.getPrimitiveTopology()), source.pushConstantSize());
            //?} else if <26.3 {
            /*var snippet = copy(source, defines);
            *///?}
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
            //? if >=26.3 {
            RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
            //?} else if <26.3 {
            /*RenderPipeline.builder(copy(RenderPipelines.EYES, RenderPipelines.EYES.getShaderDefines()))
            *///?}
                    .withLocation(Cinematic.id("pipeline/glow"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                    //? if >=26.2 {
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    //?} else if <26.2 {
                    /*.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    *///?}
                    .build());

    /** The circle is emissive, but its translucent pixels must retain depth testing. */
    public static final RenderPipeline ENCHANT_CIRCLE = RenderPipelines.register(
            //? if >=26.3 {
            RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
            //?} else if <26.3 {
            /*RenderPipeline.builder(copy(RenderPipelines.EYES, RenderPipelines.EYES.getShaderDefines()))
            *///?}
                    .withLocation(Cinematic.id("pipeline/enchant_circle"))
                    .withVertexShader("core/entity").withFragmentShader("core/entity")
                    //? if >=26.3 {
                    .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    //?} else if >=26.2 <26.3 {
                    /*.withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    *///?} else if <26.2 {
                    /*.withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                    *///?}
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    //? if >=26.2 {
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    //?} else if <26.2 {
                    /*.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    *///?}
                    .build());
    //? if >=26.2 <26.3 {

    /*private static RenderPipeline.Snippet copy(RenderPipeline source, net.minecraft.client.renderer.ShaderDefines defines) {
        return new RenderPipeline.Snippet(java.util.Optional.of(source.getVertexShader()),
                java.util.Optional.of(source.getFragmentShader()), java.util.Optional.of(defines),
                java.util.Optional.of(source.getBindGroupLayouts()), source.getColorTargetStates(),
                source.getColorTargetStates().length, java.util.Optional.ofNullable(source.getDepthStencilState()),
                java.util.Optional.of(source.getPolygonMode()), java.util.Optional.of(false),
                source.getVertexFormatBindings(), java.util.Optional.of(source.getPrimitiveTopology()));
    }
    *///?} else if <26.2 {

    /*private static RenderPipeline.Snippet copy(RenderPipeline source, net.minecraft.client.renderer.ShaderDefines defines) {
        return new RenderPipeline.Snippet(java.util.Optional.of(source.getVertexShader()),
                java.util.Optional.of(source.getFragmentShader()), java.util.Optional.of(defines),
                java.util.Optional.of(source.getSamplers()), java.util.Optional.of(source.getUniforms()),
                java.util.Optional.of(source.getColorTargetState()), java.util.Optional.ofNullable(source.getDepthStencilState()),
                java.util.Optional.of(source.getPolygonMode()), java.util.Optional.of(source.isCull()),
                java.util.Optional.of(source.getVertexFormat()), java.util.Optional.of(source.getVertexFormatMode()));
    }
    *///?}

    public static void initialize() {
        if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatibility.initialize();
    }
}
