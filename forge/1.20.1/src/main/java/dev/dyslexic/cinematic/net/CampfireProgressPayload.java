package dev.dyslexic.cinematic.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** The four real cooking timers of the campfire the player is sitting beside. */
public record CampfireProgressPayload(BlockPos pos, boolean lit, int[] progress, int[] total) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(lit);
        for (int i = 0; i < 4; i++) {
            buf.writeVarInt(progress[i]);
            buf.writeVarInt(total[i]);
        }
    }

    public static CampfireProgressPayload read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean lit = buf.readBoolean();
        int[] progress = new int[4], total = new int[4];
        for (int i = 0; i < 4; i++) {
            progress[i] = buf.readVarInt();
            total[i] = buf.readVarInt();
        }
        return new CampfireProgressPayload(pos, lit, progress, total);
    }
}
