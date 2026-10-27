package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.Cinematic;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The light and particle work of the enchanting scene: rune circles, sigils, the shaft of light, glyph streams. */
public final class EnchantFx {
    private EnchantFx() {}

    public static final ResourceLocation CIRCLE = Cinematic.id("textures/fx/magic_circle.png");
    public static final ResourceLocation PIXEL_CIRCLE = Cinematic.id("textures/fx/enchant_circle_pixel.png");
    public static final ResourceLocation GLOW = Cinematic.id("textures/fx/glow.png");
    public static final ResourceLocation BEAM = Cinematic.id("textures/fx/beam.png");
    /** Surface sigils test against the scene depth, but do not claim depth for their transparent square. */
    private static final RenderType SURFACE_CIRCLE = noDepthWrite(RenderType.entityTranslucent(PIXEL_CIRCLE, false));

    private static RenderType noDepthWrite(RenderType original) {
        return new RenderType("cinematic_enchant_surface", original.format(), original.mode(), original.bufferSize(), false, true, () -> {
            original.setupRenderState();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        }, () -> {
            original.clearRenderState();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }) {};
    }

    public static final double LAPIS_RADIUS = 0.115;
    // Raising the right-edge rune clears the open book without putting it behind the resting hand.
    private static final double LAPIS_RIGHT = 0.44;
    private static final double LAPIS_NEAR = 0.18;
    private static final double LAPIS_LIFT = 0.27;

    private static final RandomSource RANDOM = RandomSource.create();

