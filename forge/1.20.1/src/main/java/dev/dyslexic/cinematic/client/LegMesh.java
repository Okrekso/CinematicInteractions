package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player's own legs, seen from their eyes while sitting on the ground: each leg is the vanilla leg cut in
 * two at the knee (thigh and shin, 6 px each, textured from the skin like the vanilla leg), so a knee can be
 * drawn up. Drawn at the arms' scale so legs and arms match in the frame.
 */
public final class LegMesh {
    private final ModelPart.Cube thigh;
    private final ModelPart.Cube shin;
    private final float kneeOverlap;

    private static final float HALF = 6f;
    private static final float KNEE_OVERLAP = 2f;
    private static final float INSET = 0.02f;

    private LegMesh(int u, int v, float grow) {
        this(u, v, grow, 64f, KNEE_OVERLAP);
    }

    LegMesh(int u, int v, float grow, float textureHeight, float overlap) {
        this.kneeOverlap = overlap;
        EnumSet<Direction> closed = EnumSet.allOf(Direction.class);
        thigh = new ModelPart.Cube(u, v, -2f, 0f, -2f, 4f, HALF, 4f, grow, grow, grow, false, 64f, textureHeight, closed);
        // sampled HALF px further down the skin: the lower half of the leg's sides
        shin = new ModelPart.Cube(u, v + (int) HALF, -2f, 0f, -2f, 4f, HALF, 4f, grow - INSET, grow, grow - INSET,
                false, 64f, textureHeight, closed);
    }

    private static final LegMesh RIGHT = new LegMesh(0, 16, 0f);
    private static final LegMesh RIGHT_PANTS = new LegMesh(0, 32, 0.25f);
    private static final LegMesh LEFT = new LegMesh(16, 48, 0f);
    private static final LegMesh LEFT_PANTS = new LegMesh(0, 48, 0.25f);

