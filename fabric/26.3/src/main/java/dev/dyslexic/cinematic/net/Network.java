package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.station.Sessions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

public final class Network {
    private Network() {}

    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> enumCodec(Class<E> type) {
        return StreamCodec.of((buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(type));
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(RecipeSyncPayload.TYPE, RecipeSyncPayload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) server.getPlayerList().getPlayers().forEach(RecipeSyncPayload::send);
        });
        PayloadTypeRegistry.serverboundPlay().register(StationActionPayload.TYPE, StationActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TableContentsPayload.TYPE, TableContentsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FurnaceContentsPayload.TYPE, FurnaceContentsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FurnaceStatePayload.TYPE, FurnaceStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CampfireProgressPayload.TYPE, CampfireProgressPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantDisplayPayload.TYPE, EnchantDisplayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantFxPayload.TYPE, EnchantFxPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MenuDisplayPayload.TYPE, MenuDisplayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MenuFxPayload.TYPE, MenuFxPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SessionClosePayload.TYPE, SessionClosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AtStationPayload.TYPE, AtStationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StationActionPayload.TYPE, (payload, context) -> Sessions.handle(context.player(), payload));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, AtStationPayload.TYPE)) {
                handler.disconnect(Component.literal("Cinematic Interactions must be installed on both the client and server."));
            } else RecipeSyncPayload.send(handler.player);
        });
    }
}
