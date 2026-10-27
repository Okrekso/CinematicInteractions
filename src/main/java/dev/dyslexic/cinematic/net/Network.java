package dev.dyslexic.cinematic.net;

//? if forge {
/*import dev.dyslexic.cinematic.Cinematic;
*///?}
import dev.dyslexic.cinematic.station.Sessions;
//? if neoforge {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else if fabric && >=1.21 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?} else if fabric && <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
*///?} else if forge {
/*import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
//? if neoforge {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//?} else if forge {
/*import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
*///?}

/**
 * The mod's channel. Both sides accept a peer without it ({@link NetworkRegistry#acceptMissingOr}): a client with the mod
 * joins a server without it (vanilla menus, or the client-only scenes) and a client without the mod joins a server with
 * it. Nothing is sent to a peer that did not negotiate the channel.
 */
public final class Network {
    private Network() {}

    //? if neoforge {
    public static boolean accepts(ServerPlayer player, CustomPacketPayload payload) {
        return NetworkRegistry.hasChannel(player.connection, payload.type().id());
    //?} else if fabric && >=1.21 {
    /*/^*
     * NeoForge negotiates one protocol version for the whole channel set ("11" there). Fabric negotiates each payload id
     * separately, so a client and a server of different builds keep only the ids both know; a changed format is a new id.
     ^/
    public static boolean accepts(ServerPlayer player, CustomPacketPayload payload) {
        return clientHasMod(player) && ServerPlayNetworking.canSend(player, payload.type());
    }

    /^* True when this player's client runs this build's format of the mod ({@link ProtocolPayload}). ^/
    public static boolean clientHasMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, ProtocolPayload.TYPE);
    *///?} else if fabric && <1.21 {
    /*/^*
     * NeoForge negotiates one protocol version for the whole channel set ("11" there). Fabric negotiates each payload id
     * separately, so a client and a server of different builds keep only the ids both know; a changed format is a new id.
     ^/
    public static boolean accepts(ServerPlayer player, FabricPacket payload) {
        return clientHasMod(player) && ServerPlayNetworking.canSend(player, payload.getType());
    }

    /^* True when this player's client runs this build's format of the mod ({@link ProtocolPayload}). ^/
    public static boolean clientHasMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, ProtocolPayload.TYPE);
    *///?} else if forge {
    /*/^* Raised whenever the format of a message changes. ^/
    public static final String VERSION = "12";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Cinematic.id("main"), () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    private static int nextId;

    /^* Whether this player's client has the channel (a client without the mod gets nothing from us). ^/
    public static boolean accepts(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    *///?}
    }

    //? if neoforge {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (accepts(player, payload)) PacketDistributor.sendToPlayer(player, payload);
    //?} else if fabric && >=1.21 {
    /*public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (accepts(player, payload)) ServerPlayNetworking.send(player, payload);
    *///?} else if fabric && <1.21 {
    /*public static void sendToPlayer(ServerPlayer player, FabricPacket payload) {
        if (accepts(player, payload)) ServerPlayNetworking.send(player, payload);
    *///?} else if forge {
    /*public static void sendToPlayer(ServerPlayer player, Payload payload) {
        if (accepts(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    *///?}
    }

    //? if >=1.21 {
    public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, CustomPacketPayload payload) {
    //?} else if fabric && <1.21 {
    /*public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, FabricPacket payload) {
    *///?} else if forge {
    /*public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, Payload payload) {
    *///?}
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) sendToPlayer(player, payload);
    }

    //? if >=1.21 {
    public static void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
    //?} else if fabric && <1.21 {
    /*public static void sendToTrackingEntityAndSelf(Entity entity, FabricPacket payload) {
    *///?} else if forge {
    /*public static void sendToTrackingEntityAndSelf(Entity entity, Payload payload) {
    *///?}
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false))
            if (player != entity) sendToPlayer(player, payload);
        if (entity instanceof ServerPlayer player) sendToPlayer(player, payload);
    }

    //? if neoforge {
    /** Client handlers are routed through lambdas so a dedicated server never loads client classes. */
    public static void register(RegisterPayloadHandlersEvent event) {
        // Missing channels are expected when a client uses only vanilla menus on an unmodded server.
        PayloadRegistrar registrar = event.registrar("11").optional();
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                Sessions.handle(player, payload);
            }
        });
        registrar.playToClient(TableContentsPayload.TYPE, TableContentsPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.tableContents(payload));
        registrar.playToClient(FurnaceContentsPayload.TYPE, FurnaceContentsPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientPayloads.furnaceContents(payload));
        registrar.playToClient(BrewingContentsPayload.TYPE, BrewingContentsPayload.CODEC,
                (payload, context) -> dev.dyslexic.cinematic.client.ClientBrewing.apply(payload));
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
    //?} else if fabric && >=1.21 {
    /*/^* NeoForgeStreamCodecs.enumCodec: the constant's ordinal as a varint. ^/
    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> enumCodec(Class<E> type) {
        return StreamCodec.of((buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(type));
    }

    /^* Both directions are registered on both sides; the client's receivers are added by CinematicClient. ^/
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
    *///?} else if fabric && <1.21 {
    /*/^* Fabric 0.92 packet types need no registry: each side only adds receivers (the client's are in CinematicClient). ^/
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (payload, player, sender) -> {});
        ServerPlayNetworking.registerGlobalReceiver(StationActionPayload.TYPE, (payload, player, sender) -> Sessions.handle(player, payload));
    *///?} else if forge {
    /*/^* Client handlers are routed through lambdas so a dedicated server never loads client classes. ^/
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

    /^* Whether a connection (client side: to the server) carries the channel. ^/
    public static boolean present(Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    *///?}
    }
}
