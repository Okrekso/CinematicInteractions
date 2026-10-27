package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
//? if >=26.1 {
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
//?} else if <26.1 {
/*import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
*///?}
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.util.LightCoordsUtil;
//? if >=26.1 {
import dev.dyslexic.cinematic.client.SceneBuffers;
//?} else if <26.1 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * The flags on the loom's board. Since 1.21.6 the GUI draws a model only as a picture rendered on its own, and one
 * renderer keeps one picture a frame, so the board's flags are one picture: all of them drawn into it where the board
 * shows them, as they used to be drawn straight onto the screen.
 * <p>
 * The flag model is a plain one ({@link Model.Simple}) round the bare flag part, so drawing it neither sways the flag nor
 * moves it back up its pole as the banner flag model would.
 */
final class LoomFlags {
    private LoomFlags() {}

    /** One flag: its top centred at (x, y) in GUI pixels, {@code width} pixels wide. */
    record Flag(float x, float y, float width, DyeColor base, BannerPatternLayers layers) {}

    record State(List<Flag> flags, Model.Simple model, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(List<Flag> flags, Model.Simple model, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissor) {
            this(flags, model, x0, y0, x1, y1, scissor, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissor));
        }

        /** One unit of the picture's pose is one GUI pixel. */
        @Override
        public float scale() {
            return 1;
        }
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        //? if >=26.2 {
        Renderer() {}
        //?} else if >=26.1 <26.2 {
        /*private final net.minecraft.client.renderer.SubmitNodeCollector collector;

        Renderer(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                net.minecraft.client.renderer.SubmitNodeCollector collector) {
            super(buffers);
            this.collector = collector;
        }
        *///?} else if <26.1 {
        /*Renderer(MultiBufferSource.BufferSource buffers) {
            super(buffers);
        }
        *///?}

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        /** The picture's origin is the middle of its top edge. */
        @Override
        protected float getTranslateY(int height, int guiScale) {
            return 0;
        }

        @Override
        //? if >=26.2 {
        protected void renderToTexture(State state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector) {
        //?} else if <26.2 {
        /*protected void renderToTexture(State state, PoseStack pose) {
        *///?}
            // lit as the GUI lit the flags drawn straight onto it
            //? if >=26.2 {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D);
            //?} else if <26.2 {
            /*Minecraft.getInstance().gameRenderer.getLighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D);
            *///?}
            // since 1.21.9 a model is handed to the game's features and drawn with them, as the game draws its banner result
            Minecraft mc = Minecraft.getInstance();
            //? if <26.1 {
            /*FeatureRenderDispatcher features = mc.gameRenderer.getFeatureRenderDispatcher();
            *///?}
            float middle = (state.x0() + state.x1()) / 2f;
            for (Flag flag : state.flags()) {
                float k = flag.width() / 1.25f;
                pose.pushPose();
                pose.translate(flag.x() - middle, flag.y() - state.y0(), 0);
                pose.scale(k, k, k);
                //? if >=26.3 {
                collector.submitModel(state.model(), net.minecraft.util.Unit.INSTANCE, pose, LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, -1, net.minecraft.client.renderer.Sheets.BANNER_BASE, mc.getAtlasManager(), 0);
                BannerRenderer.submitPatterns(mc.getAtlasManager(), pose, collector, LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, state.model(), net.minecraft.util.Unit.INSTANCE, true, flag.base(), flag.layers());
                //?} else if >=26.1 <26.3 {
                /*collector.submitModel(state.model(), net.minecraft.util.Unit.INSTANCE, pose, LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, -1, net.minecraft.client.renderer.Sheets.BANNER_BASE, mc.getAtlasManager(), 0, null);
                BannerRenderer.submitPatterns(mc.getAtlasManager(), pose, collector, LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, state.model(), net.minecraft.util.Unit.INSTANCE, true, flag.base(), flag.layers(), null);
                *///?} else if <26.1 {
                /*BannerRenderer.submitPatterns(mc.getAtlasManager(), pose, features.getSubmitNodeStorage(), LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, state.model(), net.minecraft.util.Unit.INSTANCE, ModelBakery.BANNER_BASE, true, flag.base(),
                        flag.layers(), false, null, 0);
                *///?}
                pose.popPose();
            }
            //? if <26.1 {
            /*features.renderAllFeatures();
            *///?}
        }

        @Override
        protected String getTextureLabel() {
            return "cinematic loom board";
        }
    }
}
