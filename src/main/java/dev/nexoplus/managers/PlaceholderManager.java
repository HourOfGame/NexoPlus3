package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {
    private final NexoPlus plugin;
    public PlaceholderManager(NexoPlus p) { this.plugin = p; }

    @Override public @NotNull String getIdentifier() { return "nexoplus"; }
    @Override public @NotNull String getAuthor() { return "NexoPlus"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        return switch (params.toLowerCase()) {
            case "items_loaded" -> String.valueOf(plugin.getItemManager().getLoadedCount());
            case "blocks_loaded" -> String.valueOf(plugin.getBlockManager().getLoadedCount());
            case "pack_sha1" -> plugin.getResourcePackManager().getPackSha1() != null
                    ? plugin.getResourcePackManager().getPackSha1() : "not_generated";
            default -> null;
        };
    }
}
