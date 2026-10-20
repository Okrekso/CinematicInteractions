package dev.dyslexic.cinematic.net;

import net.minecraft.network.FriendlyByteBuf;

/** A message on the mod's channel ({@link Network}); each one also has a static {@code read(FriendlyByteBuf)}. */
public interface Payload {
    void write(FriendlyByteBuf buf);
}
