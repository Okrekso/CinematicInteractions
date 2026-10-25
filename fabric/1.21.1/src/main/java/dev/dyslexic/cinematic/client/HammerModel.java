package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.Cinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** The owner's Blockbench hammer, baked and reloaded through Minecraft's normal model system. */
public final class HammerModel {
    private HammerModel() {}

    /** Loaded as an extra model by CinematicClient (Fabric's ModelLoadingPlugin; NeoForge: a standalone model). */
    public static final ResourceLocation LOCATION = Cinematic.id("prop/hammer");
    private static final float SCALE = 0.30f;
    private static final Vec3 GRIP = new Vec3(0.5, 0.18, 0.5);
    // The lower striking edge of the imported head, relative to the grip, at the -12 degree impact pose.
    static final double STRIKE_ALONG = (1.125 - GRIP.y) * SCALE;
    static final double STRIKE_ACROSS = 0.375 * SCALE;

    static BakedModel model() {
        return ((net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(LOCATION);
    }

    /** Seen from outside it is a little larger: the arm of the player model is straight, the hammer makes up the reach. */
    private static final float ARM_SCALE = SCALE * 1.3f;
    /**
     * In a player model's right hand, the handle runs on from the fist bent up by this much, so that with the arm swung
     * down at the anvil ({@code AnvilStation.armPitch}) the handle lies level and the head falls flat on the work.
     */
    private static final double WRIST = Math.toRadians(18);

    static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Vec3 along, Vec3 headAxis, int light) {
        pose.pushPose();
        pose.translate(grip.x - camera.x, grip.y - camera.y, grip.z - camera.z);
        draw(pose, buffers, along, headAxis, SCALE, light);
        pose.popPose();
    }

    /** In a player's right hand, as others see it: the pose stands in the frame of the model's arm, as held items are drawn. */
    static void renderInArm(PoseStack pose, MultiBufferSource buffers, double shown, int light) {
        pose.pushPose();
        // the fist, where held items sit
        pose.translate(-1 / 16f, 0.6f, -0.1f);
        draw(pose, buffers, new Vec3(0, Math.cos(WRIST), -Math.sin(WRIST)), new Vec3(0, -Math.sin(WRIST), -Math.cos(WRIST)), (float) (ARM_SCALE * shown),
                light);
        pose.popPose();
    }

    /** The model at the pose's origin, gripped there: its handle along {@code along}, its striking face away from {@code headAxis}. */
    private static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 along, Vec3 headAxis, float scale, int light) {
        Vec3 z = headAxis.cross(along);
        Quaternionf rotation = new Matrix3f(
                (float) headAxis.x, (float) headAxis.y, (float) headAxis.z,
                (float) along.x, (float) along.y, (float) along.z,
                (float) z.x, (float) z.y, (float) z.z).getNormalizedRotation(new Quaternionf());
        pose.pushPose();
        pose.mulPose(rotation);
        pose.scale(scale, scale, scale);
        pose.translate(-GRIP.x, -GRIP.y, -GRIP.z);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), null, model(),
                1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
