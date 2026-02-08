package com.mario.hardcorelimbo.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mario.hardcorelimbo.HardcoreLimbo;
import com.mario.hardcorelimbo.util.MessageUtil;

/**
 * /setlimbospawn - Sets the Limbo spawn location to the executor's position.
 *
 * Only available on the Limbo server.
 */
public class SetLimboSpawnCommand implements CommandExecutor {

    private final HardcoreLimbo plugin;

    public SetLimboSpawnCommand(HardcoreLimbo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize("&cThis command can only be used in-game."));
            return true;
        }

        plugin.saveLimboSpawn(player.getLocation());
        player.sendMessage(MessageUtil.get("limbo-spawn-set"));

        plugin.getLogger().info(player.getName() + " set the limbo spawn to " +
                String.format("%.1f, %.1f, %.1f in %s",
                        player.getLocation().getX(),
                        player.getLocation().getY(),
                        player.getLocation().getZ(),
                        player.getLocation().getWorld().getName()));

        return true;
    }
}
