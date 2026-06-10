package dev.nexoplus.listeners;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.Random;

public class BlockListener implements Listener {
    private final NexoPlus plugin;
    private final Random random = new Random();

    public BlockListener(NexoPlus p) { this.plugin = p; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        NexoBlock nexoBlock = plugin.getBlockManager().getBlockAt(block);
        if (nexoBlock == null) return;

        Player player = e.getPlayer();

        // Permission check
        if (nexoBlock.getBreakPermission() != null && !player.hasPermission(nexoBlock.getBreakPermission())) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't break this block.");
            return;
        }

        // Tool check
        if (nexoBlock.isRequiresCorrectTool()) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!isCorrectTool(tool, nexoBlock)) {
                e.setCancelled(true);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_HIT, 1f, 1f);
                return;
            }
        }

        // Cancel vanilla drops, handle custom drops
        e.setDropItems(false);

        // Custom drops
        if (nexoBlock.getDrops() != null) {
            for (NexoBlock.BlockDrop drop : nexoBlock.getDrops()) {
                if (random.nextDouble() > drop.getChance()) continue;
                int amount = drop.getMinAmount() + random.nextInt(
                        Math.max(1, drop.getMaxAmount() - drop.getMinAmount() + 1));
                ItemStack dropItem = plugin.getItemManager().buildItemStack(drop.getItemId(), amount);
                if (dropItem == null) {
                    // Try vanilla material
                    try {
                        dropItem = new ItemStack(Material.valueOf(drop.getItemId().toUpperCase()), amount);
                    } catch (Exception ignored) {}
                }
                if (dropItem != null) block.getWorld().dropItemNaturally(block.getLocation(), dropItem);
            }
        }

        // XP drop
        if (nexoBlock.getExperience() > 0) {
            block.getWorld().spawn(block.getLocation(), org.bukkit.entity.ExperienceOrb.class,
                    orb -> orb.setExperience(nexoBlock.getExperience()));
        }

        // Play custom break sound
        if (nexoBlock.getBreakSound() != null) {
            block.getWorld().playSound(block.getLocation(), nexoBlock.getBreakSound(),
                    nexoBlock.getSoundVolume() > 0 ? nexoBlock.getSoundVolume() : 1f,
                    nexoBlock.getSoundPitch() > 0 ? nexoBlock.getSoundPitch() : 1f);
        }

        // Remove from database
        removeBlockFromDB(block);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        NexoBlock nexoBlock = plugin.getBlockManager().getBlock(
                dev.nexoplus.items.NexoItem.getItemId(item, plugin));
        if (nexoBlock == null) return;

        Player player = e.getPlayer();
        if (nexoBlock.getPlacePermission() != null && !player.hasPermission(nexoBlock.getPlacePermission())) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't place this block.");
            return;
        }

        // Place the custom block state
        Block block = e.getBlockPlaced();
        block.setBlockData(nexoBlock.getBlockData());

        // Play custom place sound
        if (nexoBlock.getPlaceSound() != null) {
            block.getWorld().playSound(block.getLocation(), nexoBlock.getPlaceSound(), 1f, 1f);
        }

        // Save to database
        saveBlockToDB(block, nexoBlock.getId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onNoteBlockPlay(NotePlayEvent e) {
        // Cancel sound for custom NOTE_BLOCK blocks
        if (plugin.getBlockManager().isNexoBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean isCorrectTool(ItemStack tool, NexoBlock block) {
        if (block.getRequiredTool() == null || block.getRequiredTool().equals("none")) return true;
        if (tool == null || tool.getType() == Material.AIR) return false;
        String toolType = block.getRequiredTool().toLowerCase();
        String matName = tool.getType().name().toLowerCase();
        return matName.contains(toolType);
    }

    private void saveBlockToDB(Block block, String blockId) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO nexoplus_block_data (world,x,y,z,block_id) VALUES(?,?,?,?,?)");
            ps.setString(1, block.getWorld().getName());
            ps.setInt(2, block.getX()); ps.setInt(3, block.getY()); ps.setInt(4, block.getZ());
            ps.setString(5, blockId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void removeBlockFromDB(Block block) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM nexoplus_block_data WHERE world=? AND x=? AND y=? AND z=?");
            ps.setString(1, block.getWorld().getName());
            ps.setInt(2, block.getX()); ps.setInt(3, block.getY()); ps.setInt(4, block.getZ());
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
