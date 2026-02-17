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

        // If player doesn't have the permission at all, don't block here
        // (they'll be blocked by the normal permission check)
        if (!player.hasPermission(permission)) {
            return false;
        }

        // Player has the permission - now check if it's because of OP or explicit permission
        
        // If they have the bypass permission, allow them
        if (player.hasPermission("polarsouls.bypass-limbo-op-security")) {
            return false;
        }
        
        // If player is OP, block them (they need explicit permission or bypass)
        // This is the core security check: OP status alone is not enough on Limbo server
        if (player.isOp()) {
            return true;
        }

        // Player has permission and is not OP, so they have explicit permission
        return false;
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
