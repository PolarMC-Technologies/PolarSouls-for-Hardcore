package com.mario.polarsouls.task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.util.MessageUtil;

// pings the db on main server for spectators who've been revived externally and then restores em to survival

public class MainReviveCheckTask extends BukkitRunnable {

    private static final String PERM_BYPASS = "PolarSouls.bypass";

    private final PolarSouls plugin;

    public MainReviveCheckTask(PolarSouls plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<UUID> spectators = collectDeadSpectators();
        if (spectators.isEmpty()) return;

        // Avoid string concatenation overhead unless debug is enabled
        if (plugin.isDebugMode()) {
            plugin.debug("Main revive check: scanning " + spectators.size() + " spectator(s)...");
        }

        List<UUID> revived = findRevivedPlayers(spectators);

        if (!revived.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> restoreAll(revived));
        }
    }

    private List<UUID> collectDeadSpectators() {
        List<UUID> deadSpectatorUuids = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR
                    && !player.hasPermission(PERM_BYPASS)) {
                UUID uuid = player.getUniqueId();
                // Check the database (with its internal cache) to determine if the player is dead
                if (plugin.getDatabaseManager().isPlayerDead(uuid)) {
                    deadSpectatorUuids.add(uuid);
                }
            }
        }
        return deadSpectatorUuids;
    }

    private List<UUID> findRevivedPlayers(List<UUID> spectators) {
        List<UUID> revived = new ArrayList<>();
        for (UUID uuid : spectators) {
            // Check if dead spectator is no longer dead (i.e., revived)
            if (!plugin.getDatabaseManager().isPlayerDead(uuid)) {
                revived.add(uuid);
                // Avoid string concatenation overhead unless debug is enabled
                if (plugin.isDebugMode()) {
                    plugin.debug("Spectator " + uuid + " is no longer dead in DB, restoring...");
                }
            }
        }
        return revived;
    }

    private void restoreAll(List<UUID> uuids) {
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(MessageUtil.get("revive-success"));
                plugin.getLogger().log(Level.INFO,
                        "Restored {0} from spectator to survival (revived in DB).",
                        player.getName());
            }
        }
    }
}
