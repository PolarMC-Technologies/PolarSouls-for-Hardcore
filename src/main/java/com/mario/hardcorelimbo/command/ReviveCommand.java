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
 * /revive <player> - Revive a dead player.
 *
 * Updates the database (is_dead=false, lives=configured amount).
 * The Limbo check task will detect the change and release the player.
 *
 * Works from both the Main and Limbo servers since it only touches the DB.
 */
public class ReviveCommand implements CommandExecutor, TabCompleter {

    private final HardcoreLimbo plugin;
    private final DatabaseManager db;

    public ReviveCommand(HardcoreLimbo plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /revive <player>"));
            return true;
        }

        String targetName = args[0];

        // Run DB operations async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(targetName);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        "player", targetName));
                return;
            }

            if (!data.isDead()) {
                sender.sendMessage(MessageUtil.get("revive-not-dead",
                        "player", data.getUsername()));
                return;
            }

            // Revive the player in the database
            int livesToRestore = plugin.getLivesOnRevive();
            boolean success = db.revivePlayer(data.getUuid(), livesToRestore);

            if (success) {
                plugin.getLogger().info(sender.getName() + " revived " + data.getUsername() +
                        " (lives: " + livesToRestore + ")");
                sender.sendMessage(MessageUtil.get("revive-admin-success",
                        "player", data.getUsername(),
                        "lives", livesToRestore));
            } else {
                sender.sendMessage(MessageUtil.colorize(
                        "&cFailed to revive " + data.getUsername() + ". Check console for errors."));
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            // Suggest online players (most useful for admins)
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
