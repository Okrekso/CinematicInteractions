//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/^*
 * Never sent: its id only marks the payload format. NeoForge negotiates one version for all of the mod's channels
 * ("11"), Fabric negotiates every id on its own, so both sides also register this versioned id, and a peer counts as
 * having the mod only when it knows it. An older build of this port (same ids, other formats) then counts as none.
 ^/
public record ProtocolPayload() implements CustomPacketPayload {
    public static final Type<ProtocolPayload> TYPE = new Type<>(Cinematic.id("protocol_12"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProtocolPayload> CODEC = StreamCodec.unit(new ProtocolPayload());

    @Override
    public Type<ProtocolPayload> type() {
        return TYPE;
    }
}
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/^*
 * Never sent: its id only marks the payload format. Forge negotiates one version for the mod's channel ("12"), Fabric
 * negotiates every id on its own, so both sides also register this versioned id, and a peer counts as having the mod
 * only when it knows it. An older build of this port (same ids, other formats) then counts as none.
 ^/
public record ProtocolPayload() implements FabricPacket {
    public static final PacketType<ProtocolPayload> TYPE = PacketType.create(Cinematic.id("protocol_12"), buf -> new ProtocolPayload());

    @Override
    public void write(FriendlyByteBuf buf) {}

    @Override
    public PacketType<ProtocolPayload> getType() {
        return TYPE;
    }
}
*///?}
