package org.ssoggy.ssoggysouls.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.ssoggy.ssoggysouls.SSoggySouls;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.model.PlayerData;
import org.ssoggy.ssoggysouls.util.MessageUtil;
import org.ssoggy.ssoggysouls.util.TabCompleteUtil;

public class StatusCommand implements CommandExecutor, TabCompleter {

    private static final String KEY_PLAYER = "player";

    private final SSoggySouls plugin;
    private final DatabaseManager db;

    public StatusCommand(SSoggySouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    @SuppressWarnings("java:S2259") // colorize() never returns null
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }
        String targetName;

        if (args.length >= 1) {
            targetName = args[0];
        } else if (sender instanceof org.bukkit.entity.Player player) {
            targetName = player.getName();
        } else {
            String msg = MessageUtil.colorize("&cUsage: /pstatus <player>");
            if (msg != null) {
                sender.sendMessage(msg);
            }
            return false;
        }

        final String name = targetName;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(name);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        KEY_PLAYER, name));
                return;
            }

            if (data.isDead()) {
                sender.sendMessage(MessageUtil.get("status-dead",
                        KEY_PLAYER, data.getUsername()));
            } else if (data.isInGracePeriod(plugin.getGracePeriodMillis())) {
                sender.sendMessage(MessageUtil.get("status-grace",
                        KEY_PLAYER, data.getUsername(),
                        "lives", data.getLives(),
                        "time_remaining", data.getGraceTimeRemaining(
                                plugin.getGracePeriodMillis())));
            } else {
                sender.sendMessage(MessageUtil.get("status-alive",
                        KEY_PLAYER, data.getUsername(),
                        "lives", data.getLives()));
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            return TabCompleteUtil.getOnlinePlayerNames(args[0]);
        }
        return Collections.emptyList();
    }
}