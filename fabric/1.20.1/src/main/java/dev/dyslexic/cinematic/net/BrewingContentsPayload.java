package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** The brewing stand's five slots (three bottles, ingredient, blaze powder), so everyone sees the real potions; empty = bare. */
public record BrewingContentsPayload(BlockPos pos, List<ItemStack> items) implements FabricPacket {
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
    }
}
