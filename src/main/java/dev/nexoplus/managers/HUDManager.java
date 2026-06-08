package dev.nexoplus.managers;
import dev.nexoplus.core.NexoPlus;
public class HUDManager {
    private final NexoPlus plugin;
    public HUDManager(NexoPlus p) { this.plugin = p; }
    public void loadHUDs() { plugin.getLogger().info("HUD manager ready."); }
    public void reload() { loadHUDs(); }
    public int getLoadedCount() { return 0; }
}
