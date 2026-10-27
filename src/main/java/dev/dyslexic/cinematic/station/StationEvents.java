package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.ClientBridge;
//? if neoforge {
import dev.dyslexic.cinematic.enchant.EnchantActions;
import dev.dyslexic.cinematic.net.StationActionPayload;
//?} else if fabric {
/*import dev.dyslexic.cinematic.brewing.BrewingWatch;
import dev.dyslexic.cinematic.campfire.CampfireActions;
*///?} else if forge {
/*import dev.dyslexic.cinematic.enchant.EnchantActions;
import dev.dyslexic.cinematic.net.StationActionPayload;
*///?}
import dev.dyslexic.cinematic.furnace.FurnaceWatch;
//? if fabric {
/*import dev.dyslexic.cinematic.net.Network;
*///?}
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
//? if fabric {
/*import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if neoforge {
import net.minecraft.world.entity.player.Player;
//?} else if forge {
/*import net.minecraft.world.entity.player.Player;
*///?}
import net.minecraft.world.level.block.state.BlockState;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
//?} else if fabric {
/*import net.minecraft.world.level.chunk.LevelChunk;
*///?} else if forge {
/*import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?}

/** The NeoForge events of the original, as Fabric API callbacks and the mixins that stand in for the missing ones. */
public final class StationEvents {
    private StationEvents() {}

