package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Display adapters retain vanilla block entities while applying a client's saved model alignment. */
public final class StationDisplayLayouts {
    private StationDisplayLayouts() {}
    public static boolean campfire(CampfireBlockEntity fire, PoseStack pose, MultiBufferSource buffers, int light) {
        var layout = SpatialLayouts.current(StationKind.CAMPFIRE);
        if (layout.equals(SpatialAppearance.VANILLA)) return false;
        BlockPos pos = fire.getBlockPos();
        Direction facing = fire.getBlockState().getValue(CampfireBlock.FACING);
        var frame = RemainingLayouts.frame(StationKind.CAMPFIRE, pos, facing);
        for (int i = 0; i < fire.getItems().size(); i++) {
            ItemStack food = fire.getItems().get(i);
            if (food.isEmpty()) continue;
            Direction turn = Direction.from2DDataValue((i + facing.get2DDataValue()) % 4);
            ItemPlacer.drawOriented(pose, buffers, Vec3.atLowerCornerOf(pos), food, layout.point(frame, i),
                    Axis.YP.rotationDegrees(-turn.toYRot() - (float) layout.rotation(i)).mul(Axis.XP.rotationDegrees(90)),
                    0.375f * layout.scale(i), light);
        }
        return true;
    }
    public static boolean customLectern() { return !SpatialLayouts.current(StationKind.LECTERN).equals(SpatialAppearance.VANILLA); }
    public static void lectern(LecternBlockEntity lectern, PoseStack pose) {
        BlockPos pos = lectern.getBlockPos();
        var frame = RemainingLayouts.frame(StationKind.LECTERN, pos, lectern.getBlockState().getValue(LecternBlock.FACING));
        var layout = SpatialLayouts.current(StationKind.LECTERN);
        Vec3 from = frame.base(0).subtract(Vec3.atLowerCornerOf(pos)), to = layout.point(frame, 0).subtract(Vec3.atLowerCornerOf(pos));
        Vec3 normal = frame.normal();
        pose.translate(to.x, to.y, to.z);
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(layout.rotation(0)), (float) normal.x, (float) normal.y, (float) normal.z));
        float scale = layout.scale(0); pose.scale(scale, scale, scale);
        pose.translate(-from.x, -from.y, -from.z);
    }
}
