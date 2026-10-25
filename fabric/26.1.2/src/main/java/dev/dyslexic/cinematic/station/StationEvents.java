package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.campfire.CampfireActions;
import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.net.PacketDistributor;
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.LevelChunk;

public final class StationEvents {
    private StationEvents() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var state = level.getBlockState(hit.getBlockPos());
            StationKind kind = StationKind.of(state);
            if (kind == null || !kind.startsOnUse() || hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive()
                    || player.isSpectator() || !kind.usable(level, hit.getBlockPos(), state) || !kind.usedOn(state, hit.getDirection())) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                if (player instanceof ServerPlayer serverPlayer
                        && !ServerPlayNetworking.canSend(serverPlayer, dev.dyslexic.cinematic.net.RecipeSyncPayload.TYPE)) {
                    return InteractionResult.PASS;
                }
                // no swing shown to others, as with NeoForge's CONSUME
                return InteractionResult.CONSUME;
            }
            if (ClientBridge.vanillaUse.test(hit.getBlockPos(), kind)) return InteractionResult.PASS;
            ClientBridge.useAt = hit.getLocation();
            ClientBridge.useStation.accept(hit.getBlockPos().immutable(), kind);
            ClientBridge.swallowSwing = true;
            // SUCCESS has Fabric send vanilla's use packet to the server too; CONSUME would only cancel the client side
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) Sessions.tick(player);
        });
        // Repair a lost cooking timer before the vanilla campfire ticker can treat zero as "finished".
        ServerTickEvents.START_LEVEL_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                var session = Sessions.get(player);
                if (session != null && session.kind() == StationKind.CAMPFIRE
                        && session.dimension().equals(level.dimension())) CampfireActions.tick(level, session);
            }
        });
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level.getGameTime() % 40 != 0) return;
            for (BlockPos pos : TableStore.get(level).positions()) {
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
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> Sessions.end(player, true));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Sessions.clear());
    }

    /**
     * A block changed and tells its neighbours (as NeoForge's NeighborNotifyEvent): a table that is gone, broken, blown up
     * or replaced, drops what lay on it at once.
     */
    public static void neighbourNotify(ServerLevel level, BlockPos pos) {
        if (!TableGrid.isTable(level.getBlockState(pos)) && TableStore.get(level).get(pos) != null) {
            TableActions.spill(level, pos);
        }
    }

    /** Called after the chunk packet, so contents never arrive before the client's block data. */
    public static void chunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.inChunk(chunk.getPos())) PacketDistributor.sendToPlayer(player, TableActions.contents(pos, store.get(pos)));
        FurnaceWatch.chunkSent(player, chunk);
        dev.dyslexic.cinematic.brewing.BrewingWatch.chunkSent(player, chunk);
    }
}
