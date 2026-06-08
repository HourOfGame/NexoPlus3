package dev.nexoplus.commands;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class NexoPlusCommand implements CommandExecutor, TabCompleter {
    private final NexoPlus plugin;
    public NexoPlusCommand(NexoPlus p) { this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("nexoplus.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "NexoPlus reloaded!");
            }
            case "generate" -> {
                if (!sender.hasPermission("nexoplus.resourcepack")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                sender.sendMessage(ChatColor.YELLOW + "Generating ResourcePack...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getResourcePackManager().generatePack();
                    sender.sendMessage(ChatColor.GREEN + "ResourcePack generated!");
                });
            }
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "=== NexoPlus Items (" + plugin.getItemManager().getLoadedCount() + ") ===");
                for (NexoItem item : plugin.getItemManager().getAllItems()) {
                    sender.sendMessage(ChatColor.YELLOW + " - " + ChatColor.WHITE + item.getId()
                            + ChatColor.GRAY + " [CMD:" + item.getCustomModelData() + "]");
                }
            }
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "=== NexoPlus Info ===");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Items: " + ChatColor.WHITE + plugin.getItemManager().getLoadedCount());
                sender.sendMessage(ChatColor.YELLOW + "Blocks: " + ChatColor.WHITE + plugin.getBlockManager().getLoadedCount());
                sender.sendMessage(ChatColor.YELLOW + "Pack URL: " + ChatColor.WHITE + plugin.getResourcePackManager().getPackUrl());
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "=== NexoPlus Commands ===");
        s.sendMessage(ChatColor.YELLOW + "/nexoplus reload" + ChatColor.GRAY + " - Reload plugin");
        s.sendMessage(ChatColor.YELLOW + "/nexoplus generate" + ChatColor.GRAY + " - Generate ResourcePack");
        s.sendMessage(ChatColor.YELLOW + "/nexoplus list" + ChatColor.GRAY + " - List all items");
        s.sendMessage(ChatColor.YELLOW + "/nexoplus info" + ChatColor.GRAY + " - Plugin info");
        s.sendMessage(ChatColor.YELLOW + "/give <player> <item_id> [amount]" + ChatColor.GRAY + " - Give item");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("reload", "generate", "list", "info");
        return Collections.emptyList();
    }
}
