package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
//? if >=26.2 {
import com.mojang.renderpearl.api.GpuFormat;
//?} else if <26.2 {
/*import com.mojang.renderpearl.api.textures.TextureFormat;
*///?}
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.Cinematic;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
//?} else if <26.1 {
/*import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
*///?}
import net.minecraft.util.LightCoordsUtil;
//? if >=26.1 {
import dev.dyslexic.cinematic.client.SceneBuffers;
//?} else if <26.1 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Pages of a book painted into textures, so a book lying on a lectern can be read in the world and its leaves can bend
 * while they turn. Each page is the paper of the vanilla book window ({@code textures/gui/book.png}, mirrored for a
 * left-hand page) with its text laid out exactly as the book window lays it out, in the game's own font, four
 * texels to a pixel so the letters stay sharp up close.
 * <p>
 * Pages are painted before the frame (never inside the world pass, where shader mods own the framebuffers) into a small
 * pool of textures that is reused from page to page and from book to book. Since 1.21.6 the GUI no longer draws
 * straight away, so a page is drawn as the game draws its own pictures-in-pictures: paper and letters go through a
 * buffer whose output is pointed at the page's texture.
 */
public final class BookPages {
    private BookPages() {}

    /** The paper inside the leather frame of book.png, in its pixels. */
    static final int PAPER_U = 26, PAPER_V = 8;
    static final int W = 131, H = 164;
    static final int SCALE = 4;
    /** Where the book window puts the text on that paper (BookViewScreen: x 36, y 30, 114 wide, 14 lines). */
    static final int TEXT_X = 10, TEXT_Y = 22, TEXT_W = 114, LINES = 14;
    private static final int POOL = 8;
    private static final Identifier BOOK = Identifier.withDefaultNamespace("textures/gui/book.png");
    private static final int INK = 0xFF000000, PAGE_NUMBER = 0xFF9A8466;

    /** A page painted into a texture of its own, shown as an ordinary texture. */
    private static final class Page extends AbstractTexture {
        final Identifier id;
        int book = Integer.MIN_VALUE;
        int index = Integer.MIN_VALUE;
        long used;

        Page(int slot) {
            GpuDevice device = RenderSystem.getDevice();
            //? if >=26.2 {
            texture = device.createTexture(() -> "Cinematic book page", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, W * SCALE, H * SCALE, 1, 1);
            //?} else if >=26.1 <26.2 {
            /*texture = device.createTexture(() -> "Cinematic book page", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RGBA8, W * SCALE, H * SCALE, 1, 1);
            *///?} else if <26.1 {
            /*texture = device.createTexture(() -> "Cinematic book page", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8, W * SCALE, H * SCALE, 1, 1);
            *///?}
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            textureView = device.createTextureView(texture);
            id = Cinematic.id("book_page/" + slot);
        }
    }

    //? if <26.1 {
    /*/^* Kept for the shared Minecraft mixin, which redirected the GUI into a page before 1.21.6; never set here. ^/
    @Nullable
    public static com.mojang.renderpearl.api.pipeline.RenderTarget paintingTarget;

    *///?}
    private static final List<Page> PAGES = new ArrayList<>();
    private static long frame;
    @Nullable
    private static GpuTexture depth;
    @Nullable
    private static GpuTextureView depthView;
    @Nullable
    //? if >=26.1 {
    private static ProjectionMatrixBuffer projection;
    //?} else if <26.1 {
    /*private static CachedOrthoProjectionMatrixBuffer projection;
    *///?}
    @Nullable
    private static FogRenderer noFog;

    /** A left-hand page: odd numbers and the endpapers before the first page (the first page is a right-hand one). */
    static boolean leftHand(int index) {
        return index < 0 || index % 2 != 0;
    }

    /** The texture of a page of a book, or null when it has not been painted yet. */
    @Nullable
    static Identifier texture(int book, int index) {
        for (Page page : PAGES) {
            if (page.book == book && page.index == index) {
                page.used = frame;
                return page.id;
            }
        }
        return null;
    }

    /**
     * Before a frame: makes sure these pages of the book are painted ({@code pages} may hold endpapers, below 0, and
     * blank leaves past the end).
     */
    static void prepare(int book, @Nullable List<FormattedText> text, int... pages) {
        frame++;
        for (int index : pages) {
            if (texture(book, index) == null) {
                paint(take(), book, text, index);
            }
        }
    }

