package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.station.Sessions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    public static boolean accepts(ServerPlayer player, CustomPacketPayload payload) {
        return NetworkRegistry.hasChannel(player.connection, payload.type().id());
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (accepts(player, payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, CustomPacketPayload payload) {
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) sendToPlayer(player, payload);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false))
            if (player != entity) sendToPlayer(player, payload);
        if (entity instanceof ServerPlayer player) sendToPlayer(player, payload);
    }

    /** Client handlers are routed through lambdas so a dedicated server never loads client classes. */
    public static void register(RegisterPayloadHandlersEvent event) {
        // Missing channels are expected when a client uses only vanilla menus on an unmodded server.
        PayloadRegistrar registrar = event.registrar("8").optional();
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                Sessions.handle(player, payload);
            }
        });
        registrar.playToClient(TableContentsPayload.TYPE, TableContentsPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.tableContents(payload));
        registrar.playToClient(FurnaceContentsPayload.TYPE, FurnaceContentsPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.furnaceContents(payload));
        registrar.playToClient(FurnaceStatePayload.TYPE, FurnaceStatePayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.furnaceState(payload));
        registrar.playToClient(CampfireProgressPayload.TYPE, CampfireProgressPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.campfireProgress(payload));
        registrar.playToClient(EnchantDisplayPayload.TYPE, EnchantDisplayPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.enchantDisplay(payload));
        registrar.playToClient(EnchantFxPayload.TYPE, EnchantFxPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.enchantFx(payload));
        registrar.playToClient(MenuDisplayPayload.TYPE, MenuDisplayPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.menuDisplay(payload));
        registrar.playToClient(MenuFxPayload.TYPE, MenuFxPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.menuFx(payload));
        registrar.playToClient(SessionClosePayload.TYPE, SessionClosePayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.close(payload));
        registrar.playToClient(AtStationPayload.TYPE, AtStationPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.atStation(payload));
    }
}
