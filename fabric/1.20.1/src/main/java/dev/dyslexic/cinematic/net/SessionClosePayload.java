package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** The server ended the player's session (block broken, too far away, another menu opened...). */
public record SessionClosePayload() implements FabricPacket {
    public static final PacketType<SessionClosePayload> TYPE = PacketType.create(Cinematic.id("session_close"), SessionClosePayload::read);

    @Override
    public PacketType<SessionClosePayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static SessionClosePayload read(FriendlyByteBuf buf) {
        return new SessionClosePayload();
    }
}
