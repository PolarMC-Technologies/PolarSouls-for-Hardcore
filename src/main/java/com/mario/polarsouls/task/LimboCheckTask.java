package com.mario.polarsouls.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.ServerTransferUtil;

public class LimboCheckTask extends BukkitRunnable {

    private static final String PERM_BYPASS = "PolarSouls.bypass";

    private final PolarSouls plugin;

    public LimboCheckTask(PolarSouls plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<UUID> onlinePlayers = collectOnlinePlayers();
        if (onlinePlayers.isEmpty()) return;

        // Avoid string concatenation overhead unless debug is enabled
        if (plugin.isDebugMode()) {
            plugin.debug("Limbo check: scanning " + onlinePlayers.size() + " player(s)...");
        }

        List<UUID> toRelease = findRevivedPlayers(onlinePlayers);

        if (!toRelease.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> releaseAll(toRelease));
        }
    }

    private List<UUID> collectOnlinePlayers() {
        List<UUID> players = new ArrayList<>();
        Set<UUID> deadPlayers = plugin.getLimboDeadPlayers();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!player.hasPermission(PERM_BYPASS) && deadPlayers.contains(uuid)) {
                players.add(uuid);
            }
        }
        return players;
    }

    private List<UUID> findRevivedPlayers(List<UUID> onlinePlayers) {
        List<UUID> toRelease = new ArrayList<>();
        for (UUID uuid : onlinePlayers) {
            if (!plugin.getDatabaseManager().isPlayerDead(uuid)) {
                toRelease.add(uuid);
                // Avoid string concatenation overhead unless debug is enabled
                if (plugin.isDebugMode()) {
                    plugin.debug("Player " + uuid + " has been revived! Releasing...");
                }
            }
        }
        return toRelease;
    }

    private void releaseAll(List<UUID> uuids) {
        Set<UUID> deadPlayers = plugin.getLimboDeadPlayers();
        for (UUID uuid : uuids) {
            deadPlayers.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                releasePlayer(player);
            }
        }
    }

    private void releasePlayer(Player player) {
        plugin.getLogger().log(Level.INFO, "Releasing {0} from Limbo!", player.getName());

        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.sendMessage(MessageUtil.get("revive-success"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ServerTransferUtil.sendToMain(player);
            }
        }, 40L);
    }
}
