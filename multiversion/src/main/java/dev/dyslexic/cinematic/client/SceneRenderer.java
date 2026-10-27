package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? if <26.1 {
/*import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.culling.Frustum;
//? if <26.1 {
/*import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
*///?}
import org.joml.Matrix4f;
//? if >=26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
//?} else if >=26.1 <26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?}

/** World drawing: what lies on and in the stations for everyone, the sparks, plus the scene's extras and the arms. */
public final class SceneRenderer {
    private SceneRenderer() {}

    /** A thin warm outline round the neighbouring station under the cursor (a right click moves over to it). */
    //? if >=26.1 {
    private static void outlineNeighbor(PoseStack pose, SceneBuffers buffers, Vec3 cam, ClientLevel level) {
    //?} else if <26.1 {
    /*private static void outlineNeighbor(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ClientLevel level) {
    *///?}
        if (Scene.neighbor == null) {
            return;
        }
        net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(Scene.neighbor).getShape(level, Scene.neighbor);
        if (shape.isEmpty()) {
            return;
        }
        net.minecraft.world.phys.AABB box = shape.bounds().move(Scene.neighbor).inflate(0.004).move(-cam.x, -cam.y, -cam.z);
        //? if >=26.2 {
        collector().submitShapeOutline(pose, net.minecraft.world.phys.shapes.Shapes.create(box),
                net.minecraft.client.renderer.rendertype.RenderTypes.lines(), 0xBFFFE099, 2.5f, false);
        //?} else if >=26.1 <26.2 {
        /*net.minecraft.client.renderer.ShapeRenderer.renderShape(pose,
                buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines()),
                net.minecraft.world.phys.shapes.Shapes.create(box), 0, 0, 0, 0xBFFFE099, 2.5f);
        *///?} else if <26.1 {
        /*net.minecraft.client.renderer.ShapeRenderer.renderShape(pose, buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines()), net.minecraft.world.phys.shapes.Shapes.create(box), 0, 0, 0, 0xBFFFE099, 2.5f);
        *///?}
    }

    //? if <26.1 {
    /*// Since 1.21.9 the game draws the world's entities as a batch of "features" and Fabric has no world events here:
    // the scene is drawn right after that batch, where Fabric's AFTER_ENTITIES used to come (see LevelRendererMixin).
    private static boolean inLevel, drawing;
    *///?}
    private static Camera levelCamera;
    //? if >=26.1 {
    private static SubmitNodeStorage collecting;
    //?}
    private static Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());

    /** The world's frame begins: this frame's camera and matrices. */
    //? if >=26.1 {
    private static void beginLevel(Camera camera, Matrix4f view, Matrix4f projection) {
    //?} else if <26.1 {
    /*public static void beginLevel(Camera camera, Matrix4f view, Matrix4f projection) {
    *///?}
        Picking.capture(projection, view, camera.position());
        levelCamera = camera;
        frustum = new Frustum(view, projection);
        Vec3 at = camera.position();
        frustum.prepare(at.x, at.y, at.z);
        //? if <26.1 {
        /*BrewingBottles.beginFrame();
        inLevel = true;
        *///?}
    }

    //? if >=26.1 {
    public static SubmitNodeCollector collector() {
        if (collecting == null) throw new IllegalStateException("Scene geometry outside extraction");
        return collecting;
    //?} else if <26.1 {
    /*public static void endLevel() {
        inLevel = false;
    *///?}
    }

    //? if >=26.2 {
    public static void beginExtraction() {
        BrewingBottles.beginFrame();
    }

    public static void extract() {
        Minecraft mc = Minecraft.getInstance();
        LevelRenderState state = mc.gameRenderer.gameRenderState().levelRenderState;
        collecting = new SubmitNodeStorage();
        try {
            beginLevel(mc.gameRenderer.mainCamera(), state.cameraRenderState.viewRotationMatrix, state.cameraRenderState.projectionMatrix);
            render();
            ((SceneFrameAccess) state).cinematic$setScene(collecting);
        } finally {
            collecting = null;
    //?} else if >=26.1 <26.2 {
    /*/^* 26.1.x submits block entities directly in the world pass, before the feature queue is drawn. ^/
    public static void renderInto(LevelRenderState state, SubmitNodeStorage world) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isGameLoadFinished() || mc.level == null) return;
        BrewingBottles.beginFrame();
        collecting = world;
        try {
            beginLevel(mc.gameRenderer.mainCamera(), state.cameraRenderState.viewRotationMatrix,
                    state.cameraRenderState.projectionMatrix);
            render();
        } finally {
            collecting = null;
    *///?} else if <26.1 {
    /*/^* The main world features were drawn: Iris shadow batches must not consume the scene's one draw. ^/
    public static void afterFeatures() {
        if (inLevel && !drawing && !IrisCompatibility.inShadowPass()) {
            drawing = true;
            try {
                render();
            } finally {
                drawing = false;
                inLevel = false;
            }
    *///?}
        }
    //? if >=26.2 {
    }

    @SuppressWarnings("unchecked")
    public static void append(LevelRenderState state, SubmitNodeStorage world) {
        SubmitNodeStorage scene = ((SceneFrameAccess) state).cinematic$getScene();
        if (scene == null) return;
        scene.getSubmitsPerOrder().forEach((order, source) -> {
            var target = world.order(order);
            for (int i = 0; i < source.allPhases().size(); i++) {
                FeatureRenderPhase<SubmitNode> phase = (FeatureRenderPhase<SubmitNode>) target.allPhases().get(i);
                source.allPhases().get(i).sortInto((node, ordered) -> phase.submit(node));
            }
        });
        ((SceneFrameAccess) state).cinematic$setScene(null);
    //?}
    }

    //? if <26.1 {
    /*/^* Draws what was submitted to the game's features meanwhile (item models drawn by a special renderer). ^/
    static void drawSubmitted() {
        FeatureRenderDispatcher features = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        features.renderAllFeatures();
    }

    *///?}
    private static void render() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || levelCamera == null) {
            return;
        }
        Camera camera = levelCamera;
        Vec3 cam = camera.position();
        PoseStack pose = new PoseStack();
        //? if >=26.1 {
        SceneBuffers.BufferSource buffers = new SceneBuffers.BufferSource(collector());
        //?} else if <26.1 {
        /*MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        SceneBuffers buffers = SceneBuffers.of(source);
        *///?}

        Scene.updateCursor();
        Hands.update(cam, camera.yRot());

        BrewingBottles.renderAll(pose, buffers, cam, level);
        TableRenderer.renderAll(pose, buffers, cam, frustum, level);
        FurnaceRenderer.renderAll(pose, buffers, cam, frustum, level);
        EnchantRenderer.renderAll(pose, buffers, camera, frustum, level);
        MenuRenderer.renderAll(pose, buffers, camera, frustum, level);
        ShelfBooks.renderAll(pose, buffers, camera, frustum, level);
        ShelfLayoutBooks.renderAll(pose, buffers, camera, frustum, level);
        Fx.renderSparks(pose, buffers, camera, Scene.frameDt);

        ClientStation station = Scene.station;
        if (Scene.active() && station != null) {
            //? if >=26.1 {
            int light = net.minecraft.util.LightCoordsUtil.getLightCoords(level, station.lightPos());
            //?} else if <26.1 {
            /*int light = LevelRenderer.getLightCoords(level, station.lightPos());
            *///?}
            if (station.kind() == dev.dyslexic.cinematic.station.StationKind.ADDON
                    && station instanceof MenuStation<?> menuStation) {
                menuStation.renderScene(pose, buffers, camera, light);
            }
            if (Scene.phase == Scene.Phase.ACTIVE) {
                station.renderWorld(pose, buffers, cam, light);
                outlineNeighbor(pose, buffers, cam, level);
            }
            if (!FreeLook.showsBody()) {
                // looking round from behind, the game draws the player's body as others see it instead
                station.renderBody(pose, buffers, cam, light);
                Hands.render(pose, buffers, cam, camera.yRot(), light);
            }
        }
        //? if >=26.1 {
        buffers.endBatch();
        //?} else if <26.1 {
        /*drawSubmitted();
        source.endBatch();
        *///?}
        if (Showcase.ENABLED) {
            FrameProbe.sample(camera);
        }
    }
}
