package dev.dyslexic.cinematic.net;

//? if >=1.21 {
import dev.dyslexic.cinematic.Cinematic;
//?} else if fabric && <1.21 {
/*import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
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

/** What floats over an enchanting table (and how much lapis lies in its corner) while someone works at it. */
//? if >=1.21 {
public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements CustomPacketPayload {
    public static final Type<EnchantDisplayPayload> TYPE = new Type<>(Cinematic.id("enchant_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantDisplayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EnchantDisplayPayload::pos,
            ItemStack.OPTIONAL_STREAM_CODEC, EnchantDisplayPayload::item,
            ByteBufCodecs.VAR_INT, EnchantDisplayPayload::lapis,
            ByteBufCodecs.VAR_INT, EnchantDisplayPayload::side,
            EnchantDisplayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements FabricPacket {
    public static final PacketType<EnchantDisplayPayload> TYPE = PacketType.create(Cinematic.id("enchant_display"), EnchantDisplayPayload::read);

    @Override
    public PacketType<EnchantDisplayPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(item);
        buf.writeVarInt(lapis);
        buf.writeVarInt(side);
    }

    public static EnchantDisplayPayload read(FriendlyByteBuf buf) {
        return new EnchantDisplayPayload(buf.readBlockPos(), buf.readItem(), buf.readVarInt(), buf.readVarInt());
*///?} else if forge {
/*public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(item);
        buf.writeVarInt(lapis);
        buf.writeVarInt(side);
    }

    public static EnchantDisplayPayload read(FriendlyByteBuf buf) {
        return new EnchantDisplayPayload(buf.readBlockPos(), buf.readItem(), buf.readVarInt(), buf.readVarInt());
*///?}
    }
}
