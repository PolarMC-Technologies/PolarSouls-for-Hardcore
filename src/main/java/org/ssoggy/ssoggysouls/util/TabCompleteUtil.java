package org.ssoggy.ssoggysouls.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class TabCompleteUtil {

    private TabCompleteUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * gets online player names matching the given prefix.
     *
     * @param prefix the prefix to filter by (case-insensitive)
     * @return list of matching player names
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
     * filters options by prefix (case-insensitive).
     *
     * @param options list of options to filter
     * @param prefix the prefix to match
     * @return list of matching options
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
