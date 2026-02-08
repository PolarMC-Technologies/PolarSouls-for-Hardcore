package com.mario.hardcorelimbo.util;

import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mario.hardcorelimbo.HardcoreLimbo;

/**
 * Sends players between servers via the BungeeCord plugin messaging channel.
 *
 * Works with Velocity when 'bungee-plugin-message-channel' is enabled
 * in velocity.toml (enabled by default).
 */
public final class ServerTransferUtil {

    private ServerTransferUtil() {}

    /**
     * Sends a player to another server on the proxy network.
     *
     * @param player     the player to transfer
     * @param serverName the target server name (as configured in velocity.toml)
     */
    public static void sendToServer(Player player, String serverName) {
        HardcoreLimbo plugin = HardcoreLimbo.getInstance();
        plugin.debug("Sending " + player.getName() + " to server: " + serverName);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Sends a player to the Limbo server.
     */
    public static void sendToLimbo(Player player) {
        sendToServer(player, HardcoreLimbo.getInstance().getLimboServerName());
    }

    /**
     * Sends a player to the Main server.
     */
    public static void sendToMain(Player player) {
        sendToServer(player, HardcoreLimbo.getInstance().getMainServerName());
    }
}
