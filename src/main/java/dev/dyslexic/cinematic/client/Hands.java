package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player's own arms, drawn in the world with their skin. They come up from below the frame, settle where
 * the station rests them, reach for whatever the cursor points at (the hand nearer to it does the work), and
 * sink out of the frame again when the player steps away.
 */
public final class Hands {
    private Hands() {}

    static final float ARM_SCALE = 0.55f;

    private static final Anim.Spring3 RIGHT = new Anim.Spring3();
    private static final Anim.Spring3 LEFT = new Anim.Spring3();
    private static Vec3 outroRight = Vec3.ZERO, outroLeft = Vec3.ZERO;
    /** Where the hands were when moving over to another station began; null for a scene begun afresh. */
    private static Vec3 fromRight, fromLeft;
    private static double tapAt = -10;
    private static boolean leftWorks;
    private static boolean rightLanded, leftLanded;

    static void begin() {
        fromRight = fromLeft = null;
        rightLanded = leftLanded = false;
        leftWorks = false;
        tapAt = -10;
    }

    static void enterActive() {
        RIGHT.snap(Scene.station.rest(true));
        LEFT.snap(Scene.station.rest(false));
    }

    /** Moving over: the hands set off from where they are, lift a little and settle on the new station. */
    static void beginTransition() {
        fromRight = RIGHT.pos();
        fromLeft = LEFT.pos();
        rightLanded = leftLanded = false;
        leftWorks = false;
        tapAt = -10;
    }

    static void beginOutro() {
        outroRight = RIGHT.pos();
        outroLeft = LEFT.pos();
    }

    static void tap() {
        tapAt = Scene.clock();
    }

    // ---- geometry -----------------------------------------------------------------------------------

    /** Where the arm comes from: beside and below the eyes, just outside the bottom corner of the frame. */
    static Vec3 shoulder(boolean right, Vec3 camera, float cameraYaw) {
        Vec3 forward = Scene.yawForward(cameraYaw);
        Vec3 rightVec = new Vec3(-forward.z, 0, forward.x);
        return camera.add(rightVec.scale(right ? 0.38 : -0.38)).add(0, -0.46, 0).add(forward.scale(0.08));
    }

    /** Below the bottom edge of the frame. */
    static Vec3 hidden(boolean right, Vec3 camera, float cameraYaw) {
        return shoulder(right, camera, cameraYaw).add(Scene.yawForward(cameraYaw).scale(0.1)).add(0, -0.05, 0);
    }

    // ---- animation ----------------------------------------------------------------------------------

    static void update(Vec3 camera, float cameraYaw) {
        ClientStation station = Scene.station;
        if (station == null) {
            return;
        }
        double dt = Scene.frameDt;
        switch (Scene.phase) {
            case INTRO -> {
                double u = Scene.introProgress();
                boolean carried = fromRight != null && Scene.transition;
                // moving over, the hands do not wait to rise into the frame: they are in it already
                double kr = carried ? Anim.window(u, 0.08, 0.85) : Anim.window(u, 0.42, 0.93);
                double kl = carried ? Anim.window(u, 0.12, 0.9) : Anim.window(u, 0.47, 0.98);
                RIGHT.snap(arrive(station, true, kr, camera, cameraYaw));
                LEFT.snap(arrive(station, false, kl, camera, cameraYaw));
                if (!rightLanded && kr >= 0.97) {
                    rightLanded = true;
                    Scene.sound(station.touchSound(), 0.45f, 0.82f);
                }
                if (!leftLanded && kl >= 0.97) {
                    leftLanded = true;
                    Scene.sound(station.touchSound(), 0.38f, 0.74f);
                }
            }
            case ACTIVE -> follow(station, camera, cameraYaw, dt);
            case OUTRO -> {
                double u = Scene.time / Scene.OUTRO_LENGTH;
                RIGHT.snap(depart(outroRight, true, u, camera, cameraYaw));
                LEFT.snap(depart(outroLeft, false, u - 0.05, camera, cameraYaw));
            }
            default -> {}
        }
    }

