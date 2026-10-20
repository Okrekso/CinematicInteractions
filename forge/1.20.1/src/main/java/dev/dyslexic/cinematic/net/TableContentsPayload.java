package dev.dyslexic.cinematic.net;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** What lies on one table; an empty list means the table is bare. */
public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
        buf.writeByteArray(facing);
    }

    public static TableContentsPayload read(FriendlyByteBuf buf) {
        return new TableContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem), buf.readByteArray());
    }
}