    /** A free page, or the one unused for longest. */
    private static Page take() {
        if (PAGES.size() < POOL) {
            Page page = new Page(PAGES.size());
            Minecraft.getInstance().getTextureManager().register(page.id, page);
            PAGES.add(page);
            return page;
        }
        Page oldest = PAGES.get(0);
        for (Page page : PAGES) {
            if (page.used < oldest.used) {
                oldest = page;
            }
        }
        return oldest;
    }

    private static void paint(Page page, int book, @Nullable List<FormattedText> text, int index) {
        Minecraft mc = Minecraft.getInstance();
        page.book = book;
        page.index = index;
        page.used = frame;
        boolean left = leftHand(index);

        GpuDevice device = RenderSystem.getDevice();
        if (depth == null) {
            //? if >=26.2 {
            depth = device.createTexture(() -> "Cinematic book page depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.D32_FLOAT, W * SCALE, H * SCALE, 1, 1);
            //?} else if >=26.1 <26.2 {
            /*depth = device.createTexture(() -> "Cinematic book page depth", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.DEPTH32, W * SCALE, H * SCALE, 1, 1);
            *///?} else if <26.1 {
            /*depth = device.createTexture(() -> "Cinematic book page depth", GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32,
                    W * SCALE, H * SCALE, 1, 1);
            *///?}
            depthView = device.createTextureView(depth);
            //? if >=26.1 {
            projection = new ProjectionMatrixBuffer("Cinematic book page");
            //?} else if <26.1 {
            /*projection = new CachedOrthoProjectionMatrixBuffer("Cinematic book page", -1000, 1000, true);
            *///?}
        }
        GpuBufferSlice fog = RenderSystem.getShaderFog();
        //? if >=26.2 {
        if (noFog == null) noFog = new FogRenderer();
        device.createCommandEncoder().clearColorAndDepthTextures(page.getTexture(), new org.joml.Vector4f(), depth, 0.0);
        //?} else if >=26.1 <26.2 {
        /*if (noFog == null) noFog = new FogRenderer();
        // 26.1.x uses forward depth for this off-screen pass; clearing to 0 rejects the entire page.
        device.createCommandEncoder().clearColorAndDepthTextures(page.getTexture(), 0, depth, 1.0);
        *///?} else if <26.1 {
        /*if (noFog == null) {
            noFog = new FogRenderer();
        }
        device.createCommandEncoder().clearColorAndDepthTextures(page.getTexture(), 0, depth, 1.0);
        RenderSystem.outputColorTextureOverride = page.getTextureView();
        RenderSystem.outputDepthTextureOverride = depthView;
        *///?}
        RenderSystem.backupProjectionMatrix();
        //? if >=26.1 {
        var ortho = new net.minecraft.client.renderer.Projection();
        ortho.setupOrtho(-1000, 1000, W * SCALE, H * SCALE, true);
        RenderSystem.setProjectionMatrix(projection.getBuffer(ortho), ProjectionType.ORTHOGRAPHIC);
        //?} else if <26.1 {
        /*RenderSystem.setProjectionMatrix(projection.getBuffer(W * SCALE, H * SCALE), ProjectionType.ORTHOGRAPHIC);
        *///?}
        RenderSystem.setShaderFog(noFog.getBuffer(FogRenderer.FogMode.NONE));
        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        //? if >=26.1 <26.3 {
        /*var oldColorTarget = RenderSystem.outputColorTextureOverride;
        var oldDepthTarget = RenderSystem.outputDepthTextureOverride;
        *///?}
        try {
            //? if >=26.1 {
            var collector = new net.minecraft.client.renderer.SubmitNodeStorage();
            SceneBuffers.BufferSource buffers = new SceneBuffers.BufferSource(collector);
            //?} else if <26.1 {
            /*MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            *///?}
            Matrix4f pose = new Matrix4f().scale(SCALE, SCALE, 1);
            // the paper; a left-hand page is the same sheet turned over, its stitching towards the spine
            float u0 = (left ? PAPER_U + W : PAPER_U) / 256f, u1 = (left ? PAPER_U : PAPER_U + W) / 256f;
            float v0 = PAPER_V / 256f, v1 = (PAPER_V + H) / 256f;
            VertexConsumer paper = buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.text(BOOK));
            paper.addVertex(pose, 0, 0, 0).setColor(-1).setUv(u0, v0).setLight(LightCoordsUtil.FULL_BRIGHT);
            paper.addVertex(pose, 0, H, 0).setColor(-1).setUv(u0, v1).setLight(LightCoordsUtil.FULL_BRIGHT);
            paper.addVertex(pose, W, H, 0).setColor(-1).setUv(u1, v1).setLight(LightCoordsUtil.FULL_BRIGHT);
            paper.addVertex(pose, W, 0, 0).setColor(-1).setUv(u1, v0).setLight(LightCoordsUtil.FULL_BRIGHT);
            buffers.endBatch();
            if (text != null && index >= 0 && index < text.size()) {
                Font font = mc.font;
                List<FormattedCharSequence> lines = font.split(text.get(index), TEXT_W);
                int x = left ? W - TEXT_X - TEXT_W : TEXT_X;
                //? if >=26.1 {
                var textPose = new com.mojang.blaze3d.vertex.PoseStack();
                textPose.mulPose(pose);
                //?}
                for (int i = 0; i < Math.min(LINES, lines.size()); i++) {
                    //? if >=26.1 {
                    collector.order(1).submitText(textPose, x, TEXT_Y + i * 9, lines.get(i), false, Font.DisplayMode.NORMAL,
                            LightCoordsUtil.FULL_BRIGHT, INK, 0, 0);
                    //?} else if <26.1 {
                    /*font.drawInBatch(lines.get(i), x, TEXT_Y + i * 9, INK, false, pose, buffers, Font.DisplayMode.NORMAL, 0,
                            LightCoordsUtil.FULL_BRIGHT);
                    *///?}
                }
                String number = String.valueOf(index + 1);
                //? if >=26.1 {
                collector.order(1).submitText(textPose, (W - font.width(number)) / 2f, H - 13,
                        net.minecraft.network.chat.Component.literal(number).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT, PAGE_NUMBER, 0, 0);
                //?} else if <26.1 {
                /*font.drawInBatch(number, (W - font.width(number)) / 2f, H - 13, PAGE_NUMBER, false, pose, buffers, Font.DisplayMode.NORMAL, 0,
                        LightCoordsUtil.FULL_BRIGHT);
                buffers.endBatch();
                *///?}
            }
            //? if >=26.3 {
            try (var frame = mc.gameRenderer.featureRenderDispatcher().prepareFrame(collector);
                    var pass = device.createCommandEncoder().createRenderPass(() -> "Cinematic book page",
                            page.getTextureView(), java.util.Optional.empty(), depthView, java.util.OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                net.minecraft.client.renderer.feature.FeatureRenderDispatcher.renderAllFeatures(pass, frame);
            }
            //?} else if >=26.2 <26.3 {
            /*RenderSystem.outputColorTextureOverride = page.getTextureView();
            RenderSystem.outputDepthTextureOverride = depthView;
            mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(collector);
            *///?} else if >=26.1 <26.2 {
            /*RenderSystem.outputColorTextureOverride = page.getTextureView();
            RenderSystem.outputDepthTextureOverride = depthView;
            var dispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
            var previous = dispatcher.getSubmitNodeStorage();
            try {
                ((dev.dyslexic.cinematic.mixin.FeatureRenderDispatcherAccessor) dispatcher)
                        .cinematic$setSubmitNodeStorage(collector);
                dispatcher.renderAllFeatures();
                // 26.1.x queues the page and glyph vertices in the dispatcher's buffer source.
                // Flush them before restoring the render target, or the page texture stays black.
                ((dev.dyslexic.cinematic.mixin.FeatureRenderDispatcherAccessor) dispatcher)
                        .cinematic$getBufferSource().endBatch();
            } finally {
                ((dev.dyslexic.cinematic.mixin.FeatureRenderDispatcherAccessor) dispatcher)
                        .cinematic$setSubmitNodeStorage(previous);
            }
            *///?}
        } finally {
            //? if >=26.1 <26.3 {
            /*RenderSystem.outputColorTextureOverride = oldColorTarget;
            RenderSystem.outputDepthTextureOverride = oldDepthTarget;
            *///?}
            view.popMatrix();
            RenderSystem.setShaderFog(fog);
            RenderSystem.restoreProjectionMatrix();
            //? if <26.1 {
            /*RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            *///?}
        }
    }

    /** Forget what the pages show (a resource reload or another world: they are painted again when needed). */
    static void forget() {
        for (Page page : PAGES) {
            page.book = Integer.MIN_VALUE;
            page.index = Integer.MIN_VALUE;
        }
    }
}
