package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4f;

/** World drawing: what lies on and in the stations for everyone, the sparks, plus the scene's extras and the arms. */
public final class SceneRenderer {
    private SceneRenderer() {}

    /** A thin warm outline round the neighbouring station under the cursor (a right click moves over to it). */
    private static void outlineNeighbor(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ClientLevel level) {
        if (Scene.neighbor == null) {
            return;
        }
        net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(Scene.neighbor).getShape(level, Scene.neighbor);
        if (shape.isEmpty()) {
            return;
        }
        net.minecraft.world.phys.AABB box = shape.bounds().move(Scene.neighbor).inflate(0.004).move(-cam.x, -cam.y, -cam.z);
        net.minecraft.client.renderer.ShapeRenderer.renderLineBox(pose.last(), buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines()), box, 1.0f, 0.88f, 0.6f, 0.75f);
    }

    // Since 1.21.9 the game draws the world's entities as a batch of "features" and Fabric has no world events here:
    // the scene is drawn right after that batch, where Fabric's AFTER_ENTITIES used to come (see LevelRendererMixin).
    private static boolean inLevel, drawing;
    private static Camera levelCamera;
    private static Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());

    /** The world's frame begins: this frame's camera and matrices. */
    public static void beginLevel(Camera camera, Matrix4f view, Matrix4f projection) {
        Picking.capture(projection, view, camera.getPosition());
        levelCamera = camera;
        frustum = new Frustum(view, projection);
        Vec3 at = camera.getPosition();
        frustum.prepare(at.x, at.y, at.z);
        BrewingBottles.beginFrame();
        inLevel = true;
    }

    public static void endLevel() {
        inLevel = false;
    }

    /** The main world features were drawn: Iris shadow batches must not consume the scene's one draw. */
    public static void afterFeatures() {
        if (inLevel && !drawing && !IrisCompatibility.inShadowPass()) {
            drawing = true;
            try {
                render();
            } finally {
                drawing = false;
                inLevel = false;
            }
        }
    }

    /** Draws what was submitted to the game's features meanwhile (item models drawn by a special renderer). */
    static void drawSubmitted() {
        FeatureRenderDispatcher features = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        features.renderAllFeatures();
    }

    private static void render() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || levelCamera == null) {
            return;
        }
        Camera camera = levelCamera;
        Vec3 cam = camera.getPosition();
        PoseStack pose = new PoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        Scene.updateCursor();
        Hands.update(cam, camera.getYRot());

        BrewingBottles.renderAll(pose, buffers, cam, level);
        TableRenderer.renderAll(pose, buffers, cam, frustum, level);
        FurnaceRenderer.renderAll(pose, buffers, cam, frustum, level);
        EnchantRenderer.renderAll(pose, buffers, camera, frustum, level);
        MenuRenderer.renderAll(pose, buffers, camera, frustum, level);
        ShelfBooks.renderAll(pose, buffers, camera, frustum, level);
        Fx.renderSparks(pose, buffers, camera, Scene.frameDt);

        ClientStation station = Scene.station;
        if (Scene.active() && station != null) {
            int light = LevelRenderer.getLightColor(level, station.lightPos());
            if (Scene.phase == Scene.Phase.ACTIVE) {
                station.renderWorld(pose, buffers, cam, light);
                outlineNeighbor(pose, buffers, cam, level);
            }
            if (!FreeLook.showsBody()) {
                // looking round from behind, the game draws the player's body as others see it instead
                station.renderBody(pose, buffers, cam, light);
                Hands.render(pose, buffers, cam, camera.getYRot(), light);
            }
        }
        drawSubmitted();
        buffers.endBatch();
        if (Showcase.ENABLED) {
            FrameProbe.sample(camera);
        }
    }
}
