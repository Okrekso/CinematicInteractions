package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.station.Sessions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

public final class Network {
    private Network() {}

    /**
     * NeoForge negotiates one protocol version for the whole channel set ("11" there). Fabric negotiates each payload id
     * separately, so a client and a server of different builds keep only the ids both know; a changed format is a new id.
     */
    public static boolean accepts(ServerPlayer player, CustomPacketPayload payload) {
        return clientHasMod(player) && ServerPlayNetworking.canSend(player, payload.type());
    }

    /** True when this player's client runs this build's format of the mod ({@link ProtocolPayload}). */
    public static boolean clientHasMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, ProtocolPayload.TYPE);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (accepts(player, payload)) ServerPlayNetworking.send(player, payload);
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

    /** NeoForgeStreamCodecs.enumCodec: the constant's ordinal as a varint. */
    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> enumCodec(Class<E> type) {
        return StreamCodec.of((buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(type));
    }

    /** Both directions are registered on both sides; the client's receivers are added by CinematicClient. */
    public static void register() {
        PayloadTypeRegistry.playC2S().register(ProtocolPayload.TYPE, ProtocolPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProtocolPayload.TYPE, ProtocolPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (payload, context) -> {});
        PayloadTypeRegistry.playC2S().register(StationActionPayload.TYPE, StationActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TableContentsPayload.TYPE, TableContentsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FurnaceContentsPayload.TYPE, FurnaceContentsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BrewingContentsPayload.TYPE, BrewingContentsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FurnaceStatePayload.TYPE, FurnaceStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CampfireProgressPayload.TYPE, CampfireProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantDisplayPayload.TYPE, EnchantDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantFxPayload.TYPE, EnchantFxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MenuDisplayPayload.TYPE, MenuDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MenuFxPayload.TYPE, MenuFxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SessionClosePayload.TYPE, SessionClosePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AtStationPayload.TYPE, AtStationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StationActionPayload.TYPE, (payload, context) -> Sessions.handle(context.player(), payload));
    }
}
