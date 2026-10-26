package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** What lies on one table; an empty list means the table is bare. */
public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements FabricPacket {
    public static final PacketType<TableContentsPayload> TYPE = PacketType.create(Cinematic.id("table_contents"), TableContentsPayload::read);

    @Override
    public PacketType<TableContentsPayload> getType() {
        return TYPE;
    }

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
