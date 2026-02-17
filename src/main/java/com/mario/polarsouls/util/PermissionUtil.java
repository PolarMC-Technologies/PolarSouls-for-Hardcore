package com.mario.polarsouls.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mario.polarsouls.PolarSouls;

/**
 * Utility class for permission-related operations.
 */
public final class PermissionUtil {

    private PermissionUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Check if a command sender should be blocked from executing admin/revive commands
     * due to Limbo-only OP security restrictions.
     * 
     * This security check prevents users who have OP status only on the Limbo server
     * from executing privileged commands like /revive and /psadmin.
     * 
     * @param sender The command sender to check
     * @param plugin The plugin instance
     * @param permission The permission node being checked
     * @return true if the sender should be blocked, false if allowed
     */
    public static boolean isBlockedByLimboOpSecurity(CommandSender sender, PolarSouls plugin, String permission) {
        // If security check is disabled, allow all
        if (!plugin.isLimboOpSecurityEnabled()) {
            return false;
        }

        // Only check players, not console or command blocks
        if (!(sender instanceof Player player)) {
            return false;
        }

        // Only apply security check on Limbo server
        if (!plugin.isLimboServer()) {
            return false;
        }

        // If player is not OP, they can execute commands (they have explicit permissions)
        if (!player.isOp()) {
            return false;
        }

        // Player is OP on Limbo server - check if they have bypass permission
        if (player.hasPermission("polarsouls.bypass-limbo-op-security")) {
            return false;
        }

        // Player is OP without bypass permission - block them
        // This prevents the security vulnerability where Limbo-only OPs can abuse admin commands
        return true;
    }

    /**
     * Send a security block message to the command sender.
     * 
     * @param sender The command sender to send the message to
     */
    public static void sendSecurityBlockMessage(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("&cSecurity Error: This command cannot be executed on the Limbo server with OP-only permissions."));
        sender.sendMessage(MessageUtil.colorize("&7Contact an administrator to grant you explicit permissions."));
    }
}
