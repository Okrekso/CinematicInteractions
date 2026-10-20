package dev.dyslexic.cinematic.net;

import net.minecraft.network.FriendlyByteBuf;

/** The server ended the player's session (block broken, too far away, another menu opened...). */
public record SessionClosePayload() implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {}

    public static SessionClosePayload read(FriendlyByteBuf buf) {
        return new SessionClosePayload();
    }
}
