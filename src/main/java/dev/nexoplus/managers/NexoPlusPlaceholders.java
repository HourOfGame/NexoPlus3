package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoItem;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for NexoPlus.
 *
 * Available placeholders:
 *   %nexoplus_items_loaded%      - total custom items loaded
 *   %nexoplus_blocks_loaded%     - total custom blocks loaded
 *   %nexoplus_pack_sha1%         - current resourcepack SHA1
 *   %nexoplus_pack_url%          - current resourcepack URL
 *   %nexoplus_holding_id%        - ID of item player is holding
 *   %nexoplus_holding_name%      - display name of held item
 *   %nexoplus_is_holding_<id>%   - true/false if holding specific item
 *   %nexoplus_glyph_<id>%        - renders a glyph character
 *   %nexoplus_version%           - plugin version
 */
public class NexoPlusPlaceholders extends PlaceholderExpansion {

    private final NexoPlus plugin;

    public NexoPlusPlaceholders(NexoPlus plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "nexoplus"; }
    @Override public @NotNull String getAuthor()     { return "NexoPlus"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // Static placeholders (no player needed)
        switch (params.toLowerCase()) {
            case "items_loaded"  -> { return String.valueOf(plugin.getItemManager().getLoadedCount()); }
            case "blocks_loaded" -> { return String.valueOf(plugin.getBlockManager().getLoadedCount()); }
            case "version"       -> { return plugin.getDescription().getVersion(); }
            case "pack_sha1"     -> {
                String sha1 = plugin.getResourcePackManager().getPackSha1();
                return sha1 != null ? sha1 : "not_generated";
            }
            case "pack_url" -> {
                String url = plugin.getResourcePackManager().getPackUrl();
                return url != null ? url : "not_configured";
            }
        }

        // Glyph placeholder: %nexoplus_glyph_<id>%
        if (params.startsWith("glyph_")) {
            String glyphId = params.substring(6);
            var glyph = plugin.getGlyphManager().getGlyph(glyphId);
            return glyph != null ? String.valueOf(glyph.getGlyphChar()) : "";
        }

        // Player-dependent placeholders
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "holding_id" -> {
                org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
                String id = dev.nexoplus.items.NexoItem.getItemId(held, plugin);
                return id != null ? id : "";
            }
            case "holding_name" -> {
                org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
                NexoItem item = plugin.getItemManager().getItemFromStack(held);
                return item != null ? item.getDisplayName() : "";
            }
        }

        // %nexoplus_is_holding_<item_id>%
        if (params.startsWith("is_holding_")) {
            String itemId = params.substring(11);
            org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
            String heldId = dev.nexoplus.items.NexoItem.getItemId(held, plugin);
            return itemId.equals(heldId) ? "true" : "false";
        }

        // %nexoplus_has_item_<item_id>%
        if (params.startsWith("has_item_")) {
            String itemId = params.substring(9);
            NexoItem target = plugin.getItemManager().getItem(itemId);
            if (target == null) return "false";
            for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
                if (target.matches(stack, plugin)) return "true";
            }
            return "false";
        }

        // %nexoplus_count_<item_id>%
        if (params.startsWith("count_")) {
            String itemId = params.substring(6);
            NexoItem target = plugin.getItemManager().getItem(itemId);
            if (target == null) return "0";
            int count = 0;
            for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && target.matches(stack, plugin)) count += stack.getAmount();
            }
            return String.valueOf(count);
        }

        return null;
    }
}
