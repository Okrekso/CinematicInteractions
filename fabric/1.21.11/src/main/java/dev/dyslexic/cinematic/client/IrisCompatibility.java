package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.dyslexic.cinematic.Cinematic;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.RenderPipelines;

/** Optional Iris API bindings; the scene must follow the main world's features, never its shadow batches. */
final class IrisCompatibility {
    private IrisCompatibility() {}

    private static Object iris;
    private static Method shadowPass;

    static void initialize() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            iris = api.getMethod("getInstance").invoke(null);
            shadowPass = api.getMethod("isRenderingShadowPass");
            Method assign = api.getMethod("assignPipeline", RenderPipeline.class, program);
            assign.invoke(iris, CinematicPipelines.HIGHLIGHT, program(program, "BASIC"));
            try {
                copyEntityBindings();
            } catch (ReflectiveOperationException e) {
                assign.invoke(iris, CinematicPipelines.FADING_ITEM, program(program, "ENTITIES_TRANSLUCENT"));
                assign.invoke(iris, CinematicPipelines.GLOW, program(program, "EMISSIVE_ENTITIES"));
                Cinematic.LOGGER.warn("This Iris version lacks vanilla pipeline binding copies; using its public program selection", e);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Cinematic.LOGGER.warn("Iris is installed, but its pipeline API could not be used; scene effects keep vanilla shaders", e);
        }
    }

    private static void copyEntityBindings() throws ReflectiveOperationException {
        // Iris's API compares vertex formats by identity. Its extended entity format differs from NEW_ENTITY,
        // so EMISSIVE_ENTITIES otherwise selects the particle program SPS and loses our textured glow quads.
        // Copy only Iris's program bindings; our pipelines retain their blending, depth and vertex layout.
        Class<?> pipelines = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
        Method copy = pipelines.getMethod("copyPipeline", RenderPipeline.class, RenderPipeline.class);
        copy.invoke(null, RenderPipelines.EYES, CinematicPipelines.GLOW);
        copy.invoke(null, RenderPipelines.ENTITY_TRANSLUCENT, CinematicPipelines.FADING_ITEM);
    }

    static boolean inShadowPass() {
        if (shadowPass == null) return false;
        try {
            return (boolean) shadowPass.invoke(iris);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not identify the Iris world rendering pass", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object program(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }
}
