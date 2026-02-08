package com.mario.hardcorelimbo.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Centralized message handling with config-driven strings and placeholders.
 */
public final class MessageUtil {

    private static String prefix = "&8[&4☠&8] &r";
    private static final Map<String, String> messages = new HashMap<>();

    private MessageUtil() {}

    /**
     * Loads all messages from the config's 'messages' section.
     */
    public static void loadMessages(FileConfiguration config) {
        prefix = config.getString("messages.prefix", "&8[&4☠&8] &r");

        // Load all message keys from the 'messages' section
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                if (key.equals("prefix")) continue;
                messages.put(key, config.getString("messages." + key, ""));
            }
        }
    }

    /**
     * Gets a raw (uncolored) message by key with placeholder replacement.
     */
    public static String getRaw(String key, Object... replacements) {
        String msg = messages.getOrDefault(key, "&cMissing message: " + key);

        // Apply placeholder pairs: "key", value, "key2", value2, ...
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = "%" + replacements[i] + "%";
            String value = String.valueOf(replacements[i + 1]);
            msg = msg.replace(placeholder, value);
        }
        return msg;
    }

    /**
     * Gets a formatted message with prefix, colors, and placeholders applied.
     *
     * @param key          the message key (e.g. "death-life-lost")
     * @param replacements alternating key-value pairs: "player", "Steve", "lives", 2
     */
    public static String get(String key, Object... replacements) {
        return colorize(prefix + getRaw(key, replacements));
    }

    /**
     * Gets a formatted message WITHOUT the prefix (for titles, action bars, etc.).
     */
    public static String getNoPrefix(String key, Object... replacements) {
        return colorize(getRaw(key, replacements));
    }

    /**
     * Translates '&' color codes to Minecraft formatting codes.
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