    /**
     * Rises from below the frame, hovers in and settles where the station rests it; moving over from another station,
     * from where the hand was (lowered out of the frame first if the player walks over).
     */
    private static Vec3 arrive(ClientStation station, boolean right, double k, Vec3 camera, float cameraYaw) {
        Vec3 hiddenAt = hidden(right, camera, cameraYaw);
        Vec3 was = right ? fromRight : fromLeft;
        Vec3 from = was != null && Scene.transition ? Anim.lerp(was, hiddenAt, Scene.released()) : hiddenAt;
        Vec3 to = station.rest(right);
        double e = Anim.outCubic(k);
        double hover = 0.11 * Anim.hump(Math.min(1, k * 1.15)) * (1 - Anim.window(k, 0.75, 1.0));
        return Anim.lerp(from, to, e).add(0, hover, 0);
    }

    /** Lifts off, then sinks out of the frame. */
    private static Vec3 depart(Vec3 from, boolean right, double u, Vec3 camera, float cameraYaw) {
        double lift = Anim.outCubic(Anim.window(u, 0.0, 0.3)) * 0.07;
        double away = Anim.inCubic(Anim.window(u, 0.18, 0.72));
        return Anim.lerp(from.add(0, lift, 0), hidden(right, camera, cameraYaw), away);
    }

    private static void follow(ClientStation station, Vec3 camera, float cameraYaw, double dt) {
        Vec3[] special = station.handsOverride();
        if (special != null) {
            double stiffness = station.handsStiffness();
            RIGHT.update(special[0], stiffness, dt);
            LEFT.update(special[1], stiffness, dt);
            return;
        }
        Vec3 aim = station.aim;
        if (aim != null) {
            double lateral = station.lateral(aim);
            if (!leftWorks && lateral < -0.13) {
                leftWorks = true;
            } else if (leftWorks && lateral > -0.07) {
                leftWorks = false;
            }
        }
        boolean carrying = !carried().isEmpty();
        double tap = Anim.hump(Anim.window(Scene.clock() - tapAt, 0, 0.22));
        Vec3 shoulder = shoulder(!leftWorks, camera, cameraYaw);
        Vec3 working = aim != null
                ? station.handTarget(aim, shoulder, carrying, tap)
                : station.rest(!leftWorks).add(0, (carrying ? 0.05 : 0.0) - 0.02 * tap, 0);
        Vec3 reach = working.subtract(shoulder);
        if (reach.length() > station.maxReach()) {
            working = shoulder.add(reach.normalize().scale(station.maxReach()));
        }
        double idle = Scene.clock();
        Vec3 restRight = station.rest(true).add(0, 0.003 * Math.sin(idle * 1.7), 0);
        Vec3 restLeft = station.rest(false).add(0, 0.003 * Math.sin(idle * 1.5 + 2), 0);
        RIGHT.update(leftWorks ? restRight : working, leftWorks ? 12 : 17, dt);
        LEFT.update(leftWorks ? working : restLeft, leftWorks ? 17 : 12, dt);
    }

    /** Where a hand is this frame (props such as a hammer are drawn in it). */
    static Vec3 position(boolean right) {
        return (right ? RIGHT : LEFT).pos();
    }

