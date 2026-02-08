package com.mario.hardcorelimbo.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.mario.hardcorelimbo.HardcoreLimbo;
import com.mario.hardcorelimbo.database.DatabaseManager;
import com.mario.hardcorelimbo.model.PlayerData;
import com.mario.hardcorelimbo.util.MessageUtil;
import com.mario.hardcorelimbo.util.ServerTransferUtil;

/**
 * Handles all events on the MAIN (survival) server:
 *
 * - Player death: decrement lives, send to Limbo if 0
 * - Player join: check DB, redirect dead players to Limbo
 * - HRM integration: detect gamemode change from SPECTATOR -> SURVIVAL
 */
public class MainServerListener implements Listener {

    private final HardcoreLimbo plugin;
    private final DatabaseManager db;

    /**
     * Players currently being processed for death/limbo transfer.
     * Prevents race conditions with rapid events.
     */
    private final Set<UUID> pendingLimbo = new HashSet<>();

    /**
     * Players we expect gamemode changes for (our own changes).
     * Prevents false-positive HRM detection.
     */
    private final Set<UUID> expectedGamemodeChanges = new HashSet<>();

    public MainServerListener(HardcoreLimbo plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    // -------------------------------------------------------
    // Player Join - Check if they should be in Limbo
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Bypass permission check
        if (player.hasPermission("hardcorelimbo.bypass")) {
            plugin.debug(player.getName() + " has bypass permission, skipping checks.");
            return;
        }

        // Run DB lookup async to not block the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayer(player.getUniqueId());

            if (data == null) {
                // First time player - create their record
                data = PlayerData.createNew(player.getUniqueId(), player.getName(),
                        plugin.getDefaultLives());
                db.savePlayer(data);
                plugin.debug("Created new player record for " + player.getName());

                // Notify about grace period on main thread
                final PlayerData finalData = data;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        String timeRemaining = finalData.getGraceTimeRemaining(
                                plugin.getGracePeriodHours());
                        player.sendMessage(MessageUtil.get("death-grace-period",
                                "time_remaining", timeRemaining));
                    }
                });
                return;
            }

            // Update username if changed
            if (!data.getUsername().equals(player.getName())) {
                data.setUsername(player.getName());
                db.savePlayer(data);
            }

            // Check if player is dead -> redirect to Limbo
            if (data.isDead()) {
                plugin.debug(player.getName() + " is dead, sending to Limbo.");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(MessageUtil.get("death-sent-to-limbo"));
                        // Small delay so they see the message before transfer
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                ServerTransferUtil.sendToLimbo(player);
                            }
                        }, 20L); // 1 second
                    }
                });
            }
        });
    }

    // -------------------------------------------------------
    // Player Death - Core hardcore logic
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Bypass permission
        if (player.hasPermission("hardcorelimbo.bypass")) return;

        UUID uuid = player.getUniqueId();

        // Prevent duplicate processing
        if (pendingLimbo.contains(uuid)) return;

        // Process async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayer(uuid);

            if (data == null) {
                // Should never happen (created on join), but handle gracefully
                data = PlayerData.createNew(uuid, player.getName(), plugin.getDefaultLives());
            }

            // Check grace period
            if (data.isInGracePeriod(plugin.getGracePeriodHours())) {
                String timeRemaining = data.getGraceTimeRemaining(plugin.getGracePeriodHours());
                final String msg = MessageUtil.get("death-grace-period",
                        "time_remaining", timeRemaining);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(msg);
                    }
                });
                return; // No life lost during grace period
            }

            // Decrement a life
            int remainingLives = data.decrementLife();
            db.savePlayer(data);

            plugin.debug(player.getName() + " died. Lives remaining: " + remainingLives +
                    ", isDead: " + data.isDead());

            if (data.isDead()) {
                // OUT OF LIVES -> Send to Limbo
                handleFinalDeath(player, uuid);
            } else if (remainingLives == 1) {
                // Last life warning
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(MessageUtil.get("death-last-life"));
                    }
                });
            } else {
                // Normal death - life lost
                final int lives = remainingLives;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(MessageUtil.get("death-life-lost",
                                "lives", lives));
                    }
                });
            }
        });
    }

    /**
     * Handles the transition when a player runs out of lives.
     * Sets them to spectator mode and schedules the transfer to Limbo.
     */
    private void handleFinalDeath(Player player, UUID uuid) {
        pendingLimbo.add(uuid);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                pendingLimbo.remove(uuid);
                return;
            }

            // Send the dramatic death message
            player.sendMessage(MessageUtil.get("death-sent-to-limbo"));

            // Optionally set to spectator mode to prevent "You Died" screen issues
            if (plugin.isSpectatorOnDeath()) {
                expectedGamemodeChanges.add(uuid);
                player.setGameMode(GameMode.SPECTATOR);
            }

            // Schedule the transfer to Limbo after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    ServerTransferUtil.sendToLimbo(player);
                }
                pendingLimbo.remove(uuid);
                expectedGamemodeChanges.remove(uuid);
            }, plugin.getSendToLimboDelayTicks());
        });
    }

    // -------------------------------------------------------
    // Player Respawn - Set to spectator if dead (backup)
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // If this player is pending limbo transfer, set spectator
        if (pendingLimbo.contains(uuid) && plugin.isSpectatorOnDeath()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && pendingLimbo.contains(uuid)) {
                    expectedGamemodeChanges.add(uuid);
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }, 1L);
        }
    }

    // -------------------------------------------------------
    // HRM Integration - Detect ritual revive via gamemode change
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!plugin.isDetectHrmRevive()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Ignore our own gamemode changes
        if (expectedGamemodeChanges.remove(uuid)) return;

        // Detect SPECTATOR -> SURVIVAL (this is what HRM does when reviving)
        if (player.getGameMode() == GameMode.SPECTATOR
                && event.getNewGameMode() == GameMode.SURVIVAL) {

            plugin.debug("Detected gamemode change SPECTATOR->SURVIVAL for " +
                    player.getName() + " (possible HRM revive)");

            // Check DB async - if they were dead, mark as revived
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerData data = db.getPlayer(uuid);
                if (data != null && data.isDead()) {
                    plugin.getLogger().info("HRM revive detected for " + player.getName() +
                            "! Updating database.");
                    db.revivePlayer(uuid, plugin.getLivesOnRevive());
                }
            });
        }
    }
}
