package com.mario.hardcorelimbo.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.mario.hardcorelimbo.HardcoreLimbo;
import com.mario.hardcorelimbo.database.DatabaseManager;
import com.mario.hardcorelimbo.model.PlayerData;
import com.mario.hardcorelimbo.util.MessageUtil;

/**
 * /hlsetlives <player> <lives> - Set a player's life count.
 *
 * Admin utility command. Setting lives > 0 will also mark the player
 * as alive (is_dead=false), effectively reviving them.
 */
public class SetLivesCommand implements CommandExecutor, TabCompleter {

    private final HardcoreLimbo plugin;
    private final DatabaseManager db;

    public SetLivesCommand(HardcoreLimbo plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /hlsetlives <player> <lives>"));
            return true;
        }

        String targetName = args[0];
        int lives;

        try {
            lives = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&cInvalid number: " + args[1]));
            return true;
        }

        if (lives < 0) {
            sender.sendMessage(MessageUtil.colorize("&cLives cannot be negative."));
            return true;
        }

        int maxLives = plugin.getMaxLives();
        if (maxLives > 0 && lives > maxLives) {
            sender.sendMessage(MessageUtil.colorize("&cMaximum lives: " + maxLives));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(targetName);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        "player", targetName));
                return;
            }

            // Update lives (this also handles is_dead flag)
            db.setLives(data.getUuid(), lives);

            plugin.getLogger().info(sender.getName() + " set " + data.getUsername() +
                    "'s lives to " + lives);
            sender.sendMessage(MessageUtil.get("lives-set",
                    "player", data.getUsername(),
                    "lives", lives));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (var player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(player.getName());
                }
            }
            return suggestions;
        }
        if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "5");
        }
        return Collections.emptyList();
    }
}
