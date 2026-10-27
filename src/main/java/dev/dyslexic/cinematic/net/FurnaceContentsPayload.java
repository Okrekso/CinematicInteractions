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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else if <1.21 {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}
import net.minecraft.world.item.ItemStack;

/** The three furnace slots (input, fuel, result), so everyone sees what sits in the openings; empty = bare. */
//? if >=1.21 {
public record FurnaceContentsPayload(BlockPos pos, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<FurnaceContentsPayload> TYPE = new Type<>(Cinematic.id("furnace_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceContentsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FurnaceContentsPayload::pos,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, FurnaceContentsPayload::items,
            FurnaceContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record FurnaceContentsPayload(BlockPos pos, List<ItemStack> items) implements FabricPacket {
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
*///?} else if forge {
/*public record FurnaceContentsPayload(BlockPos pos, List<ItemStack> items) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static FurnaceContentsPayload read(FriendlyByteBuf buf) {
        return new FurnaceContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
*///?}
    }
}
