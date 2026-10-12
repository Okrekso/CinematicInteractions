package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dyslexic.cinematic.Cinematic;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
 * pool of render targets that is reused from page to page and from book to book.
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
    private static final ResourceLocation BOOK = ResourceLocation.withDefaultNamespace("textures/gui/book.png");

    /** A page painted into a render target, shown as an ordinary texture. */
    private static final class Page extends AbstractTexture {
        final TextureTarget target;
        final ResourceLocation id;
        int book = Integer.MIN_VALUE;
        int index = Integer.MIN_VALUE;
        long used;

        Page(int slot) {
            target = new TextureTarget(W * SCALE, H * SCALE, false, Minecraft.ON_OSX);
            id = Cinematic.id("book_page/" + slot);
        }

        @Override
        public void load(ResourceManager manager) {}

        @Override
        public int getId() {
            return target.getColorTextureId();
        }

        @Override
        public void releaseId() {
            target.destroyBuffers();
        }
    }

    private static final List<Page> PAGES = new ArrayList<>();
    private static long frame;
    public static com.mojang.blaze3d.pipeline.RenderTarget paintingTarget;

    /** A left-hand page: odd numbers and the endpapers before the first page (the first page is a right-hand one). */
    static boolean leftHand(int index) {
        return index < 0 || index % 2 != 0;
    }

    /** The texture of a page of a book, or null when it has not been painted yet. */
    @Nullable
    static ResourceLocation texture(int book, int index) {
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

        TextureTarget target = page.target;
        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        paintingTarget = target;
        try {
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, W * SCALE, H * SCALE, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.translation(0, 0, -11000);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        g.pose().scale(SCALE, SCALE, 1);
        // the paper; a left-hand page is the same sheet turned over, its stitching towards the spine
        if (left) {
            g.blit(BOOK, 0, 0, W, H, PAPER_U + W, PAPER_V, -W, H, 256, 256);
        } else {
            g.blit(BOOK, 0, 0, W, H, PAPER_U, PAPER_V, W, H, 256, 256);
        }
        if (text != null && index >= 0 && index < text.size()) {
            Font font = mc.font;
            List<FormattedCharSequence> lines = font.split(text.get(index), TEXT_W);
            int x = left ? W - TEXT_X - TEXT_W : TEXT_X;
            for (int i = 0; i < Math.min(LINES, lines.size()); i++) {
                g.drawString(font, lines.get(i), x, TEXT_Y + i * 9, 0, false);
            }
            String number = String.valueOf(index + 1);
            g.drawString(font, number, (W - font.width(number)) / 2, H - 13, 0x9A8466, false);
        }
        g.flush();

        view.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, sorting);
        } finally { paintingTarget = null; }
        mc.getMainRenderTarget().bindWrite(true);
    }

    /** Forget what the pages show (a resource reload or another world: they are painted again when needed). */
    static void forget() {
        for (Page page : PAGES) {
            page.book = Integer.MIN_VALUE;
            page.index = Integer.MIN_VALUE;
        }
    }
}
