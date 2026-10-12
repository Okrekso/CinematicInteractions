package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * The flags on the loom's board. Since 1.21.6 the GUI draws a model only as a picture rendered on its own, and one
 * renderer keeps one picture a frame, so the board's flags are one picture: all of them drawn into it where the board
 * shows them, as they used to be drawn straight onto the screen.
 */
final class LoomFlags {
    private LoomFlags() {}

    /** One flag: its top centred at (x, y) in GUI pixels, {@code width} pixels wide. */
    record Flag(float x, float y, float width, DyeColor base, BannerPatternLayers layers) {}

    record State(List<Flag> flags, ModelPart model, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(List<Flag> flags, ModelPart model, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissor) {
            this(flags, model, x0, y0, x1, y1, scissor, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissor));
        }

        /** One unit of the picture's pose is one GUI pixel. */
        @Override
        public float scale() {
            return 1;
        }
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        Renderer(MultiBufferSource.BufferSource buffers) {
            super(buffers);
        }

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
        protected void renderToTexture(State state, PoseStack pose) {
            // lit as the GUI lit the flags drawn straight onto it
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D);
            float middle = (state.x0() + state.x1()) / 2f;
            for (Flag flag : state.flags()) {
                float k = flag.width() / 1.25f;
                pose.pushPose();
                pose.translate(flag.x() - middle, flag.y() - state.y0(), 0);
                pose.scale(k, k, k);
                BannerRenderer.renderPatterns(pose, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.model(),
                        ModelBakery.BANNER_BASE, true, flag.base(), flag.layers());
                pose.popPose();
            }
        }

        @Override
        protected String getTextureLabel() {
            return "cinematic loom board";
        }
    }
}
