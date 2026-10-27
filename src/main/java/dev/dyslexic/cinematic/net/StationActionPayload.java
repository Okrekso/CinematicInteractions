package dev.dyslexic.cinematic.net;

//? if !forge {
import dev.dyslexic.cinematic.Cinematic;
//?}
import dev.dyslexic.cinematic.station.StationKind;
//? if fabric && <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/**
 * Everything the player does at a station: step up, touch a spot ({@code index} is a table cell or a furnace
 * zone), craft, step away, or ask for the block's own window when the scene cannot be played there.
 */
public record StationActionPayload(BlockPos pos, StationKind kind, Action action, int index, int button, boolean shift, int side)
        //? if >=1.21 {
        implements CustomPacketPayload {
        //?} else if fabric && <1.21 {
        /*implements FabricPacket {
    public static final PacketType<StationActionPayload> TYPE = PacketType.create(Cinematic.id("station_action"), StationActionPayload::read);

    @Override
    public PacketType<StationActionPayload> getType() {
        return TYPE;
    }

        *///?} else if forge {
        /*implements Payload {
        *///?}
    /**
     * PLACE: items from inventory slot {@code button} (-1: the item in hand) into table cell {@code index} (at a
     * chiseled bookshelf, into its slot {@code index}); at a table {@code side} is how many (0 counts as one).
     * WINDOW at a chiseled bookshelf: {@code index} is the book used. SPREAD: the stack in hand split evenly
     * between the table cells whose bits are set in {@code index}, as a vanilla left-drag across slots. QUICK: a
     * shift-click on inventory slot {@code index} (0..35) in the scene's drawer: the stack goes into the station where it
     * fits, as a shift-click into a menu would put it, or stays where it is.
     */
    /** CHOOSE: which of the recipes competing for the station's input to use (index in RecipeChoices.matching). */
    public enum Action { BEGIN, CLICK, PAINT, CRAFT, LEAVE, WINDOW, PLACE, SPREAD, QUICK, CHOOSE }

    //? if >=1.21 {
    public static final Type<StationActionPayload> TYPE = new Type<>(Cinematic.id("station_action"));
    public static final StreamCodec<FriendlyByteBuf, StationActionPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeEnum(p.kind);
                buf.writeEnum(p.action);
                // a varint: SPREAD needs nine bits for its cells
                buf.writeVarInt(p.index);
                buf.writeByte(p.button);
                buf.writeBoolean(p.shift);
                buf.writeByte(p.side);
            },
            buf -> new StationActionPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readEnum(Action.class),
                    buf.readVarInt(), buf.readByte(), buf.readBoolean(), buf.readByte()));
    //?} else if <1.21 {
    /*@Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(kind);
        buf.writeEnum(action);
        // a varint: SPREAD needs nine bits for its cells
        buf.writeVarInt(index);
        buf.writeByte(button);
        buf.writeBoolean(shift);
        buf.writeByte(side);
    }

    public static StationActionPayload read(FriendlyByteBuf buf) {
        return new StationActionPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readEnum(Action.class),
                buf.readVarInt(), buf.readByte(), buf.readBoolean(), buf.readByte());
    }
    *///?}

    public static StationActionPayload of(BlockPos pos, StationKind kind, Action action) {
        return new StationActionPayload(pos, kind, action, 0, 0, false, 0);
    //? if >=1.21 {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    //?}
    }
}
