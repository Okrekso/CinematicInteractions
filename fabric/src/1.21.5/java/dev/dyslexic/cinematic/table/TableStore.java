package dev.dyslexic.cinematic.table;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Items lying on crafting tables in one dimension. Vanilla tables have no block entity, so they live here. */
public final class TableStore extends SavedData {
    private static final String NAME = "cinematic_tables";

    /** What lies on one table: nine cells and, per cell, the side its item was put down from. */
    public static final class Table {
        public final NonNullList<ItemStack> items = NonNullList.withSize(TableGrid.CELLS, ItemStack.EMPTY);
        public final byte[] facing = new byte[TableGrid.CELLS];

        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private final Map<BlockPos, Table> tables = new HashMap<>();
    private final Long2ObjectMap<Set<BlockPos>> byChunk = new Long2ObjectOpenHashMap<>();

    public static TableStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<TableStore>(NAME,
                context -> new TableStore(), context -> CompoundTag.CODEC.xmap(
                        tag -> load(tag, context.levelOrThrow().registryAccess()),
                        store -> store.save(new CompoundTag(), context.levelOrThrow().registryAccess())), null));
    }

    @Nullable
    public Table get(BlockPos pos) {
        return tables.get(pos);
    }

    public Table getOrCreate(BlockPos pos) {
        return tables.computeIfAbsent(pos.immutable(), p -> {
            byChunk.computeIfAbsent(ChunkPos.asLong(p), c -> new HashSet<>()).add(p);
            return new Table();
        });
    }

    @Nullable
    public Table remove(BlockPos pos) {
        Table table = tables.remove(pos);
        if (table != null) {
            long chunk = ChunkPos.asLong(pos);
            Set<BlockPos> set = byChunk.get(chunk);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    byChunk.remove(chunk);
                }
            }
            setDirty();
        }
        return table;
    }

    /** Forgets a table once nothing lies on it any more. */
    public void cleanup(BlockPos pos) {
        Table table = tables.get(pos);
        if (table != null && table.isEmpty()) {
            remove(pos);
        }
    }

    public Collection<BlockPos> inChunk(ChunkPos chunk) {
        Set<BlockPos> set = byChunk.get(chunk.toLong());
        return set == null ? List.of() : set;
    }

    public List<BlockPos> positions() {
        return new ArrayList<>(tables.keySet());
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        tables.forEach((pos, table) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            entry.putByteArray("facing", table.facing);
            ContainerHelper.saveAllItems(entry, table.items, registries);
            list.add(entry);
        });
        tag.put("tables", list);
        return tag;
    }

    private static TableStore load(CompoundTag tag, HolderLookup.Provider registries) {
        TableStore store = new TableStore();
        ListTag list = tag.getListOrEmpty("tables");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            Table table = store.getOrCreate(BlockPos.of(entry.getLongOr("pos", 0)));
            ContainerHelper.loadAllItems(entry, table.items, registries);
            byte[] facing = entry.getByteArray("facing").orElseGet(() -> new byte[0]);
            System.arraycopy(facing, 0, table.facing, 0, Math.min(facing.length, TableGrid.CELLS));
        }
        return store;
    }
}
