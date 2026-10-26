package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * What lies on an anvil, grindstone, smithing table or stonecutter while someone works at it (the menu's input
 * slots, in order), and the side they work from; an empty list once they step away.
 */
public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements FabricPacket {
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
    }
}
