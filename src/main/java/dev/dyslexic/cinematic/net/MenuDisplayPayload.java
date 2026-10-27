package dev.dyslexic.cinematic.net;

//? if !forge {
import dev.dyslexic.cinematic.Cinematic;
//?}
import dev.dyslexic.cinematic.station.StationKind;
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
//? if neoforge {
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
//?}

/**
 * What lies on an anvil, grindstone, smithing table or stonecutter while someone works at it (the menu's input
 * slots, in order), and the side they work from; an empty list once they step away.
 */
//? if neoforge {
public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<MenuDisplayPayload> TYPE = new Type<>(Cinematic.id("menu_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuDisplayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MenuDisplayPayload::pos,
            NeoForgeStreamCodecs.enumCodec(StationKind.class), MenuDisplayPayload::kind,
            ByteBufCodecs.VAR_INT, MenuDisplayPayload::side,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, MenuDisplayPayload::items,
            MenuDisplayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && >=1.21 {
/*public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<MenuDisplayPayload> TYPE = new Type<>(Cinematic.id("menu_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuDisplayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MenuDisplayPayload::pos,
            Network.enumCodec(StationKind.class), MenuDisplayPayload::kind,
            ByteBufCodecs.VAR_INT, MenuDisplayPayload::side,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, MenuDisplayPayload::items,
            MenuDisplayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
*///?} else if fabric && <1.21 {
/*public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements FabricPacket {
    public static final PacketType<MenuDisplayPayload> TYPE = PacketType.create(Cinematic.id("menu_display"), MenuDisplayPayload::read);

    @Override
    public PacketType<MenuDisplayPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(kind);
        buf.writeVarInt(side);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static MenuDisplayPayload read(FriendlyByteBuf buf) {
        return new MenuDisplayPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readVarInt(), buf.readList(FriendlyByteBuf::readItem));
*///?} else if forge {
/*public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(kind);
        buf.writeVarInt(side);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static MenuDisplayPayload read(FriendlyByteBuf buf) {
        return new MenuDisplayPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readVarInt(), buf.readList(FriendlyByteBuf::readItem));
*///?}
    }
}
