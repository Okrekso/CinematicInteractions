package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.station.Sessions;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
    public static boolean accepts(ServerPlayer player, FabricPacket payload) {
        return clientHasMod(player) && ServerPlayNetworking.canSend(player, payload.getType());
    }

    /** True when this player's client runs this build's format of the mod ({@link ProtocolPayload}). */
    public static boolean clientHasMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, ProtocolPayload.TYPE);
    }

    public static void sendToPlayer(ServerPlayer player, FabricPacket payload) {
        if (accepts(player, payload)) ServerPlayNetworking.send(player, payload);
    }

    public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, FabricPacket payload) {
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) sendToPlayer(player, payload);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, FabricPacket payload) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false))
            if (player != entity) sendToPlayer(player, payload);
        if (entity instanceof ServerPlayer player) sendToPlayer(player, payload);
    }

    /** Fabric 0.92 packet types need no registry: each side only adds receivers (the client's are in CinematicClient). */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (payload, player, sender) -> {});
        ServerPlayNetworking.registerGlobalReceiver(StationActionPayload.TYPE, (payload, player, sender) -> Sessions.handle(player, payload));
    }
}
