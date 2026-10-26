package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** The three furnace slots (input, fuel, result), so everyone sees what sits in the openings; empty = bare. */
public record FurnaceContentsPayload(BlockPos pos, List<ItemStack> items) implements FabricPacket {
    public static final PacketType<FurnaceContentsPayload> TYPE = PacketType.create(Cinematic.id("furnace_contents"), FurnaceContentsPayload::read);

    @Override
    public PacketType<FurnaceContentsPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static FurnaceContentsPayload read(FriendlyByteBuf buf) {
        return new FurnaceContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
    }
}
