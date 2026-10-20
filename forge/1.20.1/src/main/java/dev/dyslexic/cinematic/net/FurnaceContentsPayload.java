package dev.dyslexic.cinematic.net;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** The three furnace slots (input, fuel, result), so everyone sees what sits in the openings; empty = bare. */
public record FurnaceContentsPayload(BlockPos pos, List<ItemStack> items) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static FurnaceContentsPayload read(FriendlyByteBuf buf) {
        return new FurnaceContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
    }
}
