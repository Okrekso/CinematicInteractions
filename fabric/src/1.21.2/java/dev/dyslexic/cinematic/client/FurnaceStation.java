package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.furnace.FurnaceZones.Face;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.Vec3;
import dev.dyslexic.cinematic.net.PacketDistributor;
import org.joml.Matrix4f;

/**
 * The furnace scenes: walk round to the front and feed the furnace by hand, following the openings painted on
 * its textures ({@link FurnaceLayout}). Furnace and smoker are worked crouching with the head a little tilted;
 * the blast furnace standing, leaning over it, since it is charged from the top.
 */
public final class FurnaceStation extends ClientStation {
    /** How the player and camera settle in front of one kind of furnace. */
    private record Staging(boolean crouch, double out, double up, double side, double lookV, float roll, float bodyPitch,
            Face restFace, double restU, double restV, double restOut) {}

    private static final Staging LOW = new Staging(true, 0.9, 0.5, 0.1, 0.44, -3.5f, 32f, Face.FRONT, 0.2, 0.09, 0.36);
    private static final Staging TALL = new Staging(false, 0.7, 1.15, 0.08, 0.97, -2f, 45f, Face.TOP, 0.15, 0.13, 0.06);

    static final double ITEM_REACH = 0.07;
    static final double HAND_Y = 0.062;

    private final StationKind kind;
    public final FurnaceLayout layout;
    private final Staging staging;

    public int hoverZone = -1;
    private Face aimFace = Face.FRONT;

    // what the vanilla menu would show; refreshed by the server every few ticks and run on locally in between
    private int litTime, litDuration, cookingProgress, cookingTotalTime;

    public FurnaceStation(BlockPos pos, Direction facing, StationKind kind) {
        super(pos, facing);
        this.kind = kind;
        this.layout = FurnaceLayout.of(kind);
        this.staging = kind == StationKind.BLAST_FURNACE ? TALL : LOW;
    }

    @Override
    public StationKind kind() {
        return kind;
    }

