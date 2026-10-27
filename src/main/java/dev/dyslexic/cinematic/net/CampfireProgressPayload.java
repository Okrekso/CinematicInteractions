package dev.dyslexic.cinematic.net;

//? if >=1.21 {
import dev.dyslexic.cinematic.Cinematic;
//?} else if fabric && <1.21 {
/*import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** The four real cooking timers of the campfire the player is sitting beside. */
//? if >=1.21 {
public record CampfireProgressPayload(BlockPos pos, boolean lit, int[] progress, int[] total) implements CustomPacketPayload {
    public static final Type<CampfireProgressPayload> TYPE = new Type<>(Cinematic.id("campfire_progress"));
    public static final StreamCodec<FriendlyByteBuf, CampfireProgressPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeBoolean(payload.lit);
                for (int i = 0; i < 4; i++) {
                    buf.writeVarInt(payload.progress[i]);
                    buf.writeVarInt(payload.total[i]);
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                boolean lit = buf.readBoolean();
                int[] progress = new int[4], total = new int[4];
                for (int i = 0; i < 4; i++) {
                    progress[i] = buf.readVarInt();
                    total[i] = buf.readVarInt();
                }
                return new CampfireProgressPayload(pos, lit, progress, total);
            });
//?} else if fabric && <1.21 {
/*public record CampfireProgressPayload(BlockPos pos, boolean lit, int[] progress, int[] total) implements FabricPacket {
    public static final PacketType<CampfireProgressPayload> TYPE = PacketType.create(Cinematic.id("campfire_progress"), CampfireProgressPayload::read);

    @Override
    public PacketType<CampfireProgressPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(lit);
        for (int i = 0; i < 4; i++) {
            buf.writeVarInt(progress[i]);
            buf.writeVarInt(total[i]);
        }
    }
*///?} else if forge {
/*public record CampfireProgressPayload(BlockPos pos, boolean lit, int[] progress, int[] total) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(lit);
        for (int i = 0; i < 4; i++) {
            buf.writeVarInt(progress[i]);
            buf.writeVarInt(total[i]);
        }
    }
*///?}

    //? if >=1.21 {
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?} else if <1.21 {
    /*public static CampfireProgressPayload read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean lit = buf.readBoolean();
        int[] progress = new int[4], total = new int[4];
        for (int i = 0; i < 4; i++) {
            progress[i] = buf.readVarInt();
            total[i] = buf.readVarInt();
        }
        return new CampfireProgressPayload(pos, lit, progress, total);
    }
    *///?}
}
