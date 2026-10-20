package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.Sessions;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The mod's channel. Both sides accept a peer without it ({@link NetworkRegistry#acceptMissingOr}): a client with the mod
 * joins a server without it (vanilla menus, or the client-only scenes) and a client without the mod joins a server with
 * it. Nothing is sent to a peer that did not negotiate the channel.
 */
public final class Network {
    private Network() {}

    /** Raised whenever the format of a message changes. */
    public static final String VERSION = "10";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Cinematic.id("main"), () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    private static int nextId;

    /** Whether this player's client has the channel (a client without the mod gets nothing from us). */
    public static boolean accepts(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        if (accepts(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, Payload payload) {
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) sendToPlayer(player, payload);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, Payload payload) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false))
            if (player != entity) sendToPlayer(player, payload);
        if (entity instanceof ServerPlayer player) sendToPlayer(player, payload);
    }

    /** Client handlers are routed through lambdas so a dedicated server never loads client classes. */
    public static void register() {
        toServer(StationActionPayload.class, StationActionPayload::read, (payload, player) -> Sessions.handle(player, payload));
        toClient(TableContentsPayload.class, TableContentsPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.tableContents(payload));
        toClient(FurnaceContentsPayload.class, FurnaceContentsPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.furnaceContents(payload));
        toClient(BrewingContentsPayload.class, BrewingContentsPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientBrewing.apply(payload));
        toClient(FurnaceStatePayload.class, FurnaceStatePayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.furnaceState(payload));
        toClient(CampfireProgressPayload.class, CampfireProgressPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.campfireProgress(payload));
        toClient(EnchantDisplayPayload.class, EnchantDisplayPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.enchantDisplay(payload));
        toClient(EnchantFxPayload.class, EnchantFxPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.enchantFx(payload));
        toClient(MenuDisplayPayload.class, MenuDisplayPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.menuDisplay(payload));
        toClient(MenuFxPayload.class, MenuFxPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.menuFx(payload));
        toClient(SessionClosePayload.class, SessionClosePayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.close(payload));
        toClient(AtStationPayload.class, AtStationPayload::read,
                payload -> dev.dyslexic.cinematic.client.ClientPayloads.atStation(payload));
    }

    private static <T extends Payload> void toServer(Class<T> type, Function<FriendlyByteBuf, T> read,
            java.util.function.BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Payload::write).decoder(read)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) handler.accept(payload, player);
                })
                .add();
    }

    private static <T extends Payload> void toClient(Class<T> type, Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Payload::write).decoder(read)
                .consumerMainThread((payload, context) -> handler.accept(payload))
                .add();
    }

    /** Whether a connection (client side: to the server) carries the channel. */
    public static boolean present(Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }
}
