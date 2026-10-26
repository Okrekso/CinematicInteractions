package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.brewing.BrewingWatch;
import dev.dyslexic.cinematic.campfire.CampfireActions;
import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** The NeoForge events of the original, as Fabric API callbacks and the mixins that stand in for the missing ones. */
public final class StationEvents {
    private StationEvents() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            BlockState state = level.getBlockState(hit.getBlockPos());
            StationKind kind = StationKind.of(state);
            if (kind == null || !kind.startsOnUse() || hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive()
                    || player.isSpectator() || !kind.usable(level, hit.getBlockPos(), state) || !kind.usedOn(state, hit.getDirection())) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                // a client without the mod uses the block as in vanilla
                if (player instanceof ServerPlayer serverPlayer && !Network.clientHasMod(serverPlayer)) return InteractionResult.PASS;
                // no swing shown to others, as with NeoForge's CONSUME
                return InteractionResult.CONSUME;
            }
            if (ClientBridge.vanillaUse.test(hit.getBlockPos().immutable(), kind)) return InteractionResult.PASS;
            ClientBridge.useAt = hit.getLocation();
            ClientBridge.useStation.accept(hit.getBlockPos().immutable(), kind);
            ClientBridge.swallowSwing = true;
            // Fabric sends vanilla's use packet to the server only for SUCCESS (NeoForge always sends it), and the server
            // has to see the use too; the swing SUCCESS brings on the client is swallowed (ClientBridge.swallowSwing)
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) Sessions.tick(player);
        });
        // Repairs a lost cooking timer before the vanilla campfire ticker can treat zero as "finished".
        ServerTickEvents.START_WORLD_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                Sessions.Session session = Sessions.get(player);
                if (session != null && session.kind() == StationKind.CAMPFIRE && session.dimension().equals(level.dimension())) {
                    CampfireActions.tick(level, session);
                }
            }
        });
        // Catches tables removed without a neighbour update (pistons, some commands and mods).
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 40 != 0) return;
            TableStore store = TableStore.get(level);
            for (BlockPos pos : store.positions()) {
                if (level.isLoaded(pos) && !TableGrid.isTable(level.getBlockState(pos))) TableActions.spill(level, pos);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((target, watcher) -> {
            if (target instanceof ServerPlayer player) Sessions.sendAtStation(player, watcher);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Sessions.end(handler.player, false));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) Sessions.end(player, true);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> Sessions.end(player, true));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Sessions.clear());
    }

    /**
     * A block changed and tells its neighbours (ServerLevelMixin, where NeoForge fires its NeighborNotifyEvent): a table
     * that is gone, broken, blown up or replaced, drops what lay on it at once.
     */
    public static void neighbourNotify(ServerLevel level, BlockPos pos) {
        if (!TableGrid.isTable(level.getBlockState(pos)) && TableStore.get(level).get(pos) != null) {
            TableActions.spill(level, pos);
        }
    }

    /** After the chunk packet (PlayerChunkSenderMixin, NeoForge's ChunkWatchEvent.Sent): contents never come before blocks. */
    public static void chunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.inChunk(chunk.getPos())) {
            Network.sendToPlayer(player, TableActions.contents(pos, store.get(pos)));
        }
        FurnaceWatch.chunkSent(player, chunk);
        BrewingWatch.chunkSent(player, chunk);
    }
}
