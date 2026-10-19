package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Everything the player does at a station: step up, touch a spot ({@code index} is a table cell or a furnace
 * zone), craft, step away, or ask for the block's own window when the scene cannot be played there.
 */
public record StationActionPayload(BlockPos pos, StationKind kind, Action action, int index, int button, boolean shift, int side)
        implements CustomPacketPayload {
    /**
     * PLACE: one item from inventory slot {@code button} (-1: the item in hand) into table cell {@code index} (at a
     * chiseled bookshelf, into its slot {@code index}). WINDOW at a chiseled bookshelf: {@code index} is the book used.
     */
    public enum Action { BEGIN, CLICK, PAINT, CRAFT, LEAVE, WINDOW, PLACE }

    public static final Type<StationActionPayload> TYPE = new Type<>(Cinematic.id("station_action"));
    public static final StreamCodec<FriendlyByteBuf, StationActionPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeEnum(p.kind);
                buf.writeEnum(p.action);
                buf.writeByte(p.index);
                buf.writeByte(p.button);
                buf.writeBoolean(p.shift);
                buf.writeByte(p.side);
            },
            buf -> new StationActionPayload(buf.readBlockPos(), buf.readEnum(StationKind.class), buf.readEnum(Action.class),
                    buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readByte()));

    public static StationActionPayload of(BlockPos pos, StationKind kind, Action action) {
        return new StationActionPayload(pos, kind, action, 0, 0, false, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