    /** Draws one leg from hip through knee to foot, in world space. */
    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, LocalPlayer player, boolean right, Vec3 hip, Vec3 knee, Vec3 foot,
            Vec3 outward, int light) {
        ResourceLocation texture = player.getSkinTextureLocation();
        LegMesh leg = right ? RIGHT : LEFT;
        LegMesh pants = right ? RIGHT_PANTS : LEFT_PANTS;
        boolean pantsShown = player.isModelPartShown(right ? PlayerModelPart.RIGHT_PANTS_LEG : PlayerModelPart.LEFT_PANTS_LEG);
        Vec3 along = knee.subtract(hip);
        Vec3 past = along.lengthSqr() < 1e-8 ? knee : knee.add(along.normalize().scale(KNEE_OVERLAP / 16 * Hands.ARM_SCALE));
        segment(pose, buffers.getBuffer(RenderType.entitySolid(texture)), camera, leg.thigh, hip, past, outward, light);
        segment(pose, buffers.getBuffer(RenderType.entitySolid(texture)), camera, leg.shin, knee, foot, outward, light);
        if (pantsShown) {
            segment(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), camera, pants.thigh, hip, past, outward, light);
            segment(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), camera, pants.shin, knee, foot, outward, light);
        }
        CampfireArmor.drawInScene(pose, buffers, camera, player, right, hip, knee, foot, outward, light);
    }

    /** Draws the raised leg in two closed halves on the third-person player model. */
    static void drawOnModel(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, boolean right,
            float knee, int light, int overlay) {
        ResourceLocation texture = player.getSkinTextureLocation();
        LegMesh leg = right ? RIGHT : LEFT;
        LegMesh pants = right ? RIGHT_PANTS : LEFT_PANTS;
        boolean pantsShown = player.isModelPartShown(right ? PlayerModelPart.RIGHT_PANTS_LEG : PlayerModelPart.LEFT_PANTS_LEG);
        pose.pushPose();
        pose.pushPose();
        pose.scale(1f, (HALF + KNEE_OVERLAP) / HALF, 1f);
        leg.thigh.compile(pose.last(), buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay, 1f, 1f, 1f, 1f);
        if (pantsShown) pants.thigh.compile(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay, 1f, 1f, 1f, 1f);
        pose.popPose();
        pose.translate(0, HALF / 16, 0);
        pose.mulPose(Axis.XP.rotation(knee));
        leg.shin.compile(pose.last(), buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay, 1f, 1f, 1f, 1f);
        if (pantsShown) pants.shin.compile(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay, 1f, 1f, 1f, 1f);
        pose.popPose();
    }

    void drawBentOnModel(PoseStack pose, VertexConsumer consumer, float knee, int light, float red, float green, float blue) {
        pose.pushPose();
        pose.pushPose();
        pose.scale(1f, (HALF + kneeOverlap) / HALF, 1f);
        thigh.compile(pose.last(), consumer, light, OverlayTexture.NO_OVERLAY, red, green, blue, 1f);
        pose.popPose();
        pose.translate(0, HALF / 16, 0);
        pose.mulPose(Axis.XP.rotation(knee));
        shin.compile(pose.last(), consumer, light, OverlayTexture.NO_OVERLAY, red, green, blue, 1f);
        pose.popPose();
    }

    void drawShinOnModel(PoseStack pose, VertexConsumer consumer, float knee, int light, float red, float green, float blue) {
        pose.pushPose();
        pose.translate(0, HALF / 16, 0);
        pose.mulPose(Axis.XP.rotation(knee));
        shin.compile(pose.last(), consumer, light, OverlayTexture.NO_OVERLAY, red, green, blue, 1f);
        pose.popPose();
    }

    void drawBentInScene(PoseStack pose, VertexConsumer consumer, Vec3 camera, Vec3 hip, Vec3 knee, Vec3 foot,
            Vec3 outward, int light, float red, float green, float blue) {
        Vec3 along = knee.subtract(hip);
        Vec3 past = along.lengthSqr() < 1e-8 ? knee : knee.add(along.normalize().scale(kneeOverlap / 16 * Hands.ARM_SCALE));
        segment(pose, consumer, camera, thigh, hip, past, outward, light, red, green, blue);
        segment(pose, consumer, camera, shin, knee, foot, outward, light, red, green, blue);
    }

    void drawShinInScene(PoseStack pose, VertexConsumer consumer, Vec3 camera, Vec3 knee, Vec3 foot,
            Vec3 outward, int light, float red, float green, float blue) {
        segment(pose, consumer, camera, shin, knee, foot, outward, light, red, green, blue);
    }

    /** One half of a leg stretched from {@code from} to {@code to}, its outer face turned towards {@code outward}. */
    private static void segment(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, Vec3 camera, ModelPart.Cube cube, Vec3 from,
            Vec3 to, Vec3 outward, int light) {
        segment(pose, buffer, camera, cube, from, to, outward, light, 1f, 1f, 1f);
    }

    private static void segment(PoseStack pose, VertexConsumer buffer, Vec3 camera, ModelPart.Cube cube, Vec3 from,
            Vec3 to, Vec3 outward, int light, float red, float green, float blue) {
        Vec3 d = to.subtract(from);
        double length = d.length();
        if (length < 1e-4) {
            return;
        }
        d = d.scale(1 / length);
        Vec3 x = outward.subtract(d.scale(outward.dot(d)));
        if (x.lengthSqr() < 1e-6) {
            x = new Vec3(0, 1, 0).cross(d);
        }
        x = x.normalize();
        Vec3 z = x.cross(d);
        Quaternionf rotation = new Matrix3f(
                new Vector3f((float) x.x, (float) x.y, (float) x.z),
                new Vector3f((float) d.x, (float) d.y, (float) d.z),
                new Vector3f((float) z.x, (float) z.y, (float) z.z)).getNormalizedRotation(new Quaternionf());
        float scale = Hands.ARM_SCALE;
        float stretch = (float) (length * 16 / scale / HALF);
        pose.pushPose();
        pose.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
        pose.mulPose(rotation);
        pose.scale(scale, scale * stretch, scale);
        cube.compile(pose.last(), buffer, light, OverlayTexture.NO_OVERLAY, red, green, blue, 1f);
        pose.popPose();
    }
}
