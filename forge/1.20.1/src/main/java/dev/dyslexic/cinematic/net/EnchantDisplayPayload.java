package dev.dyslexic.cinematic.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** What floats over an enchanting table (and how much lapis lies in its corner) while someone works at it. */
public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(item);
        buf.writeVarInt(lapis);
        buf.writeVarInt(side);
    }

    public static EnchantDisplayPayload read(FriendlyByteBuf buf) {
        return new EnchantDisplayPayload(buf.readBlockPos(), buf.readItem(), buf.readVarInt(), buf.readVarInt());
    }
}
