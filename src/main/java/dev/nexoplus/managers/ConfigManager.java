package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final NexoPlus plugin;
    private FileConfiguration cfg;

    public ConfigManager(NexoPlus plugin) { this.plugin = plugin; }

    public void load() { cfg = plugin.getConfig(); }
    public void reload() { plugin.reloadConfig(); cfg = plugin.getConfig(); }

    public boolean isAutoGeneratePack() { return cfg.getBoolean("resourcepack.auto_generate", true); }
    public boolean isAutoSendPack() { return cfg.getBoolean("resourcepack.auto_send", true); }
    public boolean isPackRequired() { return cfg.getBoolean("resourcepack.required", false); }
    public boolean isUseBuiltinPackHost() { return cfg.getBoolean("resourcepack.use_builtin_host", true); }
    public int getPackHostPort() { return cfg.getInt("resourcepack.host_port", 8080); }
    public String getPackUrl() { return cfg.getString("resourcepack.url", ""); }
    public int getPackFormat() { return cfg.getInt("resourcepack.pack_format", 15); }
    public String getPackDescription() { return cfg.getString("resourcepack.description", "NexoPlus ResourcePack"); }
    public String getPackPromptMessage() { return cfg.getString("resourcepack.prompt_message", "Please accept the resource pack!"); }
    public boolean isDebug() { return cfg.getBoolean("settings.debug", false); }
}
