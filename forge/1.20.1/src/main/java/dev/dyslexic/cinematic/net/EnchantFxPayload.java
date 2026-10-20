package dev.dyslexic.cinematic.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** An enchantment just happened at a table: onlookers get the burst of light and runes. */
public record EnchantFxPayload(BlockPos pos, int entityId) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(entityId);
    }

    public static EnchantFxPayload read(FriendlyByteBuf buf) {
        return new EnchantFxPayload(buf.readBlockPos(), buf.readVarInt());
    }
}
