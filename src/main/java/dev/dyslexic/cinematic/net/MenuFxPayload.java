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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else if <1.21 {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}
import net.minecraft.world.item.ItemStack;

/**
 * Something in someone's scene for onlookers to see. BEAT: the big moment (the hammer falls, the wheel bites...),
 * sparks. MOVE: a big move begins (forging, grinding, cutting) with what lay on the block ({@code items}), what comes
 * of it ({@code result}) and how it ends ({@code variant}: 1 into the bag), so onlookers see the whole of it. At a
 * chiseled bookshelf, for the book in slot {@code variant} ({@code result}): PULL taken off, PUT put in, READ taken
 * out to read, READ_END put back.
 */
public record MenuFxPayload(BlockPos pos, StationKind kind, int entityId, Cue cue, int variant, List<ItemStack> items, ItemStack result)
        //? if >=1.21 {
        implements CustomPacketPayload {
        //?} else if fabric && <1.21 {
        /*implements FabricPacket {
    public static final PacketType<MenuFxPayload> TYPE = PacketType.create(Cinematic.id("menu_fx"), MenuFxPayload::read);

    @Override
    public PacketType<MenuFxPayload> getType() {
        return TYPE;
    }

        *///?} else if forge {
        /*implements Payload {
        *///?}
    public enum Cue { BEAT, MOVE, PULL, PUT, READ, READ_END }

    //? if >=1.21 {
    public static final Type<MenuFxPayload> TYPE = new Type<>(Cinematic.id("menu_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuFxPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeEnum(p.kind);
                buf.writeVarInt(p.entityId);
                buf.writeEnum(p.cue);
                buf.writeVarInt(p.variant);
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, p.items);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.result);
            },
            buf -> new MenuFxPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readVarInt(), buf.readEnum(Cue.class), buf.readVarInt(),
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));
    //?} else if <1.21 {
    /*@Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(kind);
        buf.writeVarInt(entityId);
        buf.writeEnum(cue);
        buf.writeVarInt(variant);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
        buf.writeItem(result);
    }

    public static MenuFxPayload read(FriendlyByteBuf buf) {
        return new MenuFxPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readVarInt(), buf.readEnum(Cue.class), buf.readVarInt(),
                buf.readList(FriendlyByteBuf::readItem), buf.readItem());
    }
    *///?}

    public static MenuFxPayload beat(BlockPos pos, StationKind kind, int entityId) {
        return new MenuFxPayload(pos, kind, entityId, Cue.BEAT, 0, List.of(), ItemStack.EMPTY);
    //? if >=1.21 {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    //?}
    }
}
