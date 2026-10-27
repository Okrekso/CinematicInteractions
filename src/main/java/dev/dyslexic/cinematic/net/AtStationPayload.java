package dev.dyslexic.cinematic.net;

//? if !forge {
import dev.dyslexic.cinematic.Cinematic;
//?}
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Optional;
//? if fabric && <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** Tells watchers that a player works at a station (or stopped), so their model can take the matching pose. */
//? if >=1.21 {
public record AtStationPayload(int entityId, Optional<BlockPos> pos, StationKind kind) implements CustomPacketPayload {
    public static final Type<AtStationPayload> TYPE = new Type<>(Cinematic.id("at_station"));
    public static final StreamCodec<FriendlyByteBuf, AtStationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AtStationPayload::entityId,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), AtStationPayload::pos,
            ByteBufCodecs.idMapper(i -> StationKind.values()[i], StationKind::ordinal), AtStationPayload::kind,
            AtStationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record AtStationPayload(int entityId, Optional<BlockPos> pos, StationKind kind) implements FabricPacket {
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
*///?} else if forge {
/*public record AtStationPayload(int entityId, Optional<BlockPos> pos, StationKind kind) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeOptional(pos, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(kind);
    }

    public static AtStationPayload read(FriendlyByteBuf buf) {
        return new AtStationPayload(buf.readVarInt(), buf.readOptional(FriendlyByteBuf::readBlockPos), buf.readEnum(StationKind.class));
*///?}
    }
}
