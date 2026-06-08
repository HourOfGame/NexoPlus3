package dev.nexoplus.api;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import dev.nexoplus.items.NexoItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * NexoPlus Public API
 * Access via: NexoPlus.getAPI()
 */
public class NexoPlusAPI {
    private final NexoPlus plugin;

    public NexoPlusAPI(NexoPlus plugin) {
        this.plugin = plugin;
    }

    // ===== ITEMS =====
    public NexoItem getItem(String id) { return plugin.getItemManager().getItem(id); }
    public ItemStack buildItem(String id) { return plugin.getItemManager().buildItemStack(id); }
    public ItemStack buildItem(String id, int amount) { return plugin.getItemManager().buildItemStack(id, amount); }
    public NexoItem getItemFromStack(ItemStack stack) { return plugin.getItemManager().getItemFromStack(stack); }
    public boolean isNexoItem(ItemStack stack) { return plugin.getItemManager().isNexoItem(stack); }
    public String getItemId(ItemStack stack) { return NexoItem.getItemId(stack, plugin); }
    public Collection<NexoItem> getAllItems() { return plugin.getItemManager().getAllItems(); }

    // ===== BLOCKS =====
    public NexoBlock getBlock(String id) { return plugin.getBlockManager().getBlock(id); }
    public NexoBlock getBlockAt(Block block) { return plugin.getBlockManager().getBlockAt(block); }
    public boolean isNexoBlock(Block block) { return plugin.getBlockManager().isNexoBlock(block); }

    // ===== RESOURCEPACK =====
    public void generatePack() { plugin.getResourcePackManager().generatePack(); }
    public void sendPack(Player player) { plugin.getResourcePackManager().sendPackToPlayer(player); }
    public String getPackUrl() { return plugin.getResourcePackManager().getPackUrl(); }
    public String getPackSha1() { return plugin.getResourcePackManager().getPackSha1(); }

    // ===== PLUGIN =====
    public void reload() { plugin.reload(); }
    public String getVersion() { return plugin.getDescription().getVersion(); }
}
