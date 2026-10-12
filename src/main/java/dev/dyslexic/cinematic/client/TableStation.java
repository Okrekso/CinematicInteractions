package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableCrafting;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/**
 * The crafting table scene: lean over the top, lay items into the painted 3x3 grid, click the result that
 * floats over the far edge to craft (the hands press the ingredients together while the server crafts).
 */
public final class TableStation extends ClientStation {
    static final double HAND_Y = 0.062;
    static final double REST_BACK = 0.4;
    static final double REST_SIDE = 0.24;
    /** How far in front of the fist a held item hangs. */
    static final double ITEM_REACH = 0.075;

    public static final double LEAN_BACK = 0.86;
    public static final double LEAN_UP = 0.72;
    /** The point looked at, measured from the top centre towards the player (negative: beyond the centre). */
    public static final double LOOK_BACK = -0.04;

    public static final double CRAFT_LENGTH = 0.5;
    public static final double CRAFT_MID = 0.2;
    private static final float RESULT_SIZE = 0.2f;
    private static final float OCCLUDED_OPACITY = 0.28f;
    private static final double FADE_RATE = 14;

    public final Vec3 top;
    final TableRecipeBook recipeBook;
    public int hoverCell = -1;
    public boolean hoverResult;

    public ItemStack result = ItemStack.EMPTY;
    public double resultChangedAt = -10;
    private int resultVersion = -1;

    /** The ingredients as they lay when crafting was clicked, gathered while the server crafts. */
    public double craftAt = -10;
    @Nullable
    public ItemStack[] craftItems;
    @Nullable
    public byte[] craftFacing;

    private boolean painting;
    private final boolean[] painted = new boolean[TableGrid.CELLS];
    private final float[] itemOpacity = new float[TableGrid.CELLS];

    public TableStation(BlockPos pos, Direction side) {
        super(pos, side);
        top = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        Arrays.fill(itemOpacity, 1f);
        recipeBook = new TableRecipeBook(this);
    }

    @Override
    public StationKind kind() {
        return StationKind.TABLE;
    }

