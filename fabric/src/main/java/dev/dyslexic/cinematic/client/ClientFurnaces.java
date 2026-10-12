package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/** Client copy of what sits inside nearby furnaces (input, fuel, result), with landing times per slot. */
public final class ClientFurnaces {
    private ClientFurnaces() {}

    public static final class Entry {
        public final ItemStack[] items = new ItemStack[3];
        public final double[] landedAt = new double[3];

        Entry() {
            Arrays.fill(items, ItemStack.EMPTY);
            Arrays.fill(landedAt, -10);
        }
    }

    public static final Map<BlockPos, Entry> FURNACES = new HashMap<>();

    @Nullable
    public static Entry get(BlockPos pos) {
        return FURNACES.get(pos);
    }

    public static ItemStack item(BlockPos pos, int slot) {
        Entry entry = FURNACES.get(pos);
        return entry == null ? ItemStack.EMPTY : entry.items[slot];
    }

    public static void apply(FurnaceContentsPayload payload) {
        if (payload.items().isEmpty()) {
            FURNACES.remove(payload.pos());
            return;
        }
        Entry entry = FURNACES.computeIfAbsent(payload.pos().immutable(), p -> new Entry());
        for (int i = 0; i < 3 && i < payload.items().size(); i++) {
            ItemStack before = entry.items[i];
            ItemStack after = payload.items().get(i);
            if (!after.isEmpty() && (before.isEmpty() || !ItemStack.isSameItemSameComponents(before, after) || after.getCount() > before.getCount())) {
                entry.landedAt[i] = Scene.clock();
            }
            entry.items[i] = after;
        }
    }

    /** Forgets furnaces that are no longer there. */
    public static void prune(ClientLevel level) {
        FURNACES.keySet().removeIf(pos -> {
            StationKind kind = StationKind.of(level.getBlockState(pos));
            return level.isLoaded(pos) && (kind == null || !kind.isFurnace());
        });
    }

    public static void unloadChunk(ChunkPos chunk) {
        FURNACES.keySet().removeIf(pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z);
    }

    public static void clear() {
        FURNACES.clear();
    }
}
