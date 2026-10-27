package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.MatrixUtil;
//?} else if <26.1 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
*///?}
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=26.1 {
import net.minecraft.client.resources.model.geometry.BakedQuad;
//?}

/** Records vanilla item geometry through the scene's alpha consumer, preserving tint and foil. */
final class ItemModels {
    private ItemModels() {}
    //? if >=26.1 {
    static void drawStatic(net.minecraft.world.item.ItemStack stack, int light, int overlay, PoseStack pose, SceneBuffers buffers) {
    //?} else if <26.1 {

    /*/^* What ItemRenderer.renderStatic did with an item shown on a frame: the stack's FIXED model, drawn now. ^/
    static void drawStatic(net.minecraft.world.item.ItemStack stack, int light, int overlay, PoseStack pose, MultiBufferSource buffers) {
    *///?}
        draw(ItemPlacer.model(stack, 0), pose, buffers, light, overlay);
    }
    //? if >=26.1 {
    static void draw(ItemStackRenderState state, PoseStack pose, SceneBuffers buffers, int light, int overlay) {
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(light);
        instance.setOverlayCoords(overlay);
    //?} else if <26.1 {

    /*static void draw(ItemStackRenderState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    *///?}
        for (int i = 0; i < state.activeLayerCount; i++) {
            //? if >=26.1 {
            var layer = state.layers[i];
            //?} else if <26.1 {
            /*ItemStackRenderState.LayerRenderState layer = state.layers[i];
            *///?}
            pose.pushPose();
            //? if >=26.1 {
            layer.itemTransform.apply(state.displayContext.leftHand(), pose.last());
            pose.last().mulPose(layer.localTransform);
            //?} else if <26.1 {
            /*layer.transform.apply(state.displayContext.leftHand(), pose.last());
            *///?}
            if (layer.specialRenderer != null) {
                //? if >=26.3 {
                drawSpecial(layer, pose, buffers, light, overlay);
            } else {
                PoseStack.Pose foilPose = pose.last().copy();
                if (state.displayContext == net.minecraft.world.item.ItemDisplayContext.GUI) {
                    MatrixUtil.mulComponentWise(foilPose.pose(), 0.5f);
                } else if (state.displayContext.firstPerson()) {
                    MatrixUtil.mulComponentWise(foilPose.pose(), 0.75f);
                }
                var tints = layer.tintLayers();
                for (BakedQuad quad : layer.quads.all()) {
                    var material = quad.materialInfo();
                    var type = switch (layer.foilType) {
                        case NONE -> material.itemRenderType();
                        case STANDARD -> material.itemGlintRenderType();
                        case SPECIAL -> material.itemGlintSpecialRenderType();
                    };
                    int tint = material.isTinted() && material.tintIndex() >= 0 && material.tintIndex() < tints.size()
                            ? tints.getInt(material.tintIndex()) : -1;
                    instance.setColor(tint);
                    if (layer.foilType == ItemStackRenderState.FoilType.SPECIAL) {
                        buffers.getBuffer(type).putBakedQuadWithGlint(pose.last(), quad, instance, foilPose);
                    } else {
                        buffers.getBuffer(type).putBakedQuad(pose.last(), quad, instance);
                    }
                }
                //?} else if >=26.2 <26.3 {
                /*drawSpecial(layer, pose, buffers, light, overlay);
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
                        boolean transparent = net.minecraft.client.Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                                && type.outputTarget() == net.minecraft.client.renderer.rendertype.OutputTarget.ITEM_ENTITY_TARGET;
                        var foil = buffers.getBuffer(transparent ? net.minecraft.client.renderer.rendertype.RenderTypes.glintTranslucent()
                                : net.minecraft.client.renderer.rendertype.RenderTypes.glint());
                        if (layer.foilType == ItemStackRenderState.FoilType.SPECIAL) {
                            foil = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(foil, foilPose, 0.0078125f);
                        }
                        foil.putBakedQuad(pose.last(), quad, instance);
                    }
                }
                *///?} else if >=26.1 <26.2 {
                /*drawSpecial(layer, pose, buffers, light, overlay);
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
                *///?} else if <26.1 {
                /*layer.specialRenderer.submit(layer.argumentForSpecialRendering, state.displayContext, pose,
                        Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(), light, overlay,
                        layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
            } else if (layer.renderType != null) {
                ItemRenderer.renderItem(state.displayContext, pose, buffers, light, overlay, layer.tintLayers, layer.quads, layer.renderType,
                        layer.foilType);
                *///?}
            }
            pose.popPose();
        //? if >=26.2 {
        }
    }

    private static void drawSpecial(ItemStackRenderState.LayerRenderState layer, PoseStack pose, SceneBuffers buffers, int light, int overlay) {
        var storage = new net.minecraft.client.renderer.SubmitNodeStorage();
        layer.specialRenderer.submit(layer.argumentForSpecialRendering, pose, storage, light, overlay,
                layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
        var nodes = new java.util.ArrayList<net.minecraft.client.renderer.feature.submit.SubmitNode>();
        storage.drainPhases(phase -> phase.sortInto((node, ordered) -> nodes.add(node)));
        if (nodes.stream().anyMatch(node -> !(node instanceof net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<?>)
                && !(node instanceof net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit))) {
            // Keep extensions that submit another feature type in the game's own renderer.
            layer.specialRenderer.submit(layer.argumentForSpecialRendering, pose, SceneRenderer.collector(), light, overlay,
                    layer.foilType != ItemStackRenderState.FoilType.NONE, 0);
            return;
        //?} else if >=26.1 <26.2 {
        /*}
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
        *///?}
        }
        //? if >=26.3 {
        for (var node : nodes) {
            if (node instanceof net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<?> model) {
                drawModel(model, buffers);
            } else if (node instanceof net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit custom) {
                custom.customGeometryRenderer().render(custom.pose(), buffers.getBuffer(custom.renderType()));
            }
        }
    }

    private static <S> void drawModel(net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<S> submit, SceneBuffers buffers) {
        PoseStack pose = new PoseStack();
        pose.last().set(submit.pose());
        var consumer = buffers.getBuffer(submit.renderType());
        if (submit.sheetedDecalPose() != null) {
            consumer = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(consumer, submit.sheetedDecalPose(), 1);
        } else if (submit.uvMapping() != null) {
            consumer = submit.uvMapping().wrap(consumer);
        }
        submit.model().setupAnim(submit.state());
        submit.model().renderToBuffer(pose, consumer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
        //?} else if >=26.2 <26.3 {
        /*for (var node : nodes) {
            if (node instanceof net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<?> model) {
                drawModel(model, buffers);
            } else if (node instanceof net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit custom) {
                custom.customGeometryRenderer().render(custom.pose(), buffers.getBuffer(custom.renderType()));
            }
        }
    }

    private static <S> void drawModel(net.minecraft.client.renderer.feature.ModelFeatureRenderer.Submit<S> submit, SceneBuffers buffers) {
        PoseStack pose = new PoseStack();
        pose.last().set(submit.pose());
        var consumer = buffers.getBuffer(submit.renderType());
        if (submit.sheetedDecalPose() != null) {
            consumer = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(consumer, submit.sheetedDecalPose(), 1);
        } else if (submit.sprite() != null) {
            consumer = submit.sprite().wrap(consumer);
        }
        submit.model().setupAnim(submit.state());
        submit.model().renderToBuffer(pose, consumer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
        *///?}
    }
}