    @Override
    public Vec3 center() {
        return top;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(top.x + side.getStepX() * 0.86, player.getY(), top.z + side.getStepZ() * 0.86);
        boolean walk = Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean busy() {
        return craftProgress() >= 0 || laying != null;
    }

    // ---- laying out a recipe -------------------------------------------------------------------------

    /** Seconds one item takes: from the side, over the cell, down onto it, and the hand lifts. */
    static final double LAY_MOVE = 0.36;
    /** When in a move the item is let go of (the server is asked to put it down). */
    private static final double LAY_DROP = 0.64;

    /** The moves still to make, {world cell, inventory slot or -1}; the one under way is {@link #laying}. */
    private final java.util.ArrayDeque<int[]> layQueue = new java.util.ArrayDeque<>();
    @Nullable
    private int[] laying;
    private ItemStack layItem = ItemStack.EMPTY;
    private double layAt;
    private boolean layDropped;

    /** Lays out the selected recipe from the inventory, one item after another. */
    void layOut(List<int[]> moves) {
        if (busy() || moves.isEmpty()) {
            return;
        }
        layQueue.addAll(moves);
        nextLay();
    }

    private void nextLay() {
        laying = layQueue.poll();
        layDropped = false;
        layAt = Scene.clock();
        Minecraft mc = Minecraft.getInstance();
        layItem = laying == null || mc.player == null ? ItemStack.EMPTY
                : (laying[1] < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(laying[1])).copyWithCount(1);
        if (laying != null && layItem.isEmpty()) {
            // the inventory changed meanwhile: skip it
            nextLay();
        }
    }

    /** 0..1 through the move under way. */
    private double layProgress() {
        return Anim.clamp01((Scene.clock() - layAt) / LAY_MOVE);
    }

    @Override
    public void beforeFrame() {
        if (laying == null || Scene.phase != Scene.Phase.ACTIVE) {
            if (Scene.phase != Scene.Phase.ACTIVE) {
                layQueue.clear();
                laying = null;
            }
            return;
        }
        double t = layProgress();
        if (!layDropped && t >= LAY_DROP) {
            layDropped = true;
            Hands.tap();
            PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.PLACE, laying[0], laying[1], false, 0));
        }
        if (t >= 1) {
            nextLay();
        }
    }

    /** The middle of a cell on the top. */
    private Vec3 cellPoint(int cell) {
        return new Vec3(pos.getX() + TableGrid.cellX(cell), top.y, pos.getZ() + TableGrid.cellZ(cell));
    }

    /**
     * The hand on the cell's side: back a little towards the player (taking the item), over the cell, down onto it, and
     * up again; the other hand rests.
     */
    @Nullable
    private Vec3[] layHands() {
        if (laying == null) {
            return null;
        }
        double t = layProgress();
        Vec3 cell = cellPoint(laying[0]);
        boolean right = lateral(cell) >= 0;
        Vec3 rest = rest(right);
        Vec3 take = rest.add(side().scale(0.14)).add(0, 0.03, 0);
        Vec3 toward = new Vec3(cell.x - rest.x, 0, cell.z - rest.z).normalize();
        Vec3 over = cell.add(0, HAND_Y + 0.07, 0).subtract(toward.scale(ITEM_REACH));
        Vec3 down = over.add(0, -0.06, 0);
        Vec3 hand;
        if (t < 0.28) {
            hand = Anim.lerp(rest, take, Anim.smoother(t / 0.28));
        } else if (t < LAY_DROP - 0.06) {
            double k = Anim.inOutCubic((t - 0.28) / (LAY_DROP - 0.06 - 0.28));
            hand = Anim.lerp(take, over, k).add(0, 0.05 * Anim.hump(k), 0);
        } else if (t < LAY_DROP + 0.06) {
            hand = Anim.lerp(over, down, Anim.hump((t - (LAY_DROP - 0.06)) / 0.12));
        } else {
            hand = Anim.lerp(over, rest, Anim.smoother((t - LAY_DROP - 0.06) / (1 - LAY_DROP - 0.06)));
        }
        Vec3 other = rest(!right);
        return right ? new Vec3[]{hand, other} : new Vec3[]{other, hand};
    }

    /** The item in the hand while it is carried over (until the table has it). */
    private void drawLayItem(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        if (laying == null || layItem.isEmpty()) {
            return;
        }
        double t = layProgress();
        if (t < 0.24 || t > LAY_DROP + 0.04) {
            return;
        }
        Vec3 cell = cellPoint(laying[0]);
        boolean right = lateral(cell) >= 0;
        Vec3 hand = Hands.position(right);
        Vec3 toward = new Vec3(cell.x - hand.x, 0, cell.z - hand.z);
        toward = toward.lengthSqr() < 1e-6 ? forward() : toward.normalize();
        Vec3 at = hand.add(toward.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
        float grow = (float) Anim.smoother((t - 0.24) / 0.08);
        ItemPlacer.renderOne(pose, buffers, camera, layItem, at, side, 91L, grow, 14f, light);
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 at = top.add(side().scale(LEAN_BACK)).add(0, LEAN_UP, 0);
        float pitch = (float) Math.toDegrees(Math.atan2(LEAN_UP, LEAN_BACK - LOOK_BACK));
        return new CameraRig.Pose(at, side.getOpposite().toYRot(), pitch, 0);
    }

    @Override
    public Vec3 rest(boolean right) {
        return top.add(side().scale(REST_BACK)).add(right().scale(right ? REST_SIDE : -REST_SIDE)).add(0, HAND_Y, 0);
    }

    // ---- cursor ----------------------------------------------------------------------------------------

    @Override
    public void updateCursor(boolean overPanel) {
        double[] mouse = Picking.mouseGui();
        overPanel |= recipeBook.over(mouse[0], mouse[1]);
        hoverResult = false;
        if (!result.isEmpty() && !overPanel) {
            double[] at = Picking.toGui(resultPosition());
            if (at != null) {
                double radius = resultRadiusGui();
                hoverResult = (mouse[0] - at[0]) * (mouse[0] - at[0]) + (mouse[1] - at[1]) * (mouse[1] - at[1]) < radius * radius;
            }
        }
        Vec3 hit = overPanel || hoverResult ? null : Picking.cursorOnPlane(top.y);
        hoverCell = -1;
        aim = null;
        if (hit == null) {
            return;
        }
        double lx = hit.x - pos.getX();
        double lz = hit.z - pos.getZ();
        if (lx > -0.02 && lx < 1.02 && lz > -0.02 && lz < 1.02) {
            hoverCell = TableGrid.cellAt(lx, lz);
            aim = new Vec3(Math.max(pos.getX() + 0.07, Math.min(pos.getX() + 0.93, hit.x)), top.y,
                    Math.max(pos.getZ() + 0.07, Math.min(pos.getZ() + 0.93, hit.z)));
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        hoverCell = -1;
        hoverResult = false;
    }

    /** The tabletop remains pickable through ingredients; only the ones blocking that view fade. */
    void updateOcclusion(Vec3 camera) {
        ClientTables.Entry entry = ClientTables.get(pos);
        Vec3 hit = Scene.phase == Scene.Phase.ACTIVE && hoverCell >= 0 && craftProgress() < 0
                ? Picking.cursorOnPlane(top.y) : null;
        for (int i = 0; i < TableGrid.CELLS; i++) {
            ItemStack stack = entry == null ? ItemStack.EMPTY : entry.items[i];
            boolean blocking = false;
            if (hit != null && i != hoverCell && !stack.isEmpty()) {
                Vec3 base = new Vec3(pos.getX() + TableGrid.cellX(i), top.y, pos.getZ() + TableGrid.cellZ(i));
                // A little hysteresis keeps the edge of a turning cube from flickering as the camera breathes.
                double margin = itemOpacity[i] < 0.99f ? 0.009 : 0.003;
                blocking = ItemPlacer.restingBounds(stack, base).inflate(margin).clip(camera, hit).isPresent();
            }
            float target = blocking ? OCCLUDED_OPACITY : 1f;
            itemOpacity[i] = stack.isEmpty() ? 1f : (float) Anim.approach(itemOpacity[i], target, FADE_RATE, Scene.frameDt);
            if (Math.abs(itemOpacity[i] - target) < 0.001f) {
                itemOpacity[i] = target;
            }
        }
    }

    public float itemOpacity(int cell) {
        return cell >= 0 && cell < itemOpacity.length ? itemOpacity[cell] : 1f;
    }

    boolean hasFadedItems() {
        for (float opacity : itemOpacity) {
            if (opacity < 0.999f) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.x - shoulder.x, 0, aim.z - shoulder.z).normalize();
        double hover = carrying ? 0.075 : 0.045;
        return new Vec3(aim.x, top.y + HAND_Y + hover - 0.06 * tap, aim.z).subtract(reach.scale(ITEM_REACH));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.x - shoulder.x, 0, hand.z - shoulder.z).normalize();
        return hand.add(reach.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
    }

    @Nullable
    @Override
    public Vec3[] handsOverride() {
        Vec3[] lay = layHands();
        if (lay != null) {
            return lay;
        }
        double craft = craftProgress();
        if (craft < 0) {
            return null;
        }
        Vec3 middle = top.add(side().scale(0.04));
        double y = HAND_Y + 0.05 - 0.07 * Anim.hump(Anim.window(craft, 0.08, 0.62));
        return new Vec3[]{middle.add(right().scale(0.085)).add(0, y, 0), middle.add(right().scale(-0.085)).add(0, y, 0)};
    }

    // ---- actions ---------------------------------------------------------------------------------------

    @Override
    public void click(int button, boolean shift) {
        if (laying != null) {
            return;
        }
        if (hoverResult && button == 0) {
            craft(shift);
            return;
        }
        if (hoverCell < 0) {
            return;
        }
        boolean carrying = !Hands.carried().isEmpty();
        send(StationActionPayload.Action.CLICK, hoverCell, button, shift);
        if (button == 1 && carrying) {
            painting = true;
            Arrays.fill(painted, false);
            painted[hoverCell] = true;
        }
    }

    /** Right-drag across cells drops one item into each. */
    @Override
    public void drag(int button) {
        if (painting && button == 1) {
            Scene.updateCursor();
            if (hoverCell >= 0 && !painted[hoverCell]) {
                painted[hoverCell] = true;
                send(StationActionPayload.Action.PAINT, hoverCell, 1, false);
            }
        }
    }

    @Override
    public void release(int button) {
        if (button == 1) {
            painting = false;
        }
    }

    private void send(StationActionPayload.Action action, int cell, int button, boolean shift) {
        Hands.tap();
        PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, action, cell, button, shift, 0));
    }

    public void craft(boolean all) {
        if (result.isEmpty()) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        if (entry != null) {
            craftItems = new ItemStack[TableGrid.CELLS];
            for (int i = 0; i < TableGrid.CELLS; i++) {
                craftItems[i] = entry.items[i].copy();
            }
            craftFacing = entry.facing.clone();
        }
        craftAt = Scene.clock();
        PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.CRAFT, 0, 0, all, 0));
    }

    /** 0..1 progress of the craft flourish, or -1 when idle. */
    public double craftProgress() {
        double t = (Scene.clock() - craftAt) / CRAFT_LENGTH;
        return t >= 0 && t <= 1 ? t : -1;
    }

    @Override
    public void tick() {
        recipeBook.tick();
        Minecraft mc = Minecraft.getInstance();
        ClientTables.Entry entry = ClientTables.get(pos);
        int version = entry == null ? -1 : entry.version;
        if (version == resultVersion) {
            return;
        }
        resultVersion = version;
        ItemStack next = entry == null || mc.level == null ? ItemStack.EMPTY
                : TableCrafting.match(mc.level, entry.list(), side).map(TableCrafting.Match::result).orElse(ItemStack.EMPTY);
        if (!ItemStack.isSameItemSameComponents(next, result) || next.getCount() != result.getCount()) {
            resultChangedAt = Scene.clock();
        }
        result = next;
    }

    // ---- drawing ---------------------------------------------------------------------------------------

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        drawResult(pose, buffers, camera, light);
        drawLayItem(pose, buffers, camera, light);
        recipeBook.renderWorld(pose, buffers, camera, light);
    }

    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        recipeBook.render(graphics, font, alpha);
    }

    void drawHighlight(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
        if (hoverCell < 0 || craftProgress() >= 0) {
            return;
        }
        double half = TableGrid.CELL / 2 - 0.006;
        double cx = pos.getX() + TableGrid.cellX(hoverCell) - cam.x;
        double cz = pos.getZ() + TableGrid.cellZ(hoverCell) - cam.z;
        double y = pos.getY() + 1.0015 - cam.y;
        // one even, steady glow: the rim blends over the fill instead of fighting it for depth
        int alpha = 47;
        Matrix4f m = pose.last().pose();
        VertexConsumer quads = buffers.getBuffer(Fx.HIGHLIGHT);
        quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz - half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz + half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz + half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz - half)).setColor(255, 244, 214, alpha);
        double width = 0.004;
        // The rim makes a partly hidden destination readable through the faded ingredient.
        highlightStrip(quads, m, cx - half, cz - half, cx + half, cz - half + width, y, 150);
        highlightStrip(quads, m, cx - half, cz + half - width, cx + half, cz + half, y, 150);
        highlightStrip(quads, m, cx - half, cz - half, cx - half + width, cz + half, y, 150);
        highlightStrip(quads, m, cx + half - width, cz - half, cx + half, cz + half, y, 150);
    }

    private static void highlightStrip(VertexConsumer quads, Matrix4f m, double x0, double z0, double x1, double z1, double y, int alpha) {
        quads.addVertex(m, (float) x0, (float) y, (float) z0).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x0, (float) y, (float) z1).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x1, (float) y, (float) z1).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x1, (float) y, (float) z0).setColor(255, 235, 185, alpha);
    }

    /** The result floats over the far edge of the table, turning slowly. */
    public Vec3 resultPosition() {
        double bob = Math.sin(Scene.clock() * 2.1) * 0.012;
        return top.add(forward().scale(0.42)).add(0, 0.17 + bob, 0);
    }

    /** Click radius of the result, in GUI pixels. */
    private double resultRadiusGui() {
        Vec3 at = resultPosition();
        double[] a = Picking.toGui(at);
        double[] b = Picking.toGui(at.add(0, RESULT_SIZE * 0.5, 0));
        if (a == null || b == null) {
            return 12;
        }
        return Math.max(10, Math.hypot(a[0] - b[0], a[1] - b[1]) * 1.25);
    }

    private void drawResult(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        if (result.isEmpty()) {
            return;
        }
        double craft = craftProgress();
        double appear = Anim.outBack(Anim.window(Scene.clock() - resultChangedAt, 0, 0.35), 1.8);
        double hover = hoverResult ? 1.14 : 1.0;
        double pop = craft >= 0 ? 1 - 0.6 * Anim.hump(Anim.window(craft, 0.15, 0.55)) : 1;
        float size = (float) (RESULT_SIZE * appear * hover * pop);
        if (size <= 0.002f) {
            return;
        }
        Vec3 at = resultPosition();
        float spin = (float) (Scene.clock() * (hoverResult ? 70 : 32) % 360);
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        boolean block = mc.getItemRenderer().getModel(result, mc.level, null, 0).isGui3d();
        float scale = block ? size * 1.6f : size;
        pose.scale(scale, scale, scale);
        int glow = LightTexture.pack(Math.max(LightTexture.block(light), 12), LightTexture.sky(light));
        mc.getItemRenderer().renderStatic(result, ItemDisplayContext.FIXED, glow, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
    }

    @Nullable
    @Override
    public List<Component> tooltip(ItemStack carried) {
        double[] mouse = Picking.mouseGui();
        List<Component> guide = recipeBook.tooltip(mouse[0], mouse[1]);
        if (guide != null || recipeBook.over(mouse[0], mouse[1])) return guide;
        if (!carried.isEmpty()) {
            return null;
        }
        if (hoverResult && !result.isEmpty()) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), result));
            lines.add(Component.translatable("cinematic.hint.result").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        if (hoverCell >= 0 && entry != null && !entry.items[hoverCell].isEmpty()) {
            return Screen.getTooltipFromItem(Minecraft.getInstance(), entry.items[hoverCell]);
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic.hint.table");
    }
}
