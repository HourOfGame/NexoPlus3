package dev.nexoplus.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

public class CommandMechanic implements ItemMechanic {
    private final String command;
    private final boolean asConsole;

    public CommandMechanic(ConfigurationSection cfg) {
        this.command = cfg.getString("command", "");
        this.asConsole = cfg.getBoolean("as_console", false);
    }

    @Override public String getType() { return "COMMAND"; }

    @Override
    public void execute(Player player, ItemStack item, Event event) {
        String cmd = command.replace("%player%", player.getName());
        if (asConsole) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        else player.performCommand(cmd);
    }
}
