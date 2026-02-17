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

        // If player has explicit permission node (not just OP), allow them
        // This checks if they have the permission WITHOUT being OP
        if (player.hasPermission(permission)) {
            // Check if they'd still have permission if they weren't OP
            // If they're not OP, they definitely have explicit permission
            if (!player.isOp()) {
                return false;
            }
            
            // They are OP - we need to check if they ALSO have explicit permission
            // The best way to do this is to check for a special bypass permission
            // or to assume that OP-only means they shouldn't have access
            // Since we can't easily differentiate, we'll use a bypass permission
            if (player.hasPermission("polarsouls.bypass-limbo-op-security")) {
                return false;
            }
            
            // They only have permission via OP, block them
            return true;
        }

        // They don't have permission at all
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
