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
        PayloadTypeRegistry.playC2S().register(StationActionPayload.TYPE, StationActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TableContentsPayload.TYPE, TableContentsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FurnaceContentsPayload.TYPE, FurnaceContentsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FurnaceStatePayload.TYPE, FurnaceStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantDisplayPayload.TYPE, EnchantDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantFxPayload.TYPE, EnchantFxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MenuDisplayPayload.TYPE, MenuDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MenuFxPayload.TYPE, MenuFxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SessionClosePayload.TYPE, SessionClosePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AtStationPayload.TYPE, AtStationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StationActionPayload.TYPE, (payload, context) -> Sessions.handle(context.player(), payload));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, AtStationPayload.TYPE)) {
                handler.disconnect(Component.literal("Cinematic Interactions must be installed on both the client and server."));
            }
        });
    }
}
