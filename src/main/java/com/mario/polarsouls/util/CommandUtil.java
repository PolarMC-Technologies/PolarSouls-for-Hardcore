package com.mario.polarsouls.util;

import org.bukkit.command.CommandSender;

/**
 * Utility class for common command operations.
 */
public final class CommandUtil {

    private CommandUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Check if sender has the required permission and send error message if not.
     *
     * @param sender     The command sender
     * @param permission The permission node to check
     * @return true if sender has permission, false otherwise
     */
    public static boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.colorize("&cYou don't have permission to use this command."));
            return false;
        }
        return true;
    }

    /**
     * Check if sender has the required permission and send custom error message if not.
     *
     * @param sender     The command sender
     * @param permission The permission node to check
     * @param message    The error message to send if permission check fails
     * @return true if sender has permission, false otherwise
     */
    public static boolean checkPermission(CommandSender sender, String permission, String message) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.colorize(message));
            return false;
        }
        return true;
    }
}
