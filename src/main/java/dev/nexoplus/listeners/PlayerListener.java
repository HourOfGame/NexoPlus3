package dev.nexoplus.listeners;
import dev.nexoplus.core.NexoPlus;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PlayerListener implements Listener {
    private final NexoPlus plugin;
    public PlayerListener(NexoPlus p) { this.plugin = p; }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Send resource pack on join
        if (plugin.getConfigManager().isAutoSendPack()) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                () -> plugin.getResourcePackManager().sendPackToPlayer(e.getPlayer()), 20L);
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        var status = e.getStatus();
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(e.getPlayer().getName() + " pack status: " + status);
        }
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED
                && plugin.getConfigManager().isPackRequired()) {
            e.getPlayer().kickPlayer("§cYou must accept the resource pack to play on this server!");
        }
    }
}
