package org.ssoggy.ssoggysouls.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Forge-specific MessageUtil.
 * <p>
 * Delegates raw substitution to the common {@link MessageHelper} and wraps
 * results in Minecraft {@link Component} for use with Forge's chat API.
 */
public final class MessageUtil extends MessageHelper {

    private MessageUtil() {}

    // Load from Forge config
    public static void loadMessages() {
        ConfigManager.ModConfig cfg = ConfigManager.getConfig();
        prefix = cfg.getMessagePrefix();
        messages.clear();
        messages.putAll(cfg.getMessages());
    }

    public static String getRawString(String key, Object... replacements) {
        return getRaw(key, replacements);
    }

    public static MutableComponent get(String key, Object... replacements) {
        return colorizeComponent(prefix + getRaw(key, replacements));
    }

    public static MutableComponent getNoPrefix(String key, Object... replacements) {
        return colorizeComponent(getRaw(key, replacements));
    }

    public static MutableComponent colorizeComponent(String text) {
        if (text == null) return Component.empty();
        return Component.literal(text.replace('&', '\u00a7'));
    }
}
