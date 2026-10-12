package dev.dyslexic.cinematic.client;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ask multipart models for the displayed state, without changing the world's actual block state. */
public final class StationModels implements BakedModel {
    private final boolean brewing;
    private final BakedModel wrapped;

    private StationModels(BakedModel model, boolean brewing) { this.wrapped = model; this.brewing = brewing; }

    public static BakedModel wrap(BakedModel model, ModelResourceLocation id) {
        if (model == null || id == null) return model;
        if (id.id().getNamespace().equals("minecraft")) {
            if (id.id().getPath().equals("brewing_stand")) return new StationModels(model, true);
            if (id.id().getPath().equals("chiseled_bookshelf")) return new StationModels(model, false);
        }
        return model;
    }

    private BlockState displayed(BlockState state, BlockPos pos) {
        if (state == null) return null;
        if (brewing && state.is(Blocks.BREWING_STAND)) {
            for (var property : BrewingStandBlock.HAS_BOTTLE) state = state.setValue(property, false);
        } else if (state.is(Blocks.CHISELED_BOOKSHELF)) {
            int mask = ShelfBooks.emptyMask(pos);
            for (int i = 0; i < 6; i++) if ((mask & 1 << i) != 0) state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
        }
        return state;
    }

    @Override public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return wrapped.isGui3d(); }
    @Override public boolean usesBlockLight() { return wrapped.usesBlockLight(); }
    @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return wrapped.getParticleIcon(); }
    @Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return wrapped.getTransforms(); }
    @Override public boolean isVanillaAdapter() { return false; }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        return wrapped.getQuads(displayed(state, BlockPos.ZERO), face, random);
    }
    @Override public void emitBlockQuads(net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter emitter, BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, java.util.function.Predicate<Direction> cullTest) {
        ((FabricBakedModel) wrapped).emitBlockQuads(emitter, level, displayed(state, pos), pos, random, cullTest);
    }
}
