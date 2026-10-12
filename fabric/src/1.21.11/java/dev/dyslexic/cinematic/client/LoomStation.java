package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;

/**
 * The loom scene. The banner is stretched over the warp on top of the loom, the dye and a pattern item lie at the
 * front corners. Once banner and dye are in, every pattern the loom can weave hangs beside it on a glass board,
 * each drawn as the banner would come out, in the chosen dye. Hovering one shows it on the cloth; touching it
 * weaves it: a shuttle of the dye flies from hand to hand across the cloth, row by row from the far end, and the
 * new pattern grows behind it; then the banner comes off the loom into the hand. Underneath is the vanilla loom
 * menu: the board is its pattern buttons, the banner its result slot.
 */
public final class LoomStation extends MenuStation<LoomMenu> {
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    /** The cloth: the banner's 20 x 40 px flag, lying along the warp with its top at the far end. */
    static final double CLOTH_WIDTH = 0.42;
    static final double CLOTH_LENGTH = 0.84;
    /** Dye on the right front corner, a pattern item on the left one (r, d: right, towards the player). */
    static final double[][] CORNERS = {{0, 0}, {0.37, 0.34}, {-0.37, 0.34}};

    public static final double WEAVE_LENGTH = 2.4;
    static final double WEAVE_FROM = 0.2;
    static final double WEAVE_TO = 1.85;
    static final int PASSES = 6;
    /** The banner is woven: the server hands it over now. */
    static final double BEAT = 2.0;

    // the glass board, in GUI pixels
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int CELL_W = 22;
    private static final int CELL_H = 38;
    private static final int PAD = 6;
    private static final int HEADER = 13;

    public enum Target { NONE, PLACE, ITEM, PATTERN }

    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    public int hoverPattern = -1;
    private int placeSlot = -1;

    private int scrollRow;
    private double scroll;
    private double board;
    private int boardX, boardY, boardW, boardH;
    private boolean boardShown;
    private int lastHover = -1;
    @Nullable
    private Model.Simple flag;

    // the weaving, as it was when it began
    private DyeColor weaveBase = DyeColor.WHITE;
    private List<BannerPatternLayers.Layer> weaveLayers = List.of();
    @Nullable
    private BannerPatternLayers.Layer weaveNew;
    private ItemStack woven = ItemStack.EMPTY;
    private ItemStack shuttle = ItemStack.EMPTY;
    private boolean toInventory;
    private int passesDone;

    public LoomStation(BlockPos pos, Direction side) {
        super(pos, side, LoomMenu.class, 3);
        top = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    @Override
    public StationKind kind() {
        return StationKind.LOOM;
    }

    @Override
    public Vec3 center() {
        return top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.WOOL_STEP;
    }

    static Vec3 slotPoint(Vec3 top, Direction side, int slot) {
        Vec3 out = Vec3.atLowerCornerOf(side.getUnitVec3i());
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getUnitVec3i());
        return top.add(right.scale(CORNERS[slot][0])).add(out.scale(CORNERS[slot][1]));
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return slotPoint(top, side, slot).add(right().scale(denyShake()));
    }