    static ItemStack carried() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
    }

    /** Whether the held item is drawn in a hand (true) or as a cursor icon (false). */
    public static boolean itemInHand() {
        ClientStation station = Scene.station;
        return Scene.phase == Scene.Phase.ACTIVE && station != null && station.aim != null && station.handsOverride() == null;
    }

    /** The same anchor used to draw the carried world item, for screen-space inventory effects. */
    static Vec3 heldPoint() {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return Scene.station.heldItemPoint(leftWorks ? LEFT.pos() : RIGHT.pos(),
                shoulder(!leftWorks, camera.getPosition(), camera.getYRot()));
    }

    // ---- drawing ------------------------------------------------------------------------------------

    static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float cameraYaw, int light) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientStation station = Scene.station;
        if (player == null || station == null || Scene.phase == Scene.Phase.NONE) {
            return;
        }
        Vec3 rightHand = RIGHT.pos();
        Vec3 leftHand = LEFT.pos();
        drawArm(pose, buffers, camera, rightHand, shoulder(true, camera, cameraYaw), true, light, station.forward());
        drawArm(pose, buffers, camera, leftHand, shoulder(false, camera, cameraYaw), false, light, station.forward());

        ItemStack held = carried();
        if (!held.isEmpty() && itemInHand()) {
            Vec3 hand = leftWorks ? leftHand : rightHand;
            Vec3 at = station.heldItemPoint(hand, shoulder(!leftWorks, camera, cameraYaw));
            if (station instanceof TableStation) {
                TableLayouts.current().renderItem(pose, buffers, camera, held, at, station.sideDirection(), 77L, 1f,
                        station.heldItemTilt(), light, 1, true);
            } else {
                ItemPlacer.renderOne(pose, buffers, camera, held, at, station.sideDirection(), 77L,
                        station instanceof FurnaceStation furnace ? furnace.heldItemScale() : station instanceof SurfaceStation<?> surface ? surface.heldItemScale() : station instanceof SpatialTarget spatial ? spatial.layoutHeldScale() : 1f, station.heldItemTilt(), light);
            }
        }
    }

    /**
     * One of the player's own arms in the world, with their skin, from the shoulder to the hand ({@code forward} only
     * settles which way the palm turns when the arm points straight up or down).
     */
    static void drawArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerSkin skin = player.getSkin();
        drawArm(pose, buffers, camera, skin.texture(), skin.model() == PlayerSkin.Model.SLIM,
                player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE), hand, shoulder, right, light, forward);
    }

    private static void drawArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ResourceLocation texture, boolean slim,
            boolean sleeveShown, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward) {
        Vec3 d = hand.subtract(shoulder);
        double length = d.length() + 0.18;
        d = d.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 upPerp = up.subtract(d.scale(up.dot(d)));
        if (upPerp.lengthSqr() < 1e-6) {
            upPerp = forward;
        }
        upPerp = upPerp.normalize();
        // forearm turned palm-down: the inner face of each arm looks at the work
        Vec3 x = right ? upPerp.scale(-1) : upPerp;
        Vec3 z = x.cross(d);
        Quaternionf rotation = new Matrix3f(
                new Vector3f((float) x.x, (float) x.y, (float) x.z),
                new Vector3f((float) d.x, (float) d.y, (float) d.z),
                new Vector3f((float) z.x, (float) z.y, (float) z.z)).getNormalizedRotation(new Quaternionf());
        // the hand piece ends at y = 10 px; the upper piece stretches back past the shoulder point
        float stretch = (float) Math.max(0.6, (length * 16 / ARM_SCALE - ArmMesh.HAND) / ArmMesh.UPPER);
        float centerX = slim ? (right ? -0.5f : 0.5f) : (right ? -1f : 1f);
        Vec3 pivot = hand.subtract(d.scale(10.0 / 16.0 * ARM_SCALE));

        pose.pushPose();
        pose.translate(pivot.x - camera.x, pivot.y - camera.y, pivot.z - camera.z);
        pose.mulPose(rotation);
        pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        pose.translate(-centerX / 16f, 0, 0);
        ArmMesh.arm(right, slim).render(pose, buffers.getBuffer(RenderType.entitySolid(texture)), light, OverlayTexture.NO_OVERLAY, stretch);
        if (sleeveShown) {
            ArmMesh.sleeve(right, slim).render(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), light,
                    OverlayTexture.NO_OVERLAY, stretch);
        }
        pose.popPose();
    }
}
