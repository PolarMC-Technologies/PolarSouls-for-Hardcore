package org.ssoggy.ssoggysouls.util;

import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.ssoggy.ssoggysouls.SSoggySouls;

public final class ServerTransferUtil {

    private ServerTransferUtil() {}

    public static void sendToServer(Player player, String serverName) {
        SSoggySouls plugin = SSoggySouls.getInstance();
        plugin.debug("Sending " + player.getName() + " to server: " + serverName);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendToLimbo(Player player) {
        sendToServer(player, SSoggySouls.getInstance().getLimboServerName());
    }

    public static void sendToMain(Player player) {
        sendToServer(player, SSoggySouls.getInstance().getMainServerName());
    }
}
