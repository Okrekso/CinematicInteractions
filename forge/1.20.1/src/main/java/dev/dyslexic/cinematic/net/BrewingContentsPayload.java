package dev.dyslexic.cinematic.net;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** The brewing stand's five slots (three bottles, ingredient, blaze powder), so everyone sees the real potions; empty = bare. */
public record BrewingContentsPayload(BlockPos pos, List<ItemStack> items) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static BrewingContentsPayload read(FriendlyByteBuf buf) {
        return new BrewingContentsPayload(buf.readBlockPos(), buf.readList(FriendlyByteBuf::readItem));
    }
}
