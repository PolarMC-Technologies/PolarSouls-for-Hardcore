package com.mario.polarsouls.hrm;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;

public class HeadDropListener implements Listener {

    private static final String PERM_BYPASS = "PolarSouls.bypass";

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public HeadDropListener(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission(PERM_BYPASS)) return;

        World world = player.getWorld();
        Location deathLoc = player.getLocation();
        if (deathLoc == null) return;

        if (plugin.isHrmDeathLocationMsg()) {
            player.sendMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC
                    + "You died at " + deathLoc.getBlockX() + ", "
                    + deathLoc.getBlockY() + ", " + deathLoc.getBlockZ()
                    + " in " + world.getName());
        }

        // only drop head if really dead (work pls)
        if (plugin.isHrmDropHeads()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                boolean dead = db.isPlayerDead(player.getUniqueId());
                if (dead) {
                    // Drop head on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ItemStack head = createPlayerHead(player);
                        world.dropItemNaturally(deathLoc, head);
                        plugin.debug("Dropped " + player.getName() + "'s head at "
                                + deathLoc.getBlockX() + ", " + deathLoc.getBlockY()
                                + ", " + deathLoc.getBlockZ());
                    });
                }
            }, 10L); // 0.5s delay beacause idfk it feels good
        }
    }

    public static ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.YELLOW + player.getName() + "'s Head");
            meta.setLore(List.of(
                    ChatColor.DARK_RED.toString() + ChatColor.ITALIC + "A fallen player's head",
                    ChatColor.GRAY + "Place on a revival structure to revive"));
            head.setItemMeta(meta);
        }
        return head;
    }
}
