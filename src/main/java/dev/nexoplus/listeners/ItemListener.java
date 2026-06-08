package dev.nexoplus.listeners;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import dev.nexoplus.items.NexoItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

// ===========================
// ItemListener
// ===========================
class ItemListener_ {}

public class ItemListener implements Listener {
    private final NexoPlus plugin;
    public ItemListener(NexoPlus p) { this.plugin = p; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null) return;
        NexoItem nexoItem = plugin.getItemManager().getItemFromStack(item);
        if (nexoItem == null) return;

        // Permission check
        Player player = e.getPlayer();
        if (nexoItem.getUsePermission() != null && !player.hasPermission(nexoItem.getUsePermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this item.");
            e.setCancelled(true);
            return;
        }

        // Execute mechanics
        if (nexoItem.getMechanics() != null) {
            for (var mechanic : nexoItem.getMechanics()) {
                mechanic.execute(player, item, e);
            }
        }
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        NexoItem nexoItem = plugin.getItemManager().getItemFromStack(e.getItem().getItemStack());
        if (nexoItem == null) return;
        if (nexoItem.getPickupPermission() != null && !player.hasPermission(nexoItem.getPickupPermission())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        NexoItem nexoItem = plugin.getItemManager().getItemFromStack(e.getItemDrop().getItemStack());
        if (nexoItem == null) return;
        if (nexoItem.getDropPermission() != null && !e.getPlayer().hasPermission(nexoItem.getDropPermission())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        NexoItem nexoItem = plugin.getItemManager().getItemFromStack(item);
        if (nexoItem == null) return;
        if (nexoItem.isWeapon() && nexoItem.getAttackDamage() > 0) {
            e.setDamage(nexoItem.getAttackDamage());
        }
    }
}
