package com.mario.polarsouls.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.hrm.HeadDropListener;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.ServerTransferUtil;

public class ReviveCommand implements CommandExecutor, TabCompleter {

    private static final String KEY_PLAYER = "player";

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public ReviveCommand(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /revive <player>"));
            return false;
        }

        String targetName = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayerByName(targetName);

            if (data == null) {
                sender.sendMessage(MessageUtil.get("revive-player-not-found",
                        KEY_PLAYER, targetName));
                return;
            }

            if (!data.isDead()) {
                sender.sendMessage(MessageUtil.get("revive-not-dead",
                        KEY_PLAYER, data.getUsername()));
                return;
            }

            int livesToRestore = plugin.getLivesOnRevive();
            boolean success = db.revivePlayer(data.getUuid(), livesToRestore);

            if (success) {
                plugin.getLogger().log(Level.INFO, "{0} revived {1} (lives: {2})",
                        new Object[]{sender.getName(), data.getUsername(), livesToRestore});
                sender.sendMessage(MessageUtil.get("revive-admin-success",
                        KEY_PLAYER, data.getUsername(),
                        "lives", livesToRestore));
                restoreOnlineSpectator(data);

                // Remove any dropped player head items from all worlds
                Bukkit.getScheduler().runTask(plugin, () ->
                        HeadDropListener.removeDroppedHeads(data.getUuid()));
            } else {
                sender.sendMessage(MessageUtil.colorize(
                        "&cFailed to revive " + data.getUsername() + ". Check console for errors."));
            }
        });

        return true;
    }

    private void restoreOnlineSpectator(PlayerData data) {
        Player target = Bukkit.getPlayer(data.getUuid());
        if (target != null && target.isOnline()
                && target.getGameMode() != GameMode.SURVIVAL) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    plugin.getLimboDeadPlayers().remove(target.getUniqueId());
                    target.setGameMode(GameMode.SURVIVAL);
                    target.sendMessage(MessageUtil.get("revive-success"));

                    if (plugin.isLimboServer()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (target.isOnline()) {
                                ServerTransferUtil.sendToMain(target);
                            }
                        }, 40L);
                    }
                }
            });
        }
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