package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

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
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines()), box, 1.0f, 0.88f, 0.6f, 0.75f);
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // A second player can remove the station between the last client tick and this frame.
        // Do not render a scene against the replacement block's missing properties.
        boolean stationPresent = Scene.station == null || Scene.station.stillThere(level);
        if (stationPresent) Scene.updateCursor();
        Hands.update(cam, camera.getYRot());

        TableRenderer.renderAll(pose, buffers, cam, event.getFrustum(), level);
        FurnaceRenderer.renderAll(pose, buffers, cam, event.getFrustum(), level);
        EnchantRenderer.renderAll(pose, buffers, camera, event.getFrustum(), level);
        MenuRenderer.renderAll(pose, buffers, camera, event.getFrustum(), level);
        ShelfBooks.renderAll(pose, buffers, camera, event.getFrustum(), level);
        Fx.renderSparks(pose, buffers, camera, Scene.frameDt);

        ClientStation station = Scene.station;
        if (Scene.active() && station != null && stationPresent) {
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
        buffers.endBatch();
        if (Showcase.ENABLED) {
            FrameProbe.sample(camera);
        }
    }
}