    /** Height of the enchanting table's top. */
    public static Vec3 top(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5);
    }

    /** Where the item hovers: high over the book. */
    public static Vec3 itemCenter(BlockPos pos, Direction side, double clock) {
        return top(pos).add(0, 0.62 + Math.sin(clock * 1.6) * 0.025, 0);
    }

    /** The small floating socket beside the book, as seen by a player on {@code side}. */
    public static Vec3 lapisCorner(BlockPos pos, Direction side) {
        Vec3 out = Vec3.atLowerCornerOf(side.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getNormal());
        return top(pos).add(out.scale(LAPIS_NEAR)).add(right.scale(LAPIS_RIGHT)).add(0, LAPIS_LIFT, 0);
    }

    // ---- quads -------------------------------------------------------------------------------------

    /** A textured, additive quad given by four camera-relative corners (wound both ways). */
    /** Texture corners of a quad, and its corners wound both ways (seen from either side). */
    private static final float[][] QUAD_UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final int[][] BOTH_WAYS = {{0, 1, 2, 3}, {3, 2, 1, 0}};
    private static final int[][] SURFACE_SIDE = {{3, 2, 1, 0}};

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] c, float r, float g, float b, Vec3 normal) {
        int ri = clamp(r), gi = clamp(g), bi = clamp(b);
        for (int[] order : BOTH_WAYS) {
            for (int i : order) {
                vc.addVertex(pose, (float) c[i].x, (float) c[i].y, (float) c[i].z).setColor(ri, gi, bi, 255).setUv(QUAD_UV[i][0], QUAD_UV[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            }
        }
    }

    /**
     * Heights of the circles over the table top (blocks). A few thousandths left the depth test to chance, and with
     * shaders (their own depth buffer, the camera jittered a fraction of a pixel every frame for anti-aliasing) the top and
     * the circles, and the two circles, took turns in front: they flickered. These keep a clear step between all of them.
     */
    public static final double OUTER_LIFT = 0.012, INNER_LIFT = 0.02, HINT_LIFT = 0.028, SOCKET_LIFT = 0.016;

    /** The brightness at which a pixel circle is fully there: below it the circle fades by its alpha. */
    private static final float FULL_PRESENCE = 0.4f;

    /**
     * The neutral pixel mask stays white; its two vertex colours supply the palette as the quad rotates. Brightness is
     * presence, not a darker colour: scaled RGB under a solid alpha drew an opaque black ring while a circle faded in and
     * a dull one at rest. Above full presence the colour stays the palette's own (heating it towards white turned the
     * violet a pale pink). Every circle is lit as an upward face: the entity shader's diffuse light dims a face turned
     * away from its two lights to 0.4-0.75, which greyed the camera-facing sigils.
     */
    private static void gradientQuad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] corners, CirclePalette palette, float brightness,
            boolean surface) {
        float presence = Math.max(0, Math.min(1, brightness / FULL_PRESENCE));
        int alpha = clamp(presence * presence * (3 - 2 * presence));
        if (alpha == 0) {
            // nothing to see, and a translucent quad would still write depth
            return;
        }
        for (int[] order : surface ? SURFACE_SIDE : BOTH_WAYS) {
            for (int i : order) {
                int rgb = i < 2 ? palette.top : palette.bottom;
                vc.addVertex(pose, (float) corners[i].x, (float) corners[i].y, (float) corners[i].z)
                        .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha)
                        .setUv(QUAD_UV[i][0], QUAD_UV[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0, 1, 0);
            }
        }
    }

    public enum CirclePalette {
        // a short step from deep to bright violet: lit at full strength, a lavender end read as pale pink
        VIOLET(0xAB00F9, 0xC566FF),
        RED(0xA3303B, 0xCF5354),
        LAPIS(0x3B78D9, 0x9CC8FF);

        final int top, bottom;

        CirclePalette(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255)));
    }

    /** The pixel rune circle lying on a surface, with a gradient that turns together with its glyphs. */
    public static void circle(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, double radius, double angle,
            CirclePalette palette, float brightness) {
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = angle + Math.PI / 4 + i * Math.PI / 2;
            double d = radius * Math.sqrt(2);
            corners[i] = new Vec3(center.x + Math.cos(a) * d - cam.x, center.y - cam.y, center.z + Math.sin(a) * d - cam.z);
        }
        gradientQuad(buffers.getBuffer(SURFACE_CIRCLE), pose.last(), corners, palette, brightness, true);
    }

    /** The same pixel rune facing the camera, used by the floating enchantment offers. */
    public static void circleBillboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius,
            double angle, CirclePalette palette, float brightness) {
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 cam = camera.getPosition();
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = angle + Math.PI / 4 + i * Math.PI / 2;
            double x = Math.cos(a) * radius * Math.sqrt(2);
            double y = Math.sin(a) * radius * Math.sqrt(2);
            corners[i] = new Vec3(center.x + left.x() * x + up.x() * y - cam.x,
                    center.y + left.y() * x + up.y() * y - cam.y, center.z + left.z() * x + up.z() * y - cam.z);
        }
        gradientQuad(buffers.getBuffer(RenderType.entityTranslucent(PIXEL_CIRCLE, false)), pose.last(), corners, palette, brightness, false);
    }

    /** A texture lying flat on a surface as added light. */
    public static void disc(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, double radius, double angle,
            ResourceLocation texture, float r, float g, float b) {
        Vec3[] c = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = angle + Math.PI / 4 + i * Math.PI / 2;
            double d = radius * Math.sqrt(2);
            c[i] = new Vec3(center.x + Math.cos(a) * d - cam.x, center.y - cam.y, center.z + Math.sin(a) * d - cam.z);
        }
        quad(buffers.getBuffer(RenderType.eyes(texture)), pose.last(), c, r, g, b, new Vec3(0, 1, 0));
    }

    /** A disc facing the camera: the circle texture or the soft glow. */
    public static void billboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, double angle,
            ResourceLocation texture, float r, float g, float b) {
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 cam = camera.getPosition();
        Vec3[] c = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = angle + Math.PI / 4 + i * Math.PI / 2;
            double x = Math.cos(a) * radius * Math.sqrt(2);
            double y = Math.sin(a) * radius * Math.sqrt(2);
            c[i] = new Vec3(center.x + left.x() * x + up.x() * y - cam.x, center.y + left.y() * x + up.y() * y - cam.y,
                    center.z + left.z() * x + up.z() * y - cam.z);
        }
        Vector3f look = camera.getLookVector();
        quad(buffers.getBuffer(RenderType.eyes(texture)), pose.last(), c, r, g, b, new Vec3(-look.x(), -look.y(), -look.z()));
    }

    /** A shaft of light rising from {@code base}: two crossed planes, bright at the foot. */
    public static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, double height, double width, double spin,
            float r, float g, float b) {
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(BEAM));
        for (int plane = 0; plane < 2; plane++) {
            double a = spin + plane * Math.PI / 2;
            double dx = Math.cos(a) * width / 2;
            double dz = Math.sin(a) * width / 2;
            Vec3[] c = {
                    new Vec3(base.x - dx - cam.x, base.y + height - cam.y, base.z - dz - cam.z),
                    new Vec3(base.x + dx - cam.x, base.y + height - cam.y, base.z + dz - cam.z),
                    new Vec3(base.x + dx - cam.x, base.y - cam.y, base.z + dz - cam.z),
                    new Vec3(base.x - dx - cam.x, base.y - cam.y, base.z - dz - cam.z)};
            quad(vc, pose.last(), c, r, g, b, new Vec3(Math.sin(a), 0, -Math.cos(a)));
        }
    }

    // ---- particles ---------------------------------------------------------------------------------

    /** Glyphs flying from {@code from} into {@code to} (the vanilla enchanting particle converges on its spawn point). */
    public static void stream(Vec3 from, Vec3 to, int count) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double jx = (RANDOM.nextDouble() - 0.5) * 0.5;
            double jy = (RANDOM.nextDouble() - 0.5) * 0.4;
            double jz = (RANDOM.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.ENCHANT, to.x, to.y, to.z, from.x - to.x + jx, from.y - to.y + jy, from.z - to.z + jz);
        }
    }

    // ---- sparks: soft glowing motes ({@link Fx}) in the violet and blue of the enchanting table ---------

    private static final float[][] PALETTE = {{0.75f, 0.45f, 1.0f}, {0.45f, 0.8f, 1.0f}, {1.0f, 0.92f, 1.0f}};

    /** A mote drifting slowly upwards. */
    public static void spark(Vec3 at, Vec3 velocity, double life, float size) {
        float[] c = PALETTE[RANDOM.nextInt(PALETTE.length)];
        Fx.spark(at, velocity, life, size, c[0], c[1], c[2], -0.04, 0);
    }

    /** The moment of enchantment: motes flung out, glyphs pulled in, a little purple haze. */
    public static void burst(Vec3 at, double strength) {
        for (int i = 0; i < (int) (70 * strength); i++) {
            Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.7 + 0.25, RANDOM.nextGaussian()).normalize();
            spark(at, dir.scale(0.35 + RANDOM.nextDouble() * 1.0), 0.6 + RANDOM.nextDouble() * 0.8, 0.018f + RANDOM.nextFloat() * 0.03f);
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (int i = 0; i < (int) (8 * strength); i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, at.x + (RANDOM.nextDouble() - 0.5) * 0.3, at.y + (RANDOM.nextDouble() - 0.5) * 0.3,
                    at.z + (RANDOM.nextDouble() - 0.5) * 0.3, (RANDOM.nextDouble() - 0.5) * 0.05, RANDOM.nextDouble() * 0.04,
                    (RANDOM.nextDouble() - 0.5) * 0.05);
        }
        for (int i = 0; i < (int) (30 * strength); i++) {
            Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().scale(1.1 + RANDOM.nextDouble() * 0.6);
            level.addParticle(ParticleTypes.ENCHANT, at.x, at.y, at.z, dir.x, dir.y, dir.z);
        }
    }

    /** A mote drifting up close around a point. */
    public static void sparkle(Vec3 at, double radius) {
        double a = RANDOM.nextDouble() * Math.PI * 2;
        double r = radius * (0.6 + RANDOM.nextDouble() * 0.4);
        spark(at.add(Math.cos(a) * r, (RANDOM.nextDouble() - 0.5) * 0.2, Math.sin(a) * r), new Vec3(0, 0.05 + RANDOM.nextDouble() * 0.05, 0),
                1.0 + RANDOM.nextDouble() * 0.6, 0.012f + RANDOM.nextFloat() * 0.012f);
    }

    /** Motes drawn in from a shell around a point. */
    public static void gather(Vec3 at, double radius) {
        Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize();
        double life = 0.45 + RANDOM.nextDouble() * 0.2;
        spark(at.add(dir.scale(radius)), dir.scale(-radius / life * 1.6), life, 0.014f + RANDOM.nextFloat() * 0.012f);
    }

    /** Centres of the bookshelves that empower this table. */
    public static List<Vec3> shelves(ClientLevel level, BlockPos pos) {
        List<Vec3> list = new ArrayList<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                list.add(Vec3.atCenterOf(pos.offset(offset)));
            }
        }
        return list;
    }
}
