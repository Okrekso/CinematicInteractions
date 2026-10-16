package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The bottles in a brewing stand are the real potions: the stand's own bottle pictures (one generic red potion for
 * every bottle) are taken out of its model, and each potion that stands in it is drawn on its foot, turned to whoever
 * looks at it, in its own colour and shape (splash and lingering bottles too). The contents come from the stand's
 * update packet ({@code BrewingWatch}); in the player's own scene, straight from the menu.
 */
public final class BrewingBottles implements BlockEntityRenderer<BrewingStandBlockEntity> {
    /** Edge of the drawn item: the bottle in it is a little smaller than the stand's own bottle picture. */
    static final float SIZE = 0.42f;

    public BrewingBottles(BlockEntityRendererProvider.Context context) {}

    /**
     * The stand's model is one multipart model for all its states, which picks its bottle parts by the state it is asked
     * to draw. It is wrapped so that it is always asked for the state with no bottles.
     */
    public static void hideVanillaBottles(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Map<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();
        for (BlockState state : Blocks.BREWING_STAND.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = models.get(key);
            if (model != null) {
                models.put(key, wrapped.computeIfAbsent(model, NoBottles::new));
            }
        }
    }

    /** The state as the stand's model should see it: no bottle on any foot. */
    static BlockState withoutBottles(@Nullable BlockState state) {
        if (state == null || !state.is(Blocks.BREWING_STAND)) {
            return state;
        }
        for (BooleanProperty bottle : BrewingStandBlock.HAS_BOTTLE) {
            state = state.setValue(bottle, false);
        }
        return state;
    }

    private static final class NoBottles extends BakedModelWrapper<BakedModel> {
        NoBottles(BakedModel model) {
            super(model);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return originalModel.getQuads(withoutBottles(state), side, rand);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                @Nullable RenderType renderType) {
            return originalModel.getQuads(withoutBottles(state), side, rand, data, renderType);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return originalModel.getRenderTypes(withoutBottles(state), rand, data);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            return originalModel.getModelData(level, pos, withoutBottles(state), data);
        }
    }

    @Override
    public void render(BrewingStandBlockEntity stand, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockPos pos = stand.getBlockPos();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // the pose already stands at the block's corner
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        BrewingStation scene = Scene.station(BrewingStation.class);
        boolean mine = scene != null && scene.pos.equals(pos) && scene.menu() != null;
        for (int i = 0; i < BrewingStation.BOTTLES; i++) {
            ItemStack stack = mine ? scene.slot(i) : stand.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            float grow = mine && scene.hoverSlot == i ? 1.08f : 1f;
            draw(pose, buffers, origin, cam, stack, BrewingStation.slotPoint(pos, i), grow, BrewingStation.itemScale(i), BrewingStation.itemRotation(i), light);
        }
        if (!mine) {
            // the ingredient in the bowl and the blaze powder at the foot stay where they were put (the scene at this
            // stand draws its own, melting as it brews); the powder lies on the side of the foot towards the viewer
            ItemStack ingredient = stand.getItem(BrewingStation.INGREDIENT);
            ItemStack fuel = stand.getItem(BrewingStation.FUEL);
            if (!ingredient.isEmpty() || !fuel.isEmpty()) {
                pose.pushPose();
                pose.translate(-origin.x + cam.x, -origin.y + cam.y, -origin.z + cam.z);
                Direction towards = Direction.getNearest(cam.x - (pos.getX() + 0.5), 0, cam.z - (pos.getZ() + 0.5));
                BrewingStation.drawLoose(pose, buffers, cam, pos, towards, ingredient, fuel, light);
                pose.popPose();
            }
        }
    }

    /** One bottle standing on a foot ({@code at} is the middle of the bottle), turned to the camera. */
    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 origin, Vec3 cam, ItemStack stack, Vec3 at, float grow, float scale, float rotation, int light) {
        Vec3 toEye = new Vec3(cam.x - at.x, 0, cam.z - at.z);
        float yaw = (float) (180 - Math.toDegrees(Math.atan2(-toEye.x, toEye.z)));
        // standing on its foot: grown from the bottom, not the middle
        Vec3 center = at.add(0, 7 / 16.0 * SIZE * scale * (grow - 1), 0);
        ItemPlacer.drawOriented(pose, buffers, origin, stack, center, ItemPlacer.lying(yaw + rotation, 90), SIZE * grow * scale, light);
    }

    @Override
    public boolean shouldRenderOffScreen(BrewingStandBlockEntity stand) {
        return false;
    }
}
