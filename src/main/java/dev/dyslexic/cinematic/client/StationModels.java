//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.client;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/^*
 * The brewing stand's and the chiseled bookshelf's models, asked for the state they should show without the world's block
 * state changing (NeoForge wraps the same models as BakedModelWrapper with model data). Both are single multipart models
 * that pick their parts by the state they are asked to draw: the stand is always asked for one with no bottles (the mod
 * draws the real potions, {@link BrewingBottles}), the shelf for one with the slots emptied whose books are out
 * ({@link ShelfBooks#emptyMask}). The Fabric renderer API (also Sodium's and Iris's) passes the block's position.
 ^/
public final class StationModels extends ForwardingBakedModel {
    private final boolean brewing;

    private StationModels(BakedModel model, boolean brewing) {
        wrapped = model;
        this.brewing = brewing;
    }

    public static BakedModel wrap(BakedModel model, ModelResourceLocation id) {
        if (model == null || id == null || !id.id().getNamespace().equals("minecraft")) return model;
        return switch (id.id().getPath()) {
            case "brewing_stand" -> new StationModels(model, true);
            case "chiseled_bookshelf" -> new StationModels(model, false);
            default -> model;
        };
    }

    private BlockState displayed(BlockState state, BlockPos pos) {
        if (state == null) return null;
        if (brewing) {
            // a server without the mod sends no potions to draw: the stand keeps its own bottle pictures
            return ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state;
        }
        if (state.is(Blocks.CHISELED_BOOKSHELF)) {
            int mask = ShelfBooks.emptyMask(pos);
            for (int i = 0; i < 6; i++) {
                if ((mask >> i & 1) != 0) state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
            }
        }
        return state;
    }

    /^* The quads this model hands the renderer for the block at {@code pos} (what the showcase checks). ^/
    List<BakedQuad> quadsAt(BlockState state, BlockPos pos, Direction face, RandomSource random) {
        return wrapped.getQuads(displayed(state, pos), face, random);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        // without a position (an item, a falling block): the stand without bottles, the shelf as it is
        return wrapped.getQuads(brewing ? displayed(state, BlockPos.ZERO) : state, face, random);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
        ((FabricBakedModel) wrapped).emitBlockQuads(level, displayed(state, pos), pos, random, context);
    }
}
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.client;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/^*
 * The brewing stand's and the chiseled bookshelf's models, asked for the state they should show without the world's block
 * state changing (NeoForge wraps the same models as BakedModelWrapper with model data). Both are single multipart models
 * that pick their parts by the state they are asked to draw: the stand is always asked for one with no bottles (the mod
 * draws the real potions, {@link BrewingBottles}), the shelf for one with the slots emptied whose books are out
 * ({@link ShelfBooks#emptyMask}). The Fabric renderer API (also Sodium's and Iris's) passes the block's position.
 ^/
public final class StationModels extends ForwardingBakedModel {
    private final boolean brewing;

    private StationModels(BakedModel model, boolean brewing) {
        wrapped = model;
        this.brewing = brewing;
    }

    public static BakedModel wrap(BakedModel model, net.minecraft.resources.ResourceLocation id) {
        if (model == null || id == null || !id.getNamespace().equals("minecraft")) return model;
        return switch (id.getPath()) {
            case "brewing_stand" -> new StationModels(model, true);
            case "chiseled_bookshelf" -> new StationModels(model, false);
            default -> model;
        };
    }

    private BlockState displayed(BlockState state, BlockPos pos) {
        if (state == null) return null;
        if (brewing) {
            // a server without the mod sends no potions to draw: the stand keeps its own bottle pictures
            return ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state;
        }
        if (state.is(Blocks.CHISELED_BOOKSHELF)) {
            int mask = ShelfBooks.emptyMask(pos);
            for (int i = 0; i < 6; i++) {
                if ((mask >> i & 1) != 0) state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
            }
        }
        return state;
    }

    /^* The quads this model hands the renderer for the block at {@code pos} (what the showcase checks). ^/
    List<BakedQuad> quadsAt(BlockState state, BlockPos pos, Direction face, RandomSource random) {
        return wrapped.getQuads(displayed(state, pos), face, random);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        // without a position (an item, a falling block): the stand without bottles, the shelf as it is
        return wrapped.getQuads(brewing ? displayed(state, BlockPos.ZERO) : state, face, random);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
        ((FabricBakedModel) wrapped).emitBlockQuads(level, displayed(state, pos), pos, random, context);
    }
}
*///?}
