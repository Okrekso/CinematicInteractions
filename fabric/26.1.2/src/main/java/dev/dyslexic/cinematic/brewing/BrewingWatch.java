package dev.dyslexic.cinematic.brewing;

import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The brewing stand's bottles are drawn by the mod as the real potions (the stand's own bottle pictures are taken
 * out of its model), so clients need to know what stands in it. Vanilla sends nothing: the stand's update packet
 * carries its slots, and every server tick a cheap fingerprint of the bottles is compared; when it changes (a potion
 * put in or taken, a brew finished) the stand is sent again.
 */
public final class BrewingWatch {
    private BrewingWatch() {}

    public static final int SLOTS = 5;
    public static final int BOTTLES = 3;

    /** Mixed into the brewing stand block entity so the last sent fingerprint lives with the stand itself. */
    public interface Fingerprinted {
        int cinematic$fingerprint();

        void cinematic$setFingerprint(int fingerprint);
    }

    /** Called at the end of every server tick of a brewing stand. */
    public static void afterTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand) {
        if (level.isClientSide() || !(stand instanceof Fingerprinted marked)) {
            return;
        }
        int fingerprint = fingerprint(stand);
        if (fingerprint != marked.cinematic$fingerprint()) {
            marked.cinematic$setFingerprint(fingerprint);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Cheap enough for every tick of every stand: which stacks stand in it and how many (as for furnaces). */
    private static int fingerprint(BrewingStandBlockEntity stand) {
        return FurnaceWatch.contentsFingerprint(stand, SLOTS);
    }

    /** What a client is told about a stand: its slots, nothing else. */
    public static CompoundTag updateTag(BrewingStandBlockEntity stand, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOTS; slot++) {
            items.set(slot, stand.getItem(slot));
        }
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(output, items, true);
        return output.buildResult();
    }
}
