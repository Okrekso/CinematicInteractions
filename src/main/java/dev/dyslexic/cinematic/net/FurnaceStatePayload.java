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

//? if >=1.21 {
/**
 * Fire and cooking progress of the furnace the player crouches at (what the vanilla menu would show), and the recipe it
 * was told to use when several compete for its input (Polymorph; "" when there is no choice).
 */
public record FurnaceStatePayload(BlockPos pos, int litTime, int litDuration, int cookingProgress, int cookingTotalTime, String choice)
        implements CustomPacketPayload {
    public static final Type<FurnaceStatePayload> TYPE = new Type<>(Cinematic.id("furnace_state"));
    public static final StreamCodec<FriendlyByteBuf, FurnaceStatePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeVarInt(p.litTime);
                buf.writeVarInt(p.litDuration);
                buf.writeVarInt(p.cookingProgress);
                buf.writeVarInt(p.cookingTotalTime);
                buf.writeUtf(p.choice);
            },
            buf -> new FurnaceStatePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*/^* Fire and cooking progress of the furnace the player crouches at (what the vanilla menu would show). ^/
public record FurnaceStatePayload(BlockPos pos, int litTime, int litDuration, int cookingProgress, int cookingTotalTime, String choice) implements FabricPacket {
    public static final PacketType<FurnaceStatePayload> TYPE = PacketType.create(Cinematic.id("furnace_state"), FurnaceStatePayload::read);

    @Override
    public PacketType<FurnaceStatePayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(litTime);
        buf.writeVarInt(litDuration);
        buf.writeVarInt(cookingProgress);
        buf.writeVarInt(cookingTotalTime);
        buf.writeUtf(choice);
    }

    public static FurnaceStatePayload read(FriendlyByteBuf buf) {
        return new FurnaceStatePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
*///?} else if forge {
/*/^* Fire and cooking progress of the furnace the player crouches at (what the vanilla menu would show). ^/
public record FurnaceStatePayload(BlockPos pos, int litTime, int litDuration, int cookingProgress, int cookingTotalTime, String choice) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(litTime);
        buf.writeVarInt(litDuration);
        buf.writeVarInt(cookingProgress);
        buf.writeVarInt(cookingTotalTime);
        buf.writeUtf(choice);
    }

    public static FurnaceStatePayload read(FriendlyByteBuf buf) {
        return new FurnaceStatePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
*///?}
    }
}
