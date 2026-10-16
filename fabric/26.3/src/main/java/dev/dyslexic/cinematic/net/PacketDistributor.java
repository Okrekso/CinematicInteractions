package dev.dyslexic.cinematic.net;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/** The scene's audience is the set of players tracking the block's chunk. */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(level, chunk)) sendToPlayer(player, payload);
    }

    public static void sendToPlayersTrackingEntityAndSelf(ServerPlayer entity, CustomPacketPayload payload) {
        sendToPlayer(entity, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(entity)) sendToPlayer(watcher, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
    }
}
