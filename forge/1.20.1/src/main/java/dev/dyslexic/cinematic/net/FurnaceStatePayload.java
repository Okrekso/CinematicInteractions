package dev.dyslexic.cinematic.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Fire and cooking progress of the furnace the player crouches at (what the vanilla menu would show). */
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
    }
}
