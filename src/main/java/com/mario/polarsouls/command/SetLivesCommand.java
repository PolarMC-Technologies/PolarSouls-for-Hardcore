package com.mario.polarsouls.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.CommandUtil;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.TabCompleteUtil;

public class SetLivesCommand implements CommandExecutor, TabCompleter {

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public SetLivesCommand(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.checkPermission(sender, "polarsouls.admin")) {
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /psetlives <player> <lives>"));
            return false;
        }

        String targetName = args[0];
        int lives;

        try {
            lives = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&cInvalid number: " + args[1]));
            return false;
        }

        if (lives < 0) {
            sender.sendMessage(MessageUtil.colorize("&cLives cannot be negative."));
            return false;
        }

        int maxLives = plugin.getMaxLives();
        if (maxLives > 0 && lives > maxLives) {
            sender.sendMessage(MessageUtil.colorize("&cMaximum lives: " + maxLives));
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(targetName);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        "player", targetName));
                return;
            }

            db.setLives(data.getUuid(), lives);

            plugin.getLogger().log(Level.INFO, "{0} set {1}''s lives to {2}",
                    new Object[]{sender.getName(), data.getUsername(), lives});
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
            return TabCompleteUtil.getOnlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "5");
        }
        return Collections.emptyList();
    }
}
