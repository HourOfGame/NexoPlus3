package dev.nexoplus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;

public class ColorUtils {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static String translate(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(translate(text));
    }

    public static Component miniMessage(String text) {
        if (text == null) return Component.empty();
        return MM.deserialize(text);
    }
}
