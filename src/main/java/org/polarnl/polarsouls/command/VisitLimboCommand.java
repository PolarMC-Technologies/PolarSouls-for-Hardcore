package org.polarnl.polarsouls.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.polarnl.polarsouls.PolarSouls;
import org.polarnl.polarsouls.util.MessageUtil;
import org.polarnl.polarsouls.util.ServerTransferUtil;

public class VisitLimboCommand implements CommandExecutor {

    private final PolarSouls plugin;

    public VisitLimboCommand(PolarSouls plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("java:S3516")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            handleVisit(player);
        } else if (sender != null) {
            sender.sendMessage("Only players can use this command.");
        }
        return true;
    }

    private void handleVisit(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isDead = plugin.getDatabaseManager().isPlayerDead(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (isDead) {
                    player.sendMessage(MessageUtil.get("limbo-visit-already-dead"));
                } else {
                    player.sendMessage(MessageUtil.get("limbo-visit-going"));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ServerTransferUtil.sendToLimbo(player);
                        }
                    }, 20L);
                }
            });
        });
    }
}
