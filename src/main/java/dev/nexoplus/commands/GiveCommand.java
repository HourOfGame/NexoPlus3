package dev.nexoplus.commands;

import dev.nexoplus.core.NexoPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.stream.Collectors;

public class GiveCommand implements CommandExecutor, TabCompleter {
    private final NexoPlus plugin;
    public GiveCommand(NexoPlus p) { this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("nexoplus.give")) {
            sender.sendMessage(ChatColor.RED + "No permission."); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /give <player> <item_id> [amount]"); return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
        String itemId = args[1];
        int amount = args.length >= 3 ? parseInt(args[2], 1) : 1;
        ItemStack item = plugin.getItemManager().buildItemStack(itemId, amount);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Item '" + itemId + "' not found."); return true;
        }
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Given " + amount + "x " + itemId + " to " + target.getName());
        target.sendMessage(ChatColor.YELLOW + "You received " + ChatColor.WHITE + amount + "x "
                + ChatColor.GOLD + itemId);
        return true;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.startsWith(args[0])).collect(Collectors.toList());
        if (args.length == 2) return plugin.getItemManager().getAllIds().stream()
                .filter(id -> id.startsWith(args[1])).sorted().collect(Collectors.toList());
        return Collections.emptyList();
    }
}
