package org.ssoggy.ssoggysouls.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.ssoggy.ssoggysouls.SSoggySouls;
import org.ssoggy.ssoggysouls.util.MessageUtil;
import org.ssoggy.ssoggysouls.util.ServerTransferUtil;

public class LeaveLimboCommand implements CommandExecutor {

    private final SSoggySouls plugin;

    public LeaveLimboCommand(SSoggySouls plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("java:S3516")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            handleLeave(player);
        } else if (sender != null) {
            sender.sendMessage("Only players can use this command.");
        }
        return true;
    }

    private void handleLeave(Player player) {
        if (plugin.getLimboDeadPlayers().contains(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get("limbo-cannot-leave"));
            return;
        }

        player.sendMessage(MessageUtil.get("limbo-visit-leaving"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ServerTransferUtil.sendToMain(player);
            }
        }, 20L);
    }
}
