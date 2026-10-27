package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Item models drawn straight into a buffer, as the game drew them up to 1.21.8. Since 1.21.9 an item is only handed to
 * the game's features and drawn with them later, so the scene could no longer fade an item through its own buffer:
 * this draws an ordinary model layer by layer as the item feature does. A model drawn by a special renderer (a chest,
 * a banner, a shield) still goes to the features and is drawn when the scene has finished (SceneRenderer.drawSubmitted).
 */
final class ItemModels {
    private ItemModels() {}

    /** What ItemRenderer.renderStatic did with an item shown on a frame: the stack's FIXED model, drawn now. */
    static void drawStatic(net.minecraft.world.item.ItemStack stack, int light, int overlay, PoseStack pose, MultiBufferSource buffers) {
        draw(ItemPlacer.model(stack, 0), pose, buffers, light, overlay);
    }

    static void draw(ItemStackRenderState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int i = 0; i < state.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState layer = state.layers[i];
            pose.pushPose();
            layer.transform.apply(state.displayContext.leftHand(), pose.last());
            if (layer.specialRenderer != null) {
                layer.specialRenderer.submit(layer.argumentForSpecialRendering, state.displayContext, pose,
                        Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), light, overlay,
                        layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
            } else if (layer.renderType != null) {
                ItemRenderer.renderItem(state.displayContext, pose, buffers, light, overlay, layer.tintLayers, layer.quads, layer.renderType,
                        layer.foilType);
            }
            pose.popPose();
        }
    }
}
