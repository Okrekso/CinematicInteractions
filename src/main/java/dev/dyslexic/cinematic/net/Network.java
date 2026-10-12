package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.station.Sessions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    /** Client handlers are routed through lambdas so a dedicated server never loads client classes. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("7");
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
