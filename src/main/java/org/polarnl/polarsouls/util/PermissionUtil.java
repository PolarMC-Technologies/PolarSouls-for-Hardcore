package org.polarnl.polarsouls.util;

import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.polarnl.polarsouls.PolarSouls;

public final class PermissionUtil {

    private PermissionUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * checks whether a sender should be blocked by limbo-only OP security rules.
     *
     * @param sender the command sender to check
     * @param plugin the plugin instance
     * @return true when the sender should be blocked
     */
    public static boolean isBlockedByLimboOpSecurity(CommandSender sender, PolarSouls plugin) {
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

        // Limbo OP security only targets OP players; for non-OP players, this check does not apply
        // and normal permission checks elsewhere will determine whether they can execute the command.
        if (!player.isOp()) {
            return false;
        }

        // Check if player is in the trusted admins whitelist (by UUID or username)
        Set<String> trustedAdmins = plugin.getLimboTrustedAdmins();
        if (!trustedAdmins.isEmpty()) {
            String playerUuid = player.getUniqueId().toString();
            String playerNameLowercase = player.getName().toLowerCase();
            
            // Check both UUID (exact match) and username (case-insensitive via lowercase)
            if (trustedAdmins.contains(playerUuid) || trustedAdmins.contains(playerNameLowercase)) {
                return false;
            }
        }

        // Player is OP on Limbo server - check if they have bypass permission
        if (player.hasPermission("polarsouls.bypass-limbo-op-security")) {
            return false;
        }

        // Player is OP without bypass permission or whitelist entry - block them
        // This prevents the security vulnerability where Limbo-only OPs can abuse admin commands
        return true;
    }

    /**
     * sends the limbo OP security block message.
     *
     * @param sender the command sender
     */
    public static void sendSecurityBlockMessage(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("&cSecurity Error: On the Limbo server, OP status cannot be used to execute this command."));
        sender.sendMessage(MessageUtil.colorize("&7Either /deop yourself on Limbo, ask an administrator to add you to the whitelist, or have them grant you the bypass permission &e(polarsouls.bypass-limbo-op-security)&7."));
    }
}
