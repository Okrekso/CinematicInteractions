package example.addon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.phys.Vec3;

/** Compile-checked example for a custom block that already opens a CraftingMenu. Not shipped in the mod JAR. */
public final class ExampleAddonScene extends AddonMenuStation<CraftingMenu> {
    private final Vec3 top;
    private int hovered = -1;

    public ExampleAddonScene(BlockPos pos, Direction side) {
        super(pos, side, CraftingMenu.class, 0);
        top = Vec3.atCenterOf(pos).add(0, 0.5, 0);
    }

    @Override public Vec3 center() { return top; }

    @Override public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = top.add(side().scale(0.9));
        return List.of(new Vec3(stand.x, player.getY(), stand.z));
    }

    @Override public CameraRig.Pose lean() {
        return new CameraRig.Pose(top.add(side().scale(0.8)).add(0, 0.7, 0),
                side.getOpposite().toYRot(), 36, 0);
    }

    @Override public Vec3 rest(boolean rightHand) {
        return top.add(side().scale(0.32)).add(right().scale(rightHand ? 0.28 : -0.28)).add(0, 0.1, 0);
    }

    private Vec3 cell(int slot) {
        int index = slot - 1;
        return top.add(right().scale((index % 3 - 1) * 0.25))
                .add(side().scale((index / 3 - 1) * 0.25)).add(0, 0.01, 0);
    }

    @Override public void updateCursor(boolean overPanel) {
        hovered = -1;
        aim = null;
        if (overPanel) return;
        Vec3 hit = Picking.cursorOnPlane(top.y + 0.02);
        if (hit == null) return;
        Vec3 offset = hit.subtract(top);
        int column = (int) Math.floor((offset.dot(right()) + 0.375) / 0.25);
        int row = (int) Math.floor((offset.dot(side()) + 0.375) / 0.25);
        if (column >= 0 && column < 3 && row >= 0 && row < 3) {
            hovered = 1 + row * 3 + column;
            aim = cell(hovered);
        }
    }

    @Override public Vec3 handTarget(Vec3 target, Vec3 shoulder, boolean carrying, double tap) {
        return target.add(0, carrying ? 0.13 : 0.08, 0);
    }

    @Override public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand.add(0, -0.07, 0);
    }

    @Override protected void act(int button, boolean shift) {
        if (hovered > 0) slotClick(hovered, button, shift);
    }

    @Override protected Vec3 restPoint(int slot) { return cell(slot + 1); }

    @Override public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        for (int slot = 1; slot <= 9; slot++) {
            ItemPlacer.render(pose, buffers, camera.getPosition(), slot(slot), cell(slot), side, slot, 1, 0, light);
        }
    }

    @Override public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {}

    @Override public Component hint() { return Component.literal("Click a square to use the real crafting menu"); }
}
