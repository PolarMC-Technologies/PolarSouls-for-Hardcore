package com.mario.polarsouls.command;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.util.MessageUtil;

public class SetLimboSpawnCommand implements CommandExecutor {

    private final PolarSouls plugin;

    public SetLimboSpawnCommand(PolarSouls plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }
        if (!sender.hasPermission("polarsouls.admin")) {
            sender.sendMessage(MessageUtil.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            String msg = MessageUtil.colorize("&cThis command can only be used in-game.");
            if (msg != null) {
                sender.sendMessage(msg);
            }
            return false;
        }

        Location loc = player.getLocation();
        if (loc == null) {
            return false;
        }

        plugin.saveLimboSpawn(loc);
        player.sendMessage(MessageUtil.get("limbo-spawn-set"));

        World world = loc.getWorld();
        String worldName = world != null ? world.getName() : "unknown";

        plugin.getLogger().log(Level.INFO, "{0} set the limbo spawn to {1} in {2}",
                new Object[]{
                        player.getName(),
                        String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()),
                        worldName
                });

        return true;
    }
}