package dev.dyslexic.cinematic.client;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/** Text in the scene: 1.21.11 draws it straight into the scene's buffers. */
final class SceneText {
    private SceneText() {}
    static void draw(Font font, String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
            SceneBuffers buffers, Font.DisplayMode mode, int background, int light) {
        draw(font, Component.literal(text).getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, background, light);
    }
    static void draw(Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f matrix,
            SceneBuffers buffers, Font.DisplayMode mode, int background, int light) {
        draw(font, text.getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, background, light);
    }
    static void draw(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f matrix,
            SceneBuffers buffers, Font.DisplayMode mode, int background, int light) {
        font.drawInBatch(text, x, y, color, shadow, matrix, buffers, mode, background, light);
    }
}
