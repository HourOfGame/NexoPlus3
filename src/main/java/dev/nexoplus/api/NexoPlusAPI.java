package dev.nexoplus.api;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import dev.nexoplus.items.NexoItem;
import dev.nexoplus.managers.GlyphManager;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;

public class NexoPlusAPI {
    private final NexoPlus plugin;
    public NexoPlusAPI(NexoPlus plugin) { this.plugin = plugin; }

    // Items
    public NexoItem getItem(String id)                     { return plugin.getItemManager().getItem(id); }
    public ItemStack buildItem(String id)                  { return plugin.getItemManager().buildItemStack(id); }
    public ItemStack buildItem(String id, int amount)      { return plugin.getItemManager().buildItemStack(id, amount); }
    public NexoItem getItemFromStack(ItemStack stack)      { return plugin.getItemManager().getItemFromStack(stack); }
    public boolean isNexoItem(ItemStack stack)             { return plugin.getItemManager().isNexoItem(stack); }
    public String getItemId(ItemStack stack)               { return NexoItem.getItemId(stack, plugin); }
    public Collection<NexoItem> getAllItems()              { return plugin.getItemManager().getAllItems(); }

    // Blocks
    public NexoBlock getBlock(String id)                   { return plugin.getBlockManager().getBlock(id); }
    public NexoBlock getBlockAt(Block block)               { return plugin.getBlockManager().getBlockAt(block); }
    public boolean isNexoBlock(Block block)                { return plugin.getBlockManager().isNexoBlock(block); }

    // Glyphs
    public Component getGlyph(String id)                   { return plugin.getGlyphManager().getGlyphComponent(id); }
    public String getGlyphChar(String id) {
        GlyphManager.GlyphData g = plugin.getGlyphManager().getGlyph(id);
        return g != null ? String.valueOf(g.getGlyphChar()) : "";
    }
    public String parseGlyphs(String text)                 { return plugin.getGlyphManager().parseGlyphs(text); }

    // Furniture
    public boolean placeFurniture(NexoItem item, org.bukkit.Location loc, float yaw, Player player) {
        return plugin.getFurnitureManager().placeFurniture(item, loc, yaw, player);
    }

    // ResourcePack
    public void generatePack()                             { plugin.getResourcePackManager().generatePack(); }
    public void sendPack(Player player)                    { plugin.getResourcePackManager().sendPackToPlayer(player); }
    public String getPackUrl()                             { return plugin.getResourcePackManager().getPackUrl(); }
    public String getPackSha1()                            { return plugin.getResourcePackManager().getPackSha1(); }

    // Compatibility
    public boolean isPluginIntegrated(String name)         { return plugin.getCompatibilityManager().isIntegrated(name); }
    public String recognizeItemPlugin(ItemStack item)      { return plugin.getCompatibilityManager().recognizeItemPlugin(item); }

    // Plugin
    public void reload()                                   { plugin.reload(); }
    public String getVersion()                             { return plugin.getDescription().getVersion(); }
}
