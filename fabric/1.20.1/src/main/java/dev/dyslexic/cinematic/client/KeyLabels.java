package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The name of the key a mapping is bound to, as printed on the key. Vanilla asks GLFW for the character the current
 * layout gives, so with a Russian layout the H key reads "Р", easily taken for a Latin P; the bound key itself is a
 * physical one, named after the US layout, so letter keys are shown by that Latin letter. Anything else (Alt, F5, a
 * mouse button, a key rebound to a non-letter) keeps the vanilla name.
 */
final class KeyLabels {
    private KeyLabels() {}

    static Component of(KeyMapping mapping) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
        int code = key.getValue();
        if (key.getType() == InputConstants.Type.KEYSYM && code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) {
            return Component.literal(String.valueOf((char) code));
        }
        return mapping.getTranslatedKeyMessage();
    }
}
