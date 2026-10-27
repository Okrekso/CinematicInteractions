package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.enchant.EnchantActions;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
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

public final class StationEvents {
    private StationEvents() {}

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
            }
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
            }
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
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        TableStore store = TableStore.get(event.getLevel());
        for (BlockPos pos : store.inChunk(event.getPos())) {
            dev.dyslexic.cinematic.net.Network.sendToPlayer(event.getPlayer(), TableActions.contents(pos, store.get(pos)));
        }
        FurnaceWatch.chunkSent(event.getPlayer(), event.getChunk());
        dev.dyslexic.cinematic.brewing.BrewingWatch.chunkSent(event.getPlayer(), event.getChunk());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sessions.tick(player);
        }
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
    }
}
