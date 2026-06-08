package dev.nexoplus.commands;
import dev.nexoplus.core.NexoPlus;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.Bukkit;
import java.util.*;
public class RecipeCommand implements CommandExecutor, TabCompleter {
    private final NexoPlus plugin;
    public RecipeCommand(NexoPlus p) { this.plugin = p; }
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) { s.sendMessage(ChatColor.RED + "Usage: /" + l + " <generate|send|reload>"); return true; }
        switch(a[0].toLowerCase()) {
            case "generate" -> { Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> { plugin.getResourcePackManager().generatePack(); s.sendMessage(ChatColor.GREEN + "Done!"); }); }
            case "send" -> { plugin.getResourcePackManager().sendPackToAll(); s.sendMessage(ChatColor.GREEN + "Pack sent to all players."); }
            case "reload" -> { plugin.getResourcePackManager().reload(); s.sendMessage(ChatColor.GREEN + "Reloaded."); }
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return a.length == 1 ? List.of("generate","send","reload") : Collections.emptyList();
    }
}
