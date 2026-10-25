package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
    }
}
