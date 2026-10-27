//? if forge {
/*package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.Payload;
import net.minecraft.client.Minecraft;

/^* The client's end of the mod's channel. ^/
public final class ClientNet {
    private ClientNet() {}

    /^* Whether the server we are connected to negotiated the channel (it has the mod). ^/
    public static boolean serverHasChannel() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && Network.present(connection.getConnection());
    }

    /^* To the server, only when it has the channel: a server without the mod is never sent anything of ours. ^/
    public static void sendToServer(Payload payload) {
        if (serverHasChannel()) Network.CHANNEL.sendToServer(payload);
    }
}
*///?}
