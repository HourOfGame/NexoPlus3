package dev.nexoplus.listeners;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PlayerListener implements Listener {
    private final NexoPlus plugin;

    public PlayerListener(NexoPlus p) { this.plugin = p; }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!plugin.getConfigManager().isAutoSendPack()) return;
        // Delay to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> plugin.getResourcePackManager().sendPackToPlayer(e.getPlayer()), 20L);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        Player player = e.getPlayer();
        var status = e.getStatus();

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(player.getName() + " resource pack status: " + status);
        }

        switch (status) {
            case DECLINED -> {
                if (plugin.getConfigManager().isPackRequired()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.kickPlayer("\u00a7cYou must accept the resource pack to play on this server!"));
                }
            }
            case FAILED_DOWNLOAD -> {
                // Retry once
                Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getResourcePackManager().sendPackToPlayer(player), 60L);
            }
            case SUCCESSFULLY_LOADED -> {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info(player.getName() + " loaded the resource pack successfully.");
                }
            }
            default -> {}
        }
    }
}
