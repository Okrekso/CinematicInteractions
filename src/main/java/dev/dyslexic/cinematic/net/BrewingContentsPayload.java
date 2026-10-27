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

/** The brewing stand's five slots (three bottles, ingredient, blaze powder), so everyone sees the real potions; empty = bare. */
//? if >=1.21 {
public record BrewingContentsPayload(BlockPos pos, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<BrewingContentsPayload> TYPE = new Type<>(Cinematic.id("brewing_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingContentsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BrewingContentsPayload::pos,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, BrewingContentsPayload::items,
            BrewingContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record BrewingContentsPayload(BlockPos pos, List<ItemStack> items) implements FabricPacket {
    public static final PacketType<BrewingContentsPayload> TYPE = PacketType.create(Cinematic.id("brewing_contents"), BrewingContentsPayload::read);

    @Override
    public PacketType<BrewingContentsPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static BrewingContentsPayload read(FriendlyByteBuf buf) {
        return new BrewingContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
*///?} else if forge {
/*public record BrewingContentsPayload(BlockPos pos, List<ItemStack> items) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static BrewingContentsPayload read(FriendlyByteBuf buf) {
        return new BrewingContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
*///?}
    }
}
