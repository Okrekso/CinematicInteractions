package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** What floats over an enchanting table (and how much lapis lies in its corner) while someone works at it. */
public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements FabricPacket {
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
    }
}