    private String key() {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    /** Middle of the front face. */
    @Override
    public Vec3 center() {
        return FurnaceZones.point(pos, side, 0.5, 0.5, 0);
    }

    @Override
    public BlockPos lightPos() {
        return pos.relative(side);
    }

    @Override
    public SoundEvent touchSound() {
        return kind == StationKind.BLAST_FURNACE ? SoundEvents.METAL_STEP : SoundEvents.STONE_STEP;
    }

    @Override
    public boolean stillThere(Level level) {
        return super.stillThere(level) && level.getBlockState(pos).getValue(AbstractFurnaceBlock.FACING) == side;
    }

    // ---- staging -------------------------------------------------------------------------------------

    /** Round the furnace if needed, then to the middle of its front. */
    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 middle = new Vec3(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
        Vec3 out = side();
        Vec3 across = right();
        Vec3 rel = player.position().subtract(middle);
        double ahead = rel.dot(out);
        double aside = rel.dot(across);
        List<Vec3> path = new ArrayList<>();
        if (ahead < 0.45) {
            double sign = aside >= 0 ? 1 : -1;
            if (ahead < -0.45) {
                path.add(middle.add(across.scale(sign * 1.15)));
            }
            path.add(middle.add(across.scale(sign * 1.1)).add(out.scale(1.0)));
        }
        path.add(middle.add(out.scale(0.92)));
        return path;
    }

    @Override
    public boolean crouches() {
        return staging.crouch();
    }

    @Override
    public boolean workOnTop() {
        return false;
    }

    @Override
    public float bodyPitch() {
        return staging.bodyPitch();
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 face = center();
        Vec3 eye = face.add(side().scale(staging.out())).add(0, staging.up(), 0).add(right().scale(staging.side()));
        Vec3 look = FurnaceZones.point(pos, side, 0.5, staging.lookV(), 0).subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        return new CameraRig.Pose(eye, yaw, pitch, staging.roll());
    }

    /**
     * Crouched, the hands wait low in front of the furnace; standing at the blast furnace, they rest on its top. A furnace
     * sunk below the player's floor is rested on its top too, at the front edge: waiting low in front of it the arms
     * would reach all the way down into the pit.
     */
    @Override
    public Vec3 rest(boolean right) {
        if (staging.restFace() == Face.FRONT && sunk()) {
            Vec3 at = FurnaceZones.point(pos, side, Face.TOP, right ? 1 - SUNK_REST_U : SUNK_REST_U, SUNK_REST_V, SUNK_REST_OUT);
            return at.add(0, HAND_Y - SUNK_REST_OUT, 0);
        }
        double u = right ? 1 - staging.restU() : staging.restU();
        Vec3 at = FurnaceZones.point(pos, side, staging.restFace(), u, staging.restV(), staging.restOut());
        return staging.restFace() == Face.TOP ? at.add(0, HAND_Y - staging.restOut(), 0) : at;
    }

    /** Where the hands rest on the top of a sunk furnace: near the front edge, a little in from the sides. */
    private static final double SUNK_REST_U = 0.18, SUNK_REST_V = 0.14, SUNK_REST_OUT = 0.06;

    /** Whether the furnace's top is at the player's floor or below it. */
    private boolean sunk() {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        return player != null && player.getY() >= pos.getY() + 0.8;
    }

    // ---- cursor --------------------------------------------------------------------------------------

    @Override
    public void updateCursor(boolean overPanel) {
        hoverZone = -1;
        aim = null;
        if (overPanel) {
            return;
        }
        Vec3 camera = Picking.camera();
        double bestDistance = Double.MAX_VALUE;
        Vec3 face = center();
        Vec3 hit = Picking.cursorOnPlane(face, side());
        if (hit != null) {
            double u = hit.subtract(face).dot(right()) + 0.5;
            double v = hit.y - pos.getY();
            if (u > -0.08 && u < 1.08 && v > -0.08 && v < 1.08) {
                bestDistance = hit.distanceToSqr(camera);
                pick(Face.FRONT, u, v);
            }
        }
        if (layout.usesTop()) {
            Vec3 topCenter = FurnaceZones.top(pos, side, 0.5, 0.5, 0);
            Vec3 above = Picking.cursorOnPlane(topCenter, new Vec3(0, 1, 0));
            if (above != null && above.distanceToSqr(camera) < bestDistance) {
                Vec3 rel = above.subtract(topCenter);
                double u = rel.dot(right()) + 0.5;
                double v = 0.5 - rel.dot(side());
                if (u > -0.05 && u < 1.05 && v > -0.05 && v < 1.05) {
                    pick(Face.TOP, u, v);
                }
            }
        }
    }

    private void pick(Face face, double u, double v) {
        aimFace = face;
        hoverZone = layout.zoneAt(face, u, v);
        aim = FurnaceZones.point(pos, side, face, Mth.clamp(u, 0.08, 0.92), Mth.clamp(v, 0.05, 0.95), 0);
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        hoverZone = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        if (aimFace == Face.TOP) {
            Vec3 reach = new Vec3(aim.x - shoulder.x, 0, aim.z - shoulder.z).normalize();
            double hover = carrying ? 0.075 : 0.045;
            return aim.add(0, HAND_Y + hover - 0.07 * tap, 0).subtract(reach.scale(ITEM_REACH));
        }
        Vec3 reach = aim.subtract(shoulder).normalize();
        double hover = carrying ? 0.11 : 0.075;
        // a tap pushes the fist past the face, into the opening
        return aim.add(side().scale(hover - 0.18 * tap)).subtract(reach.scale(ITEM_REACH)).add(0, 0.035, 0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        if (aimFace == Face.TOP) {
            Vec3 reach = new Vec3(hand.x - shoulder.x, 0, hand.z - shoulder.z).normalize();
            return hand.add(reach.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
        }
        Vec3 reach = hand.subtract(shoulder).normalize();
        return hand.add(reach.scale(ITEM_REACH)).add(0, -0.045, 0);
    }

    @Override
    public float heldItemTilt() {
        return 10f;
    }

    @Override
    public void click(int button, boolean shift) {
        if (hoverZone < 0) {
            return;
        }
        Hands.tap();
        PacketDistributor.sendToServer(new StationActionPayload(pos, kind, StationActionPayload.Action.CLICK, hoverZone, button, shift, 0));
    }

    /** Would the furnace take what is in hand at this zone? */
    private boolean accepts(int zone, ItemStack carried) {
        if (carried.isEmpty()) {
            return true;
        }
        return switch (zone) {
            case FurnaceZones.FUEL -> Minecraft.getInstance().level.fuelValues().isFuel(carried);
            case FurnaceZones.RESULT -> ItemStack.isSameItemSameComponents(carried, ClientFurnaces.item(pos, FurnaceZones.RESULT));
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private boolean cookable(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        RecipeType<?> type = layout.recipeType;
        return mc.level != null && mc.level.recipeAccess().propertySet(type == RecipeType.SMELTING ? net.minecraft.world.item.crafting.RecipePropertySet.FURNACE_INPUT : type == RecipeType.SMOKING ? net.minecraft.world.item.crafting.RecipePropertySet.SMOKER_INPUT : net.minecraft.world.item.crafting.RecipePropertySet.BLAST_FURNACE_INPUT).test(stack);
    }

    // ---- state ---------------------------------------------------------------------------------------

    public void applyState(FurnaceStatePayload state) {
        litTime = state.litTime();
        litDuration = state.litDuration();
        cookingProgress = state.cookingProgress();
        cookingTotalTime = state.cookingTotalTime();
    }

    @Override
    public void tick() {
        if (litTime > 0) {
            litTime--;
            if (!ClientFurnaces.item(pos, FurnaceZones.INPUT).isEmpty() && cookingTotalTime > 0) {
                cookingProgress = Math.min(cookingTotalTime - 1, cookingProgress + 1);
            }
        }
    }

    public double burn() {
        return litTime <= 0 || litDuration <= 0 ? 0 : Mth.clamp(litTime / (double) litDuration, 0, 1);
    }

    public double cook() {
        return cookingTotalTime <= 0 ? 0 : Mth.clamp(cookingProgress / (double) cookingTotalTime, 0, 1);
    }

    // ---- drawing -------------------------------------------------------------------------------------

    private static final int[][] BOTH_WAYS = {{0, 1, 2, 3}, {3, 2, 1, 0}};
    private static final int[][] ONE_WAY = {{0, 1, 2, 3}};

    /** Hover glow on the zone under the cursor, and two gauges burnt into the front: cooking and fuel. */
    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        double flicker = 0.9 + 0.1 * Math.sin(Scene.clock() * 9.0);
        gauge(pose, glow, camera, layout.cookGauge, cook(), new int[]{255, 196, 90}, flicker);
        gauge(pose, glow, camera, layout.burnGauge, burn(), new int[]{255, 92, 30}, flicker);
        FurnaceLayout.Zone zone = hoverZone < 0 ? null : layout.zone(hoverZone);
        if (zone == null) {
            return;
        }
        boolean ok = accepts(hoverZone, Hands.carried());
        // one even, steady glow (it used to pulse, and its two windings fought over depth and shimmered in stripes)
        int[] tint = ok ? new int[]{255, 236, 200} : new int[]{255, 70, 60};
        quad(pose, buffers.getBuffer(Fx.HIGHLIGHT), camera, pos, side, zone.face(), zone.highlight(), 0.004, tint, tint, 44, false);
    }

    /** A thin glowing line on the front, lit up to {@code fill}; the rest is a faint track. */
    private void gauge(PoseStack pose, VertexConsumer glow, Vec3 camera, double[] strip, double fill, int[] rgb, double flicker) {
        int[] track = {(int) (rgb[0] * 0.12), (int) (rgb[1] * 0.12), (int) (rgb[2] * 0.12)};
        quad(pose, glow, camera, pos, side, Face.FRONT, strip, 0.003, track, track, 255);
        if (fill > 0.001) {
            int[] lit = {(int) (rgb[0] * 0.75 * flicker), (int) (rgb[1] * 0.75 * flicker), (int) (rgb[2] * 0.75 * flicker)};
            quad(pose, glow, camera, pos, side, Face.FRONT, new double[]{strip[0], strip[0] + (strip[1] - strip[0]) * fill, strip[2], strip[3]},
                    0.0035, lit, lit, 255);
        }
    }

    /**
     * A flat rectangle (u0, u1, v0, v1) on a face, coloured from {@code near} at v0 to {@code far} at v1 and wound both
     * ways so it shows whatever the culling.
     */
    static void quad(PoseStack pose, VertexConsumer buffer, Vec3 camera, BlockPos pos, Direction facing, Face face, double[] rect,
            double out, int[] near, int[] far, int alpha) {
        quad(pose, buffer, camera, pos, facing, face, rect, out, near, far, alpha, true);
    }

    /** The same, wound one way only when the render type does not cull ({@code bothWays} false). */
    static void quad(PoseStack pose, VertexConsumer buffer, Vec3 camera, BlockPos pos, Direction facing, Face face, double[] rect,
            double out, int[] near, int[] far, int alpha, boolean bothWays) {
        Matrix4f m = pose.last().pose();
        Vec3[] corners = {
                FurnaceZones.point(pos, facing, face, rect[0], rect[2], out), FurnaceZones.point(pos, facing, face, rect[1], rect[2], out),
                FurnaceZones.point(pos, facing, face, rect[1], rect[3], out), FurnaceZones.point(pos, facing, face, rect[0], rect[3], out)};
        int[][] orders = bothWays ? BOTH_WAYS : ONE_WAY;
        for (int[] order : orders) {
            for (int i : order) {
                Vec3 c = corners[i];
                int[] rgb = i < 2 ? near : far;
                buffer.addVertex(m, (float) (c.x - camera.x), (float) (c.y - camera.y), (float) (c.z - camera.z))
                        .setColor(rgb[0], rgb[1], rgb[2], alpha);
            }
        }
    }

    @Nullable
    @Override
    public List<Component> tooltip(ItemStack carried) {
        if (hoverZone < 0) {
            return null;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("cinematic." + key() + ".zone." + hoverZone).withStyle(ChatFormatting.GOLD));
        if (!carried.isEmpty()) {
            if (!accepts(hoverZone, carried)) {
                lines.add(Component.translatable(hoverZone == FurnaceZones.FUEL ? "cinematic.furnace.not_fuel" : "cinematic.furnace.no_room")
                        .withStyle(ChatFormatting.RED));
            } else if (hoverZone == FurnaceZones.INPUT && !cookable(carried)) {
                lines.add(Component.translatable("cinematic." + key() + ".not_cookable").withStyle(ChatFormatting.YELLOW));
            } else {
                return null;
            }
            return lines;
        }
        ItemStack inside = ClientFurnaces.item(pos, hoverZone);
        if (inside.isEmpty()) {
            lines.add(Component.translatable("cinematic." + key() + ".empty." + hoverZone).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(inside.getHoverName().copy().append(" ×" + inside.getCount()));
        }
        if (hoverZone == FurnaceZones.INPUT && cook() > 0) {
            lines.add(Component.translatable("cinematic." + key() + ".progress", Math.round(cook() * 100)).withStyle(ChatFormatting.GRAY));
        } else if (hoverZone == FurnaceZones.FUEL && litTime > 0) {
            lines.add(Component.translatable("cinematic.furnace.burning", (litTime + 19) / 20).withStyle(ChatFormatting.GRAY));
        } else if (hoverZone == FurnaceZones.RESULT && !inside.isEmpty()) {
            lines.add(Component.translatable("cinematic.furnace.take").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic.hint." + key());
    }
}
