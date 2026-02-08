package com.mario.hardcorelimbo.task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mario.hardcorelimbo.HardcoreLimbo;
import com.mario.hardcorelimbo.util.MessageUtil;
import com.mario.hardcorelimbo.util.ServerTransferUtil;

/**
 * Runs periodically on the LIMBO server (async).
 *
 * Checks each online player against the database.
 * If a player's is_dead flag is now FALSE (they've been revived),
 * schedules them to be transferred back to the Main server.
 */
public class LimboCheckTask extends BukkitRunnable {

    private final HardcoreLimbo plugin;

    public LimboCheckTask(HardcoreLimbo plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Collect online players (snapshot, since we're async)
        List<UUID> onlinePlayers = new ArrayList<>();
        List<String> onlineNames = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip admins with bypass
            if (player.hasPermission("hardcorelimbo.bypass")) continue;
            onlinePlayers.add(player.getUniqueId());
            onlineNames.add(player.getName());
        }

        if (onlinePlayers.isEmpty()) return;

        plugin.debug("Limbo check: scanning " + onlinePlayers.size() + " player(s)...");

        // Check each player against the database
        List<UUID> toRelease = new ArrayList<>();

        for (int i = 0; i < onlinePlayers.size(); i++) {
            UUID uuid = onlinePlayers.get(i);
            boolean isDead = plugin.getDatabaseManager().isPlayerDead(uuid);

            if (!isDead) {
                toRelease.add(uuid);
                plugin.debug("Player " + onlineNames.get(i) + " has been revived! Releasing...");
            }
        }

        // Release revived players on the main thread
        if (!toRelease.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : toRelease) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        releasePlayer(player);
                    }
                }
            });
        }
    }

    /**
     * Notifies and transfers a revived player back to the Main server.
     * Must be called on the main thread.
     */
    private void releasePlayer(Player player) {
        plugin.getLogger().info("Releasing " + player.getName() + " from Limbo!");

        // Send the revive message
        player.sendMessage(MessageUtil.get("revive-success"));

        // Transfer after a short delay (so they can read the message)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ServerTransferUtil.sendToMain(player);
            }
        }, 40L); // 2 seconds
    }
}
