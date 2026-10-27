package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Physical letter keys keep their Latin label when the operating-system layout changes. */
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
