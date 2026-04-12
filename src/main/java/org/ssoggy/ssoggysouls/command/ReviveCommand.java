package org.ssoggy.ssoggysouls.command;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.ssoggy.ssoggysouls.SSoggySouls;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.model.PlayerData;
import org.ssoggy.ssoggysouls.util.CommandUtil;
import org.ssoggy.ssoggysouls.util.MessageUtil;
import org.ssoggy.ssoggysouls.util.PermissionUtil;
import org.ssoggy.ssoggysouls.util.PlayerRevivalUtil;
import org.ssoggy.ssoggysouls.util.TabCompleteUtil;

public class ReviveCommand implements CommandExecutor, TabCompleter {

    private static final String KEY_PLAYER = "player";

    private final SSoggySouls plugin;
    private final DatabaseManager db;

    public ReviveCommand(SSoggySouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.checkPermission(sender, "ssoggysouls.revive", "&cYou don't have permission to revive players.")) {
            return true;
        }

        // Security check: Prevent Limbo-only OP from using this command
        if (PermissionUtil.isBlockedByLimboOpSecurity(sender, plugin)) {
            PermissionUtil.sendSecurityBlockMessage(sender);
            return true;
        }

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
                plugin.removeDroppedHeads(data.getUuid());
            } else {
                sender.sendMessage(MessageUtil.colorize(
                        "&cFailed to revive " + data.getUsername() + ". Check console for errors."));
            }
        });

        return true;
    }

    private void restoreOnlineSpectator(PlayerData data) {
        PlayerRevivalUtil.restoreOnlineSpectator(plugin, data);
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
