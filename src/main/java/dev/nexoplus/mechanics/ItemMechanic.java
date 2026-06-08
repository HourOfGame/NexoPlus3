package dev.nexoplus.mechanics;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Base interface for all item mechanics.
 * Mechanics define what happens when player interacts with a NexoItem.
 */
public interface ItemMechanic {
    String getType();
    void execute(Player player, ItemStack item, Event event);
}
