package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions.Session;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The jukebox's scene on the server: a record is put in (from the item in hand or a hotbar slot) by the same rules as
 * using the jukebox with it, or the one playing is taken out, into the inventory rather than dropped. Without the scene
 * (turned off, or out of reach) using the jukebox does what it does in vanilla.
 */
public final class JukeboxActions {
    private JukeboxActions() {}

    public static void handle(ServerPlayer player, ServerLevel level, Session session, StationActionPayload action) {
        BlockPos pos = session.pos();
        if (action.action() != StationActionPayload.Action.CLICK || !(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getValue(JukeboxBlock.HAS_RECORD)) {
            ItemStack record = jukebox.removeFirstItem();
            if (!record.isEmpty()) {
                player.getInventory().placeItemBackInInventory(record);
            }
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        } else {
            ItemStack source = action.index() == 0 ? player.inventoryMenu.getCarried()
                    : action.button() >= 0 && action.button() < 36 ? player.getInventory().getItem(action.button()) : ItemStack.EMPTY;
            if (isRecord(source)) {
                insert(level, pos, state, jukebox, source, player);
                if (action.index() == 0 && source.isEmpty()) {
                    player.inventoryMenu.setCarried(ItemStack.EMPTY);
                }
            }
        }
        player.inventoryMenu.broadcastChanges();
        show(player, session);
    }

    /** Tells the clients nearby which record is in the jukebox (they are not told otherwise), for the scene to show it. */
    public static void show(ServerPlayer player, Session session) {
        ServerLevel level = player.serverLevel();
        ItemStack record = level.getBlockEntity(session.pos()) instanceof JukeboxBlockEntity jukebox ? jukebox.getFirstItem().copy() : ItemStack.EMPTY;
        dev.dyslexic.cinematic.net.Network.sendToTrackingChunk(level, new ChunkPos(session.pos()),
                new MenuDisplayPayload(session.pos(), StationKind.JUKEBOX, session.side().get2DDataValue(), List.of(record)));
    }

    /** Using the jukebox without the scene: the vanilla way (the record pops out, or the one in hand goes in). */
    public static void vanillaUse(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX)) {
            return;
        }
        if (state.getValue(JukeboxBlock.HAS_RECORD)) {
            if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
                jukebox.popOutRecord();
            }
        } else if (isRecord(player.getMainHandItem()) && level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
            insert(level, pos, state, jukebox, player.getMainHandItem(), player);
        }
    }

    /** What a jukebox plays in 1.20.1: a music disc (the jukebox itself takes nothing else). */
    public static boolean isRecord(ItemStack stack) {
        return stack.is(ItemTags.MUSIC_DISCS);
    }

    /** As a record used on an empty jukebox: one goes in and plays; a creative player's stack is not used up. */
    private static void insert(ServerLevel level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, ItemStack source, ServerPlayer player) {
        jukebox.setFirstItem(source.copyWithCount(1));
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        if (!player.getAbilities().instabuild) {
            source.shrink(1);
        }
        player.awardStat(Stats.PLAY_RECORD);
    }
}
