//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/^* The client's way to the server, under the name the NeoForge sources use; only client code calls it. ^/
public final class PacketDistributor {
    private PacketDistributor() {}

    /^* Without the mod on the server there is nobody to receive it (a client-only connection): nothing is sent. ^/
    public static void sendToServer(CustomPacketPayload payload) {
        if (serverHasMod() && ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
    }

    /^* True when the server runs this build's format of the mod ({@link ProtocolPayload}). ^/
    public static boolean serverHasMod() {
        return ClientPlayNetworking.canSend(ProtocolPayload.TYPE);
    }
}
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;

/^* The client's way to the server, under the name the NeoForge sources use; only client code calls it. ^/
public final class PacketDistributor {
    private PacketDistributor() {}

    /^* Without the mod on the server there is nobody to receive it (a client-only connection): nothing is sent. ^/
    public static void sendToServer(FabricPacket payload) {
        if (serverHasMod() && ClientPlayNetworking.canSend(payload.getType())) ClientPlayNetworking.send(payload);
    }

    /^* True when the server runs this build's format of the mod ({@link ProtocolPayload}). ^/
    public static boolean serverHasMod() {
        return ClientPlayNetworking.canSend(ProtocolPayload.TYPE);
    }
}
*///?}
