package dev.dyslexic.cinematic.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the baked geometry for each state, selecting the visible state at render time. */
public final class StationModels extends WrapperBlockStateModel {
    private static final Map<BlockState, BlockStateModel> MODELS = new ConcurrentHashMap<>();
    private final BlockState state;

    private StationModels(BlockStateModel model, BlockState state) { super(model); this.state = state; }

    public static BlockStateModel wrap(BlockStateModel model, BlockState state) {
        if (!state.is(Blocks.BREWING_STAND) && !state.is(Blocks.CHISELED_BOOKSHELF)) return model;
        MODELS.put(state, model);
        return new StationModels(model, state);
    }

    private BlockState displayed(BlockState state, BlockPos pos) {
        if (state.is(Blocks.BREWING_STAND)) {
            for (var property : BrewingStandBlock.HAS_BOTTLE) state = state.setValue(property, false);
        } else {
            int mask = ShelfBooks.emptyMask(pos);
            for (int i = 0; i < 6; i++) if ((mask & 1 << i) != 0)
                state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
        }
        return state;
    }

    @Override public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        MODELS.getOrDefault(displayed(state, BlockPos.ZERO), wrapped).collectParts(random, parts);
    }

    @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        BlockState visible = displayed(state, pos);
        MODELS.getOrDefault(visible, wrapped).emitQuads(emitter, level, pos, visible, random, cullTest);
    }

    @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        // Books can be temporarily removed by the reading animation without changing the world's block state.
        return null;
    }
}
