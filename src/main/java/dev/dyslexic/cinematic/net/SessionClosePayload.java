package dev.dyslexic.cinematic.net;

//? if >=1.21 {
import dev.dyslexic.cinematic.Cinematic;
//?} else if fabric && <1.21 {
/*import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** The server ended the player's session (block broken, too far away, another menu opened...). */
//? if >=1.21 {
public record SessionClosePayload() implements CustomPacketPayload {
    public static final Type<SessionClosePayload> TYPE = new Type<>(Cinematic.id("session_close"));
    public static final StreamCodec<FriendlyByteBuf, SessionClosePayload> CODEC = StreamCodec.unit(new SessionClosePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record SessionClosePayload() implements FabricPacket {
    public static final PacketType<SessionClosePayload> TYPE = PacketType.create(Cinematic.id("session_close"), SessionClosePayload::read);

    @Override
    public PacketType<SessionClosePayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {}

    public static SessionClosePayload read(FriendlyByteBuf buf) {
        return new SessionClosePayload();
*///?} else if forge {
/*public record SessionClosePayload() implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {}

    public static SessionClosePayload read(FriendlyByteBuf buf) {
        return new SessionClosePayload();
*///?}
    }
}
