package com.mario.hardcorelimbo.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import com.mario.hardcorelimbo.HardcoreLimbo;
import com.mario.hardcorelimbo.util.MessageUtil;
import com.mario.hardcorelimbo.util.ServerTransferUtil;

/**
 * Handles all events on the LIMBO server:
 *
 * - Player join: lock to adventure mode, clear inventory, tp to spawn
 * - Block commands (except whitelisted ones)
 * - Prevent damage, portals, and escape attempts
 */
public class LimboServerListener implements Listener {

    private final HardcoreLimbo plugin;

    public LimboServerListener(HardcoreLimbo plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------
    // Player Join - Full lockdown
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Bypass for admins
        if (player.hasPermission("hardcorelimbo.bypass")) {
            plugin.debug(player.getName() + " has bypass, skipping limbo lockdown.");
            return;
        }

        // Check if the player is actually dead (they might be an admin visiting)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isDead = plugin.getDatabaseManager().isPlayerDead(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (isDead) {
                    // Apply full Limbo lockdown
                    applyLimboState(player);
                } else {
                    // Player is NOT dead - they shouldn't be here, send to Main
                    plugin.debug(player.getName() + " is alive but on Limbo, sending to Main.");
                    player.sendMessage(MessageUtil.get("revive-success"));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ServerTransferUtil.sendToMain(player);
                        }
                    }, 40L); // 2 second delay
                }
            });
        });
    }

    /**
     * Locks a player into the Limbo state: adventure mode, clear inventory,
     * teleport to spawn, show welcome message.
     */
    private void applyLimboState(Player player) {
        // Set adventure mode
        player.setGameMode(GameMode.ADVENTURE);

        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Clear XP
        player.setExp(0);
        player.setLevel(0);

        // Full health and food (so they don't die in limbo)
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // Teleport to limbo spawn
        Location spawn = plugin.getLimboSpawn();
        if (spawn != null && spawn.getWorld() != null) {
            player.teleport(spawn);
        } else {
            // Fallback to world spawn
            player.teleport(player.getWorld().getSpawnLocation());
            plugin.getLogger().warning("Limbo spawn not set! Using world spawn. " +
                    "Use /setlimbospawn to configure.");
        }

        // Send the Limbo welcome message
        player.sendMessage(MessageUtil.getNoPrefix("limbo-welcome"));

        plugin.debug("Applied limbo state to " + player.getName());
    }

    // -------------------------------------------------------
    // Block most commands in Limbo
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Admins can use commands
        if (player.hasPermission("hardcorelimbo.bypass")) return;

        String command = event.getMessage().toLowerCase().split(" ")[0];

        // Whitelist some basic commands
        if (command.equals("/msg") || command.equals("/tell") ||
            command.equals("/r") || command.equals("/reply") ||
            command.equals("/help") || command.equals("/list") ||
            command.equals("/hlstatus")) {
            return;
        }

        // Block everything else
        event.setCancelled(true);
        player.sendMessage(MessageUtil.get("limbo-cannot-leave"));
    }

    // -------------------------------------------------------
    // Prevent all damage in Limbo
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------
    // Prevent portal usage
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hardcorelimbo.bypass")) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.get("limbo-cannot-leave"));
        }
    }

    // -------------------------------------------------------
    // Optional: Prevent leaving a defined area (world border)
    // If you want to confine players to a specific area, you
    // can set up a world border on the Limbo server instead.
    // This is a lightweight fallback for void-fall prevention.
    // -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("hardcorelimbo.bypass")) return;

        // Prevent falling into the void
        if (event.getTo() != null && event.getTo().getY() < -64) {
            Location spawn = plugin.getLimboSpawn();
            if (spawn != null && spawn.getWorld() != null) {
                player.teleport(spawn);
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }
}
