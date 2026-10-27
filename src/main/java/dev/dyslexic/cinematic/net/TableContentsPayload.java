package dev.dyslexic.cinematic.net;

//? if !forge {
import dev.dyslexic.cinematic.Cinematic;
//?}
import java.util.List;
//? if fabric && <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else if <1.21 {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}
import net.minecraft.world.item.ItemStack;

/** What lies on one table; an empty list means the table is bare. */
//? if >=1.21 {
public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements CustomPacketPayload {
    public static final Type<TableContentsPayload> TYPE = new Type<>(Cinematic.id("table_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TableContentsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TableContentsPayload::pos,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, TableContentsPayload::items,
            ByteBufCodecs.BYTE_ARRAY, TableContentsPayload::facing,
            TableContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements FabricPacket {
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
*///?} else if forge {
/*public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
        buf.writeByteArray(facing);
    }

    public static TableContentsPayload read(FriendlyByteBuf buf) {
        return new TableContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem), buf.readByteArray());
*///?}
    }
}
