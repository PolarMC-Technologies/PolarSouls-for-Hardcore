package com.mario.hardcorelimbo.command;

import java.util.ArrayList;
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
 * /hlstatus [player] - Check a player's hardcore status.
 *
 * Shows lives, death state, and grace period info.
 * If no player is specified and sender is a player, shows their own status.
 */
public class StatusCommand implements CommandExecutor, TabCompleter {

    private final HardcoreLimbo plugin;
    private final DatabaseManager db;

    public StatusCommand(HardcoreLimbo plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String targetName;

        if (args.length >= 1) {
            targetName = args[0];
        } else if (sender instanceof org.bukkit.entity.Player player) {
            targetName = player.getName();
        } else {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /hlstatus <player>"));
            return true;
        }

        final String name = targetName;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(name);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        "player", name));
                return;
            }

            if (data.isDead()) {
                sender.sendMessage(MessageUtil.get("status-dead",
                        "player", data.getUsername()));
            } else if (data.isInGracePeriod(plugin.getGracePeriodHours())) {
                sender.sendMessage(MessageUtil.get("status-grace",
                        "player", data.getUsername(),
                        "lives", data.getLives(),
                        "time_remaining", data.getGraceTimeRemaining(
                                plugin.getGracePeriodHours())));
            } else {
                sender.sendMessage(MessageUtil.get("status-alive",
                        "player", data.getUsername(),
                        "lives", data.getLives()));
            }
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
        return Collections.emptyList();
    }
}
