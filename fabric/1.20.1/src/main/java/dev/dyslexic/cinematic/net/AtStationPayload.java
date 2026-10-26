package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Tells watchers that a player works at a station (or stopped), so their model can take the matching pose. */
public record AtStationPayload(int entityId, Optional<BlockPos> pos, StationKind kind) implements FabricPacket {
    public static final PacketType<AtStationPayload> TYPE = PacketType.create(Cinematic.id("at_station"), AtStationPayload::read);

    @Override
    public PacketType<AtStationPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeOptional(pos, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(kind);
    }

    public static AtStationPayload read(FriendlyByteBuf buf) {
        return new AtStationPayload(buf.readVarInt(), buf.readOptional(FriendlyByteBuf::readBlockPos), buf.readEnum(StationKind.class));
    }
}