    //? if neoforge {
    /**
     * Using a station starts its scene instead of the menu. Sneaking keeps the vanilla behaviour (place a block,
     * or the classic window with empty hands), and so does a furnace whose front is walled in.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        StationKind kind = StationKind.of(state);
        if (kind == null || !kind.startsOnUse() || event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock() == TriState.FALSE
                || player.isSecondaryUseActive() || player.isSpectator() || !kind.usable(event.getLevel(), event.getPos(), state)
                || !kind.usedOn(state, event.getHitVec().getDirection())) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            if (ClientBridge.vanillaUse.test(event.getPos().immutable(), kind)) {
                return;
    //?} else if fabric {
    /*public static void register() {
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
    *///?} else if forge {
    /*/^*
     * Using a station starts its scene instead of the menu. Sneaking keeps the vanilla behaviour (place a block,
     * or the classic window with empty hands), and so does a furnace whose front is walled in.
     ^/
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        StationKind kind = StationKind.of(state);
        if (kind == null || !kind.startsOnUse() || event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock() == Event.Result.DENY
                || player.isSecondaryUseActive() || player.isSpectator() || !kind.usable(event.getLevel(), event.getPos(), state)
                || !kind.usedOn(state, event.getHitVec().getDirection())) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            if (ClientBridge.vanillaUse.test(event.getPos().immutable(), kind)) {
                return;
            }
        } else if (player instanceof ServerPlayer serverPlayer && !dev.dyslexic.cinematic.net.Network.accepts(serverPlayer)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getLevel().isClientSide()) {
            ClientBridge.useAt = event.getHitVec().getLocation();
            ClientBridge.useStation.accept(event.getPos().immutable(), kind);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !TableGrid.isTable(event.getState())
                && TableStore.get(level).get(event.getPos()) != null) {
            TableActions.spill(level, event.getPos());
        }
    }

    /^* Repairs a lost cooking timer before the vanilla campfire ticker can treat zero as "finished". ^/
    @SubscribeEvent
    public static void onLevelTickPre(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Sessions.Session session = Sessions.get(player);
            if (session != null && session.kind() == StationKind.CAMPFIRE && session.dimension().equals(level.dimension())) {
                dev.dyslexic.cinematic.campfire.CampfireActions.tick(level, session);
    *///?}
            }
        //? if neoforge {
        } else if (player instanceof ServerPlayer serverPlayer
                && !NetworkRegistry.hasChannel(serverPlayer.connection, StationActionPayload.TYPE.id())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getLevel().isClientSide()) {
            ClientBridge.useAt = event.getHitVec().getLocation();
            ClientBridge.useStation.accept(event.getPos().immutable(), kind);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !TableGrid.isTable(event.getState())
                && TableStore.get(level).get(event.getPos()) != null) {
            TableActions.spill(level, event.getPos());
        }
    }

    /** Repairs a lost cooking timer before the vanilla campfire ticker can treat zero as "finished". */
    @SubscribeEvent
    public static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Sessions.Session session = Sessions.get(player);
            if (session != null && session.kind() == StationKind.CAMPFIRE && session.dimension().equals(level.dimension())) {
                dev.dyslexic.cinematic.campfire.CampfireActions.tick(level, session);
        //?} else if fabric {
            /*if (ClientBridge.vanillaUse.test(hit.getBlockPos().immutable(), kind)) return InteractionResult.PASS;
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
        *///?} else if forge {
        /*}
    }

    /^* Catches tables removed without a neighbour update (pistons, some commands and mods). ^/
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 40 != 0) {
            return;
        }
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.positions()) {
            if (level.isLoaded(pos) && !TableGrid.isTable(level.getBlockState(pos))) {
                TableActions.spill(level, pos);
        *///?}
            }
        //? if neoforge {
        }
    }

    /** Catches tables removed without a neighbour update (pistons, some commands and mods). */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 40 != 0) {
            return;
        }
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.positions()) {
            if (level.isLoaded(pos) && !TableGrid.isTable(level.getBlockState(pos))) {
                TableActions.spill(level, pos);
            }
        }
        //?} else if fabric {
        /*});
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
        *///?} else if forge {
        /*}
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Watch event) {
        TableStore store = TableStore.get(event.getLevel());
        for (BlockPos pos : store.inChunk(event.getPos())) {
            dev.dyslexic.cinematic.net.Network.sendToPlayer(event.getPlayer(), TableActions.contents(pos, store.get(pos)));
        }
        FurnaceWatch.chunkSent(event.getPlayer(), event.getChunk());
        dev.dyslexic.cinematic.brewing.BrewingWatch.chunkSent(event.getPlayer(), event.getChunk());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            Sessions.tick(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher) {
            Sessions.sendAtStation(target, watcher);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, false);
        }
        *///?}
    }

    //? if neoforge {
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        TableStore store = TableStore.get(event.getLevel());
        for (BlockPos pos : store.inChunk(event.getPos())) {
            dev.dyslexic.cinematic.net.Network.sendToPlayer(event.getPlayer(), TableActions.contents(pos, store.get(pos)));
    //?} else if fabric {
    /*/^*
     * A block changed and tells its neighbours (ServerLevelMixin, where NeoForge fires its NeighborNotifyEvent): a table
     * that is gone, broken, blown up or replaced, drops what lay on it at once.
     ^/
    public static void neighbourNotify(ServerLevel level, BlockPos pos) {
        if (!TableGrid.isTable(level.getBlockState(pos)) && TableStore.get(level).get(pos) != null) {
            TableActions.spill(level, pos);
    *///?} else if forge {
    /*@SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, true);
    *///?}
        }
        //? if neoforge {
        FurnaceWatch.chunkSent(event.getPlayer(), event.getChunk());
        dev.dyslexic.cinematic.brewing.BrewingWatch.chunkSent(event.getPlayer(), event.getChunk());
        //?}
    }

    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.tick(player);
    //?} else if fabric {
    /*/^* After the chunk packet (PlayerChunkSenderMixin, NeoForge's ChunkWatchEvent.Sent): contents never come before blocks. ^/
    public static void chunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.inChunk(chunk.getPos())) {
            Network.sendToPlayer(player, TableActions.contents(pos, store.get(pos)));
    *///?} else if forge {
    /*@SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, true);
    *///?}
        }
    //? if neoforge {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher) {
            Sessions.sendAtStation(target, watcher);
        }
    }

    /** A crafting window opened at a table (another mod, a sneaking use, the scene off): it takes what lies on the table. */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof net.minecraft.world.inventory.CraftingMenu menu) {
            dev.dyslexic.cinematic.table.TableActions.intoWindow(player, menu);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, false);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.end(player, true);
        }
    }

    @SubscribeEvent
    public static void onEnchant(PlayerEnchantItemEvent event) {
        EnchantActions.onEnchant(event);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Sessions.clear();
    //?} else if fabric {
        /*FurnaceWatch.chunkSent(player, chunk);
        BrewingWatch.chunkSent(player, chunk);
    *///?} else if forge {
    /*}

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Sessions.clear();
    *///?}
    }
}
