package com.mario.polarsouls.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.util.MessageUtil;

public class LimboServerListener implements Listener {

    private static final String PERM_BYPASS = "PolarSouls.bypass";

    private final PolarSouls plugin;
    
    // Cache limbo spawn location to avoid repeated lookups
    private Location cachedLimboSpawn;

    public LimboServerListener(PolarSouls plugin) {
        this.plugin = plugin;
        refreshLimboSpawnCache();
    }
    
    /**
     * Refresh cached limbo spawn (call on config reload or spawn change)
     */
    public void refreshLimboSpawnCache() {
        this.cachedLimboSpawn = plugin.getLimboSpawn();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(PERM_BYPASS)) {
            if (plugin.isDebugMode()) {
                plugin.debug(player.getName() + " has bypass, skipping limbo lockdown.");
            }
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isDead = plugin.getDatabaseManager().isPlayerDead(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (isDead) {
                    plugin.getLimboDeadPlayers().add(player.getUniqueId());
                    applyLimboState(player);
                } else {
                    if (plugin.isDebugMode()) {
                        plugin.debug(player.getName() + " is alive, visiting Limbo.");
                    }
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(MessageUtil.get("limbo-visitor-welcome"));
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLimboDeadPlayers().remove(event.getPlayer().getUniqueId());
    }

    private void applyLimboState(Player player) {
        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.setExp(0);
        player.setLevel(0);

        var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : 20.0;
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20f);

        Location spawn = cachedLimboSpawn; // Use cached value
        if (spawn != null && spawn.getWorld() != null) {
            player.teleport(findSafeLocation(spawn));
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
            plugin.getLogger().warning("Limbo spawn not set! Using world spawn. "
                    + "Use /setlimbospawn to configure.");
        }

        player.sendMessage(MessageUtil.getNoPrefix("limbo-welcome"));
        
        if (plugin.isDebugMode()) {
            plugin.debug("Applied limbo state to " + player.getName());
        }
    }

    private static Location findSafeLocation(Location loc) {
        Location safe = loc.clone();
        org.bukkit.World world = safe.getWorld();
        if (world == null) return safe;

        int maxY = world.getMaxHeight();
        int safeBlockX = safe.getBlockX();
        int safeBlockZ = safe.getBlockZ();
        int startY = safe.getBlockY();

        for (int blockY = startY; blockY < maxY - 1; blockY++) {
            if (world.getBlockAt(safeBlockX, blockY, safeBlockZ).getType().isAir()
                    && world.getBlockAt(safeBlockX, blockY + 1, safeBlockZ).getType().isAir()) {
                safe.setY(blockY);
                return safe;
            }
        }
        // Fallback: use original + 2 to be above the block
        safe.setY(startY + 2.0);
        return safe;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(PERM_BYPASS)) return;
        if (player.hasPermission("PolarSouls.admin")) return;

        // visitors (not dead in main) are unrestricted
        if (!plugin.getLimboDeadPlayers().contains(player.getUniqueId())) return;

        String command = event.getMessage().toLowerCase().split(" ")[0];

        if (isWhitelistedCommand(command)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(MessageUtil.get("limbo-cannot-leave"));
    }

    private static boolean isWhitelistedCommand(String command) {
        return "/msg".equals(command) || "/tell".equals(command)
                || "/r".equals(command) || "/reply".equals(command)
                || "/help".equals(command) || "/list".equals(command)
                || "/pstatus".equals(command)
                || "/psadmin".equals(command) || "/psa".equals(command)
                || "/revive".equals(command) || "/psetlives".equals(command);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.getLimboDeadPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(PERM_BYPASS)
                && plugin.getLimboDeadPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.get("limbo-cannot-leave"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Location to = event.getTo();
        if (to != null && to.getY() < -64) {
            Location spawn = cachedLimboSpawn; // Use cached value
            if (spawn != null && spawn.getWorld() != null) {
                player.teleport(spawn);
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }
}
