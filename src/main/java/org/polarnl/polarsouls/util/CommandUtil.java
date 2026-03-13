package org.polarnl.polarsouls.util;

import org.bukkit.command.CommandSender;

public final class CommandUtil {

    private CommandUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * checks whether the sender has a permission, sending the default deny message when missing.
     *
     * @param sender the command sender
     * @param permission the permission node to check
     * @return true when allowed
     */
    public static boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.colorize("&cYou don't have permission to use this command."));
            return false;
        }
        return true;
    }

    /**
     * checks whether the sender has a permission, sending a custom deny message when missing.
     *
     * @param sender the command sender
     * @param permission the permission node to check
     * @param message the message to send when denied
     * @return true when allowed
     */
    public static boolean checkPermission(CommandSender sender, String permission, String message) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.colorize(message));
            return false;
        }
        return true;
    }
}
