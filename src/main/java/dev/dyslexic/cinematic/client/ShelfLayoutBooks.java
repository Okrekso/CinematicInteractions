package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Custom resting books also remain in place after leaving the scene, without changing the real block state. */
final class ShelfLayoutBooks implements BlockEntityRenderer<ChiseledBookShelfBlockEntity> {
    private final OpenBook book = new OpenBook();
    private static final ItemStack UNKNOWN_BOOK = new ItemStack(Items.BOOK);
    private static final Set<BlockPos> VISIBLE_SHELVES = new HashSet<>();
    private static double restoringUntil = -1;
    static boolean drawsRestingBooks() { return SpatialLayouts.customShelf() || Scene.clock() < restoringUntil; }
    static void layoutChanged(boolean custom) {
        if (!custom) restoringUntil = Scene.clock() + 0.35;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // Refresh only sections with shelves, rather than rebuilding the whole world while a slider moves.
        Set<SectionPos> sections = new HashSet<>();
        for (BlockPos pos : VISIBLE_SHELVES) sections.add(SectionPos.of(pos));
        for (SectionPos section : sections) mc.levelRenderer.setSectionDirty(section.x(), section.y(), section.z());
    }
    static void unloadChunk(ChunkPos chunk) { VISIBLE_SHELVES.removeIf(pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z); }
    static void clear() { VISIBLE_SHELVES.clear(); restoringUntil = -1; }
    ShelfLayoutBooks(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ChiseledBookShelfBlockEntity shelf, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var pos = shelf.getBlockPos();
        VISIBLE_SHELVES.add(pos);
        if (!drawsRestingBooks()) return;
        ShelfStation mine = Scene.station(ShelfStation.class);
        if (mine != null && mine.pos.equals(pos)) return;
        var state = shelf.getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        for (int i = 0; i < 6; i++) {
            if (!state.getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i)) || ShelfBooks.flight(pos, i) != null) continue;
            // Vanilla syncs occupancy, but not book contents. The slot's spine is enough to draw its resting book.
            ShelfBooks.draw(pose, buffers, origin, i, UNKNOWN_BOOK, ShelfBooks.inSlot(pos, facing, i, 0), book, light);
        }
    }
}
