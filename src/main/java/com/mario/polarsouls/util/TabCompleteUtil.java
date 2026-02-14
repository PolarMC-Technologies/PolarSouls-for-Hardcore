package com.mario.polarsouls.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for tab completion functionality across commands.
 */
public final class TabCompleteUtil {

    private TabCompleteUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get a list of online player names that start with the given prefix.
     *
     * @param prefix The prefix to filter by (case-insensitive)
     * @return List of matching player names
     */
    public static List<String> getOnlinePlayerNames(String prefix) {
        List<String> names = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lower)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    /**
     * Filter a list of options by prefix (case-insensitive).
     *
     * @param options List of options to filter
     * @param prefix  The prefix to filter by
     * @return List of matching options
     */
    public static List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
