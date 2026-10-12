package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.dyslexic.cinematic.Cinematic;
import java.lang.reflect.Method;

/**
 * Loaded only when Iris is present. Its public API is called by name, so the mod neither bundles Iris nor needs a
 * build of it for every game version: each of our pipelines goes to the shader-pack program vanilla's closest pipeline
 * goes to.
 */
final class IrisCompatibility {
    private IrisCompatibility() {}

    static void initialize() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object iris = api.getMethod("getInstance").invoke(null);
            Method assign = api.getMethod("assignPipeline", RenderPipeline.class, program);
            assign.invoke(iris, CinematicPipelines.HIGHLIGHT, program(program, "BASIC"));
            assign.invoke(iris, CinematicPipelines.FADING_ITEM, program(program, "ENTITIES_TRANSLUCENT"));
            // vanilla's eyes pipeline goes to the pack's glowing-entity program too
            assign.invoke(iris, CinematicPipelines.GLOW, program(program, "EMISSIVE_ENTITIES"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            Cinematic.LOGGER.warn("Iris is installed, but its pipeline API could not be used; scene effects keep vanilla shaders", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object program(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }
}
