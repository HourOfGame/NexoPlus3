package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoFont;
import dev.nexoplus.items.NexoSound;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ===== SoundManager =====
class SoundManager_ {}

public class SoundManager {
    private final NexoPlus plugin;
    private final Map<String, NexoSound> sounds = new ConcurrentHashMap<>();
    public SoundManager(NexoPlus plugin) { this.plugin = plugin; }
    public void loadSounds() {
        sounds.clear();
        File f = new File(plugin.getDataFolder(), "sounds/sounds.yml");
        if (!f.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        for (String key : cfg.getKeys(false)) {
            if (!cfg.isConfigurationSection(key)) continue;
            var s = cfg.getConfigurationSection(key);
            sounds.put(key, NexoSound.builder()
                .id(key).namespace(s.getString("namespace","default"))
                .soundName(key).category(s.getString("category","master"))
                .files(s.getStringList("files")).build());
        }
        plugin.getLogger().info("Loaded " + sounds.size() + " custom sounds.");
    }
    public void reload() { loadSounds(); }
    public Collection<NexoSound> getAllSounds() { return Collections.unmodifiableCollection(sounds.values()); }
    public NexoSound getSound(String id) { return sounds.get(id); }
}
