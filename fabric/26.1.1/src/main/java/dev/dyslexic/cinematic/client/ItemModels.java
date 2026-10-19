package dev.dyslexic.cinematic.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.MatrixUtil;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;

/** Records vanilla item geometry through the scene's alpha consumer, preserving tint and foil. */
final class ItemModels {
    private ItemModels() {}
    static void drawStatic(net.minecraft.world.item.ItemStack stack, int light, int overlay, PoseStack pose, SceneBuffers buffers) {
        draw(ItemPlacer.model(stack, 0), pose, buffers, light, overlay);
    }
    static void draw(ItemStackRenderState state, PoseStack pose, SceneBuffers buffers, int light, int overlay) {
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(light);
        instance.setOverlayCoords(overlay);
        for (int i = 0; i < state.activeLayerCount; i++) {
            var layer = state.layers[i];
            pose.pushPose();
            layer.itemTransform.apply(state.displayContext.leftHand(), pose.last());
            pose.last().mulPose(layer.localTransform);
            if (layer.specialRenderer != null) {
                drawSpecial(layer, pose, buffers, light, overlay);
            } else {
                PoseStack.Pose foilPose = pose.last().copy();
                if (state.displayContext == net.minecraft.world.item.ItemDisplayContext.GUI) {
                    MatrixUtil.mulComponentWise(foilPose.pose(), 0.5f);
                } else if (state.displayContext.firstPerson()) {
                    MatrixUtil.mulComponentWise(foilPose.pose(), 0.75f);
                }
                var tints = layer.tintLayers();
                for (BakedQuad quad : layer.quads) {
                    var material = quad.materialInfo();
                    var type = material.itemRenderType();
                    int tint = material.isTinted() && material.tintIndex() >= 0 && material.tintIndex() < tints.size()
                            ? tints.getInt(material.tintIndex()) : -1;
                    instance.setColor(tint);
                    buffers.getBuffer(type).putBakedQuad(pose.last(), quad, instance);
                    if (layer.foilType != ItemStackRenderState.FoilType.NONE) {
                        boolean transparent = net.minecraft.client.Minecraft.useShaderTransparency()
                                && type.outputTarget() == net.minecraft.client.renderer.rendertype.OutputTarget.ITEM_ENTITY_TARGET;
                        var foil = buffers.getBuffer(transparent ? net.minecraft.client.renderer.rendertype.RenderTypes.glintTranslucent()
                                : net.minecraft.client.renderer.rendertype.RenderTypes.glint());
                        if (layer.foilType == ItemStackRenderState.FoilType.SPECIAL) {
                            foil = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(foil, foilPose, 0.0078125f);
                        }
                        foil.putBakedQuad(pose.last(), quad, instance);
                    }
                }
            }
            pose.popPose();
        }
    }

    private static void drawSpecial(ItemStackRenderState.LayerRenderState layer, PoseStack pose, SceneBuffers buffers, int light, int overlay) {
        // In 26.1 the submitted nodes are stored by feature rather than exposed as a
        // flat list. Capture the two geometry kinds used by vanilla special items so
        // they pass through the same alpha buffer as ordinary item quads.
        var capture = new net.minecraft.client.renderer.SubmitNodeStorage() {
            boolean captured;

            @Override public <S> void submitModel(net.minecraft.client.model.Model<? super S> model, S state,
                    PoseStack modelPose, net.minecraft.client.renderer.rendertype.RenderType type,
                    int modelLight, int modelOverlay, int color,
                    net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int outline,
                    net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
                captured = true;
                var consumer = buffers.getBuffer(type);
                if (sprite != null) consumer = sprite.wrap(consumer);
                model.setupAnim(state);
                model.renderToBuffer(modelPose, consumer, modelLight, modelOverlay, color);
            }

            @Override public void submitCustomGeometry(PoseStack customPose,
                    net.minecraft.client.renderer.rendertype.RenderType type,
                    net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer geometry) {
                captured = true;
                geometry.render(customPose.last(), buffers.getBuffer(type));
            }
        };
        layer.specialRenderer.submit(layer.argumentForSpecialRendering, pose, capture, light, overlay,
                layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
        if (!capture.captured) {
            // Unknown extension renderers keep their own vanilla rendering path.
            layer.specialRenderer.submit(layer.argumentForSpecialRendering, pose, SceneRenderer.collector(), light, overlay,
                    layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
        }
    }
}