    // ---- staging -------------------------------------------------------------------------------------

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(top.x + side.getStepX() * 0.86, player.getY(), top.z + side.getStepZ() * 0.86);
        boolean walk = Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = top.add(side().scale(0.8)).add(right().scale(0.08)).add(0, 0.72, 0);
        Vec3 look = top.add(side().scale(-0.02)).add(right().scale(0.12)).add(0, 0.05, 0).subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        return new CameraRig.Pose(eye, yaw, pitch, 0);
    }

    @Override
    public Vec3 rest(boolean right) {
        return top.add(side().scale(0.4)).add(right().scale(right ? 0.25 : -0.25)).add(0, HAND_Y, 0);
    }

    // ---- what the loom offers ------------------------------------------------------------------------

    private List<Holder<BannerPattern>> patterns() {
        LoomMenu menu = menu();
        return menu == null || slot(0).isEmpty() || slot(1).isEmpty() || full() ? List.of() : menu.getSelectablePatterns();
    }

    /** A banner takes six patterns at most. */
    private boolean full() {
        return slot(0).getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size() >= 6;
    }

    private static DyeColor baseOf(ItemStack banner) {
        return banner.getItem() instanceof BannerItem item ? item.getColor() : DyeColor.WHITE;
    }

    private DyeColor dye() {
        return slot(1).getItem() instanceof DyeItem dye ? dye.getDyeColor() : DyeColor.WHITE;
    }

    @Override
    public void scroll(double delta) {
        int rows = (patterns().size() + COLS - 1) / COLS;
        int next = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, Math.max(0, rows - ROWS));
        if (next != scrollRow) {
            scrollRow = next;
            Scene.sound(SoundEvents.UI_LOOM_SELECT_PATTERN, 0.25f, 1.5f);
        }
    }

    // ---- cursor --------------------------------------------------------------------------------------

    @Override
    public void updateCursor(boolean overPanel) {
        clearCursor();
        if (overPanel || menu() == null || moving()) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (boardShown && mouse[0] >= boardX && mouse[0] < boardX + boardW && mouse[1] >= boardY && mouse[1] < boardY + boardH) {
            int col = (int) ((mouse[0] - boardX - PAD) / CELL_W);
            int row = (int) ((mouse[1] - boardY - PAD - HEADER) / CELL_H);
            int k = (row + scrollRow) * COLS + col;
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS && mouse[1] >= boardY + PAD + HEADER && k < patterns().size()) {
                target = Target.PATTERN;
                hoverPattern = k;
            }
            // the hand lifts towards the board
            aim = top.add(right().scale(0.42)).add(side().scale(0.05)).add(0, 0.12, 0);
            return;
        }
        ItemStack carried = Hands.carried();
        if (carried.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                if (!slot(i).isEmpty() && GrindstoneStation.under(mouse, restPoint(i), i == 0 ? 0.24 : 0.09)) {
                    target = Target.ITEM;
                    hoverSlot = i;
                    aim = restPoint(i);
                    return;
                }
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(top.y);
        if (hit == null || Math.abs(hit.x - top.x) > 0.52 || Math.abs(hit.z - top.z) > 0.52) {
            return;
        }
        int nearest = 0;
        for (int i = 1; i < 3; i++) {
            if (hit.distanceToSqr(restPoint(i)) < hit.distanceToSqr(restPoint(nearest))) {
                nearest = i;
            }
        }
        target = Target.PLACE;
        placeSlot = slotFor(carried, nearest);
        aim = restPoint(placeSlot < 0 ? nearest : placeSlot);
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        target = Target.NONE;
        hoverSlot = -1;
        hoverPattern = -1;
        placeSlot = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.x - shoulder.x, 0, aim.z - shoulder.z).normalize();
        double hover = carrying ? 0.075 : 0.045;
        return aim.add(0, HAND_Y + hover - 0.06 * tap, 0).subtract(reach.scale(ITEM_REACH));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.x - shoulder.x, 0, hand.z - shoulder.z).normalize();
        return hand.add(reach.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
    }

    // ---- actions -------------------------------------------------------------------------------------

    @Override
    protected void act(int button, boolean shift) {
        switch (target) {
            case PLACE -> {
                if (placeSlot < 0) {
                    deny();
                    return;
                }
                slotClick(placeSlot, button, false);
                Scene.sound(SoundEvents.WOOL_PLACE, 0.5f, 1.1f);
            }
            case ITEM -> slotClick(hoverSlot, 0, shift);
            case PATTERN -> {
                if (button == 0) {
                    weave(hoverPattern, shift);
                }
            }
            default -> {}
        }
    }

    private void weave(int k, boolean shift) {
        LoomMenu menu = menu();
        Minecraft mc = Minecraft.getInstance();
        List<Holder<BannerPattern>> patterns = patterns();
        if (menu == null || mc.gameMode == null || mc.player == null || k < 0 || k >= patterns.size()) {
            return;
        }
        ItemStack banner = slot(0);
        weaveBase = baseOf(banner);
        weaveLayers = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
        weaveNew = new BannerPatternLayers.Layer(patterns.get(k), dye());
        shuttle = slot(1).copyWithCount(1);
        // the window does the same: pick the pattern here and tell the server
        menu.clickMenuButton(mc.player, k);
        mc.gameMode.handleInventoryButtonClick(menu.containerId, k);
        woven = slot(3).copy();
        toInventory = shift;
        passesDone = 0;
        Scene.sound(SoundEvents.UI_LOOM_SELECT_PATTERN, 0.7f, 1.0f);
        startMove(WEAVE_LENGTH, BEAT, () -> {
            LoomMenu open = menu();
            if (slot(3).isEmpty()) {
                cancelMove();
                return;
            }
            mc.gameMode.handleInventoryMouseClick(open.containerId, 3, 0, toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
        });
    }

    // ---- the weaving ---------------------------------------------------------------------------------

    /** 0..1: how much of the new pattern is woven, from the far end of the cloth. */
    private double woven(double t) {
        return Anim.window(t, WEAVE_FROM, WEAVE_TO);
    }

    /** The shuttle, -1 at the left edge of the cloth .. 1 at the right, crossing once per pass. */
    private double shuttleAcross(double t) {
        double passes = woven(t) * PASSES;
        int pass = Math.min(PASSES - 1, (int) passes);
        double f = Anim.inOutSine(passes - pass);
        return pass % 2 == 0 ? -1 + 2 * f : 1 - 2 * f;
    }

    /** Where the weaving has got to along the cloth (the far end is the flag's top). */
    private Vec3 weaveLine(double t) {
        return top.add(side().scale(-CLOTH_LENGTH / 2 + CLOTH_LENGTH * woven(t))).add(0, 0.01, 0);
    }

    @Nullable
    @Override
    public Vec3[] handsOverride() {
        double t = moveTime();
        if (t < 0) {
            return null;
        }
        double in = Anim.inOutCubic(Anim.window(t, 0, WEAVE_FROM)) * (1 - Anim.inOutCubic(Anim.window(t, WEAVE_TO - 0.05, BEAT)));
        Vec3 line = weaveLine(Math.min(t, WEAVE_TO)).add(side().scale(0.06)).add(0, HAND_Y + 0.03, 0);
        double across = shuttleAcross(t);
        // each hand reaches in a little to catch the shuttle as it arrives, then beats the row tight
        double beat = Anim.hump(Anim.window((woven(t) * PASSES) % 1.0, 0.8, 1.0)) * 0.02;
        Vec3 r = line.add(right().scale(CLOTH_WIDTH / 2 + 0.06 - 0.05 * Math.max(0, across))).add(side().scale(-beat));
        Vec3 l = line.add(right().scale(-CLOTH_WIDTH / 2 - 0.06 + 0.05 * Math.max(0, -across))).add(side().scale(-beat));
        Vec3 right = Anim.lerp(rest(true), r, in);
        Vec3 left = Anim.lerp(rest(false), l, in);
        if (t > BEAT - 0.1) {
            // the right hand takes the banner off the loom
            Vec3 grab = top.add(side().scale(0.2)).add(0, HAND_Y + 0.04, 0);
            right = Anim.lerp(Anim.lerp(right, grab, Anim.inOutCubic(Anim.window(t, BEAT - 0.1, BEAT + 0.05))),
                    rest(true).add(0, toInventory ? 0 : 0.05, 0), Anim.inOutCubic(Anim.window(t, BEAT + 0.05, WEAVE_LENGTH)));
        }
        return new Vec3[]{right, left};
    }

    @Override
    void frame() {
        super.frame();
        double t = moveTime();
        double target = boardShown ? 1 : 0;
        board = Anim.approach(board, target, target > board ? 9 : 14, Scene.frameDt);
        scroll = Anim.approach(scroll, scrollRow, 14, Scene.frameDt);
        if (t < 0) {
            return;
        }
        int passes = (int) Math.min(PASSES, woven(t) * PASSES + 0.5);
        if (passes > passesDone) {
            passesDone = passes;
            Scene.sound(SoundEvents.UI_LOOM_SELECT_PATTERN, 0.35f, 1.3f + passes % 2 * 0.15f);
            float[] c = rgb(weaveNew == null ? DyeColor.WHITE : weaveNew.color());
            Fx.motes(weaveLine(t).add(right().scale(shuttleAcross(t) * CLOTH_WIDTH / 2)), 0.05, 4, c[0], c[1], c[2], 0.12);
        }
    }

    @Override
    public void tick() {
        if (hoverPattern != lastHover && hoverPattern >= 0) {
            Scene.sound(SoundEvents.WOOL_HIT, 0.35f, 1.4f);
        }
        lastHover = hoverPattern;
    }

    static float[] rgb(DyeColor color) {
        int c = color.getTextureDiffuseColor();
        return new float[]{(c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f};
    }

    // ---- drawing in the world ------------------------------------------------------------------------

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.getPosition();
        if (outro()) {
            drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = moveTime();
        Vec3 far = side().scale(-1);
        Vec3 up = new Vec3(0, 1, 0);
        if (t >= 0) {
            drawWeaving(pose, buffers, camera, t, light);
        } else if (!slot(0).isEmpty()) {
            ItemStack banner = slot(0);
            List<BannerPatternLayers.Layer> layers = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
            BannerPatternLayers.Layer preview = null;
            if (hoverPattern >= 0 && hoverPattern < patterns().size()) {
                preview = new BannerPatternLayers.Layer(patterns().get(hoverPattern), dye());
            }
            drawCloth(pose, buffers, cam, top.add(0, 0.003, 0).add(right().scale(denyShake())), far, right(), up, CLOTH_WIDTH, CLOTH_LENGTH,
                    baseOf(banner), layers, preview, 1, light);
        }
        for (int i = 1; i < 3; i++) {
            ItemStack stack = slot(i);
            if (i == 1 && t >= 0 && t < WEAVE_TO) {
                // the dye is out, flying as the shuttle
                stack = stack.copyWithCount(Math.max(0, stack.getCount() - 1));
            }
            SmithingStation.drawLaid(pose, buffers, cam, stack, restPoint(i), side, 2, i == hoverSlot ? 1.08f : 0.95f, light);
        }
    }

    private void drawWeaving(PoseStack pose, MultiBufferSource buffers, Camera camera, double t, int light) {
        Vec3 cam = camera.getPosition();
        Vec3 far = side().scale(-1);
        Vec3 up = new Vec3(0, 1, 0);
        if (t < BEAT) {
            drawCloth(pose, buffers, cam, top.add(0, 0.003, 0), far, right(), up, CLOTH_WIDTH, CLOTH_LENGTH, weaveBase, weaveLayers, weaveNew,
                    woven(t), light);
            if (t >= WEAVE_FROM && t < WEAVE_TO) {
                // the shuttle, carrying the dye's thread across
                Vec3 line = weaveLine(t);
                Vec3 at = line.add(right().scale(shuttleAcross(t) * (CLOTH_WIDTH / 2 + 0.04))).add(0, 0.03, 0);
                ItemPlacer.drawOriented(pose, buffers, cam, shuttle, at, ItemPlacer.lying(ItemPlacer.awayYaw(side) + 90, 15), 0.13f, light);
                float[] c = rgb(weaveNew.color());
                float g = 0.35f;
                EnchantFx.billboard(pose, buffers, camera, line.add(right().scale(shuttleAcross(t) * CLOTH_WIDTH / 2)), 0.05, 0, EnchantFx.GLOW,
                        c[0] * g + 0.1f, c[1] * g + 0.1f, c[2] * g + 0.1f);
            }
            return;
        }
        // off the loom: the new banner rises, stands up and goes to the hand (or the bag)
        double k = Anim.inOutCubic(Anim.window(t, BEAT, WEAVE_LENGTH));
        Vec3 lift = top.add(0, 0.003 + 0.25 * k, 0).add(side().scale(0.15 * k));
        Vec3 destination = toInventory ? cam.add(0, 0.5, 0).add(Scene.yawForward(camera.getYRot()).scale(0.6))
                : heldItemPoint(Hands.position(true), Hands.shoulder(true, cam, camera.getYRot()));
        Vec3 center = Anim.lerp(lift, destination, Anim.inCubic(Anim.window(t, BEAT + 0.15, WEAVE_LENGTH)));
        double stand = Anim.smoother(k * 1.4);
        Vec3 vAxis = Anim.lerp(far, up, stand).normalize();
        Vec3 normal = Anim.lerp(up, side(), stand).normalize();
        double size = 1 - 0.75 * Anim.inCubic(Anim.window(t, BEAT + 0.1, WEAVE_LENGTH));
        List<BannerPatternLayers.Layer> after = woven.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
        drawCloth(pose, buffers, cam, center, vAxis, right(), normal, CLOTH_WIDTH * size, CLOTH_LENGTH * size, baseOf(woven), after, null, 1, light);
    }

    /**
     * The banner's flag as a cloth: base and layers drawn from the banner atlas like the banner renderer does, as
     * flat quads so a layer can be drawn only down to {@code reveal} of its length (the woven part).
     *
     * @param up    unit vector along the cloth towards the flag's top
     * @param right unit vector across it, towards the flag's right
     */
    static void drawCloth(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, Vec3 up, Vec3 right, Vec3 normal, double width,
            double length, DyeColor base, List<BannerPatternLayers.Layer> layers, @Nullable BannerPatternLayers.Layer extra, double reveal, int light) {
        Vec3 topLeft = center.add(up.scale(length / 2)).subtract(right.scale(width / 2));
        boolean partly = extra != null && reveal > 0.001 && reveal < 0.999;
        // translucent layers are sorted by depth and write none: a partial layer only stays on top of quads of the
        // same shape, so every layer is cut at the woven line too
        double[][] pieces = partly ? new double[][]{{0, reveal}, {reveal, 1}} : new double[][]{{0, 1}};
        // Request a consumer only when writing its layer: switching render types can close the previous builder.
        for (double[] piece : pieces) {
            layer(pose, buffers.getBuffer(RenderType.entitySolid(Sheets.BANNER_SHEET)), cam, topLeft, up, right, normal, width, length, ModelBakery.BANNER_BASE, 0xFFFFFFFF, piece, 0, light);
            layer(pose, buffers.getBuffer(RenderType.entityNoOutline(Sheets.BANNER_SHEET)), cam, topLeft, up, right, normal, width, length, Sheets.BANNER_BASE, base.getTextureDiffuseColor(), piece, 1, light);
            int i = 2;
            for (BannerPatternLayers.Layer l : layers) {
                layer(pose, buffers.getBuffer(RenderType.entityNoOutline(Sheets.BANNER_SHEET)), cam, topLeft, up, right, normal, width, length, Sheets.getBannerMaterial(l.pattern()),
                        l.color().getTextureDiffuseColor(), piece, i++, light);
            }
        }
        if (extra != null && reveal > 0.001) {
            layer(pose, buffers.getBuffer(RenderType.entityNoOutline(Sheets.BANNER_SHEET)), cam, topLeft, up, right, normal, width, length, Sheets.getBannerMaterial(extra.pattern()),
                    extra.color().getTextureDiffuseColor(), new double[]{0, Math.min(1, reveal)}, 2 + layers.size(), light);
        }
    }

    /** One layer of the flag's front, the part from {@code piece[0]} to {@code piece[1]} of its length (0 = its top). */
    private static void layer(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 topLeft, Vec3 up, Vec3 right, Vec3 normal, double width,
            double length, Material material, int argb, double[] piece, int index, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(material);
        Vec3 lift = normal.scale(0.0007 * index);
        Vec3 a = topLeft.add(lift).subtract(cam).subtract(up.scale(length * piece[0]));
        Vec3 b = a.add(right.scale(width));
        Vec3 c = b.subtract(up.scale(length * (piece[1] - piece[0])));
        Vec3 d = a.subtract(up.scale(length * (piece[1] - piece[0])));
        float u0 = sprite.getU(1 / 64f), u1 = sprite.getU(21 / 64f);
        float v0 = sprite.getV((float) ((1 + 40 * piece[0]) / 64)), v1 = sprite.getV((float) ((1 + 40 * piece[1]) / 64));
        Vec3[] p = {a, d, c, b};
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        int r = argb >> 16 & 255, g = argb >> 8 & 255, bl = argb & 255;
        for (int k = 0; k < 4; k++) {
            vc.addVertex(pose.last(), (float) p[k].x, (float) p[k].y, (float) p[k].z).setColor(r, g, bl, 255).setUv(uv[k][0], uv[k][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        if (target == Target.PLACE && aim != null) {
            float p = (float) (0.18 + 0.06 * Math.sin(Scene.clock() * 5));
            boolean ok = placeSlot >= 0;
            EnchantFx.disc(pose, buffers, camera, aim.add(0, 0.004, 0), placeSlot == 0 ? 0.24 : 0.1, 0, EnchantFx.GLOW, p, ok ? p * 0.8f : p * 0.2f,
                    ok ? p * 0.6f : p * 0.2f);
        }
    }

    // ---- the glass board of patterns -----------------------------------------------------------------

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        List<Holder<BannerPattern>> patterns = patterns();
        boardShown = !patterns.isEmpty() && !moving() && Scene.phase == Scene.Phase.ACTIVE;
        if (board < 0.02 || patterns.isEmpty()) {
            return;
        }
        int rows = Math.min(ROWS, (patterns.size() + COLS - 1) / COLS);
        boardW = PAD * 2 + COLS * CELL_W;
        boardH = PAD * 2 + HEADER + rows * CELL_H;
        double[] anchor = Picking.toGui(top.add(right().scale(0.58)).add(0, 0.28, 0));
        int width = g.guiWidth(), height = g.guiHeight();
        double ax = anchor == null ? width * 0.7 : anchor[0];
        double ay = anchor == null ? height * 0.45 : anchor[1];
        float appear = (float) Anim.smoother(board) * alpha;
        boardX = (int) Mth.clamp(ax + 6 + (1 - board) * 18, 8, width - boardW - 8);
        boardY = (int) Mth.clamp(ay - boardH / 2.0, height * 0.14, height * 0.86 - boardH);
        g.fill(boardX, boardY, boardX + boardW, boardY + boardH, argb(0.5f * appear, 12, 12, 16));
        g.fill(boardX, boardY + boardH - 1, boardX + boardW, boardY + boardH, argb(0.22f * appear, 255, 255, 255));
        g.fill(boardX, boardY, boardX + 1, boardY + boardH - 1, argb(0.08f * appear, 255, 255, 255));
        g.fill(boardX + boardW - 1, boardY, boardX + boardW, boardY + boardH - 1, argb(0.08f * appear, 255, 255, 255));
        Component title = Component.translatable("cinematic.loom.board");
        g.drawString(font, title, boardX + PAD, boardY + PAD - 1, argb(0.75f * appear, 242, 234, 216), false);
        int total = (patterns.size() + COLS - 1) / COLS;
        if (total > ROWS) {
            String page = (scrollRow + 1) + "–" + Math.min(total, scrollRow + ROWS) + " / " + total;
            g.drawString(font, page, boardX + boardW - PAD - font.width(page), boardY + PAD - 1, argb(0.45f * appear, 242, 234, 216), false);
        }
        if (appear < 0.3f) {
            return;
        }
        ItemStack banner = slot(0);
        DyeColor base = baseOf(banner);
        List<BannerPatternLayers.Layer> layers = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
        DyeColor dye = dye();
        LoomMenu menu = menu();
        int selected = menu == null ? -1 : menu.getSelectedBannerPatternIndex();
        int clipTop = boardY + PAD + HEADER - 1, clipBottom = boardY + boardH - PAD + 1;
        g.enableScissor(boardX, clipTop, boardX + boardW, clipBottom);
        List<LoomFlags.Flag> flags = new ArrayList<>();
        double offset = (scroll - scrollRow) * CELL_H;
        for (int row = -1; row <= rows; row++) {
            for (int col = 0; col < COLS; col++) {
                int k = (row + scrollRow) * COLS + col;
                if (k < 0 || k >= patterns.size()) {
                    continue;
                }
                int x = boardX + PAD + col * CELL_W;
                int y = (int) (boardY + PAD + HEADER + row * CELL_H - offset);
                boolean hovered = k == hoverPattern;
                g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, argb((hovered ? 0.3f : 0.1f) * appear, 255, 255, 255));
                if (k == selected) {
                    int c = argb(0.8f * appear, 255, 226, 150);
                    g.fill(x, y, x + CELL_W, y + 1, c);
                    g.fill(x, y + CELL_H - 1, x + CELL_W, y + CELL_H, c);
                }
                List<BannerPatternLayers.Layer> with = new ArrayList<>(layers);
                with.add(new BannerPatternLayers.Layer(patterns.get(k), dye));
                flags.add(new LoomFlags.Flag(x + CELL_W / 2f, y + 3 - (hovered ? 1 : 0), hovered ? 15f : 14f, base, new BannerPatternLayers(with)));
            }
        }
        drawFlags(g, flags, boardX, clipTop, boardX + boardW, clipBottom);
        g.disableScissor();
    }

    /** GUI centre of pattern {@code k}'s cell on the board as last drawn, or null when it is not on view. */
    @Nullable
    public double[] boardCell(int k) {
        int row = k / COLS - scrollRow;
        if (!boardShown || row < 0 || row >= ROWS) {
            return null;
        }
        return new double[]{boardX + PAD + (k % COLS + 0.5) * CELL_W, boardY + PAD + HEADER + (row + 0.5) * CELL_H};
    }

    /** Index of the pattern drawing the given pattern asset (e.g. "minecraft:flower"), or -1. */
    public int patternIndex(String asset) {
        List<Holder<BannerPattern>> patterns = patterns();
        for (int k = 0; k < patterns.size(); k++) {
            if (patterns.get(k).value().assetId().toString().equals(asset)) {
                return k;
            }
        }
        return -1;
    }

    /** The board's banner flags, each with its top centred where given, as one picture over the board's clip. */
    private void drawFlags(GuiGraphics g, List<LoomFlags.Flag> flags, int x0, int y0, int x1, int y1) {
        if (flags.isEmpty()) {
            return;
        }
        if (flag == null) {
            // the flag alone, hanging from its top edge, as the board shows it (no sway, no pole offset)
            ModelPart part = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG).getChild("flag");
            part.xRot = 0;
            part.y = 0;
            flag = new Model.Simple(part, RenderType::entitySolid);
        }
        g.guiRenderState.submitPicturesInPictureState(new LoomFlags.State(flags, flag, x0, y0, x1, y1, g.scissorStack.peek()));
    }

    private static int argb(float a, int r, int g, int b) {
        return Mth.clamp(Math.round(a * 255), 0, 255) << 24 | r << 16 | g << 8 | b;
    }

    // ---- what others see -----------------------------------------------------------------------------

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        Vec3 top = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        Vec3 far = Vec3.atLowerCornerOf(side.getOpposite().getUnitVec3i());
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getUnitVec3i());
        if (!items.isEmpty() && !items.get(0).isEmpty()) {
            ItemStack banner = items.get(0);
            drawCloth(pose, buffers, cam, top.add(0, 0.003, 0), far, right, new Vec3(0, 1, 0), CLOTH_WIDTH, CLOTH_LENGTH, baseOf(banner),
                    banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers(), null, 1, light);
        }
        for (int i = 1; i < Math.min(3, items.size()); i++) {
            SmithingStation.drawLaid(pose, buffers, cam, items.get(i), slotPoint(top, side, i), side, 2, 0.95f, light);
        }
    }

    // ---- words ---------------------------------------------------------------------------------------

    @Nullable
    @Override
    public List<Component> tooltip(ItemStack carried) {
        Minecraft mc = Minecraft.getInstance();
        switch (target) {
            case PATTERN -> {
                List<Component> lines = new ArrayList<>();
                lines.add(new BannerPatternLayers.Layer(patterns().get(hoverPattern), dye()).description().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("cinematic.loom.weave").withStyle(ChatFormatting.GOLD));
                return lines;
            }
            case ITEM -> {
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(mc, slot(hoverSlot)));
                if (hoverSlot == 0 && full()) {
                    lines.add(Component.translatable("cinematic.loom.full").withStyle(ChatFormatting.RED));
                } else if (hoverSlot == 0 && slot(1).isEmpty()) {
                    lines.add(Component.translatable("cinematic.loom.need_dye").withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable("cinematic.loom.take").withStyle(ChatFormatting.GRAY));
                return lines;
            }
            case PLACE -> {
                return placeSlot < 0 ? List.of(Component.translatable("cinematic.loom.refuse").withStyle(ChatFormatting.RED)) : null;
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic.hint.loom");
    }
}
