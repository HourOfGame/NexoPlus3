package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoFont;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    private final NexoPlus plugin;
    private final Map<String, NexoFont> fonts = new ConcurrentHashMap<>();
    public FontManager(NexoPlus plugin) { this.plugin = plugin; }
    public void loadFonts() {
        fonts.clear();
        File folder = new File(plugin.getDataFolder(), "fonts");
        if (!folder.exists()) { folder.mkdirs(); return; }
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            for (String key : cfg.getKeys(false)) {
                if (!cfg.isConfigurationSection(key)) continue;
                var s = cfg.getConfigurationSection(key);
                String ns = f.getName().replace(".yml","");
                fonts.put(ns+":"+key, NexoFont.builder()
                    .id(ns+":"+key).namespace(ns).fontName(key)
                    .type(s.getString("type","bitmap"))
                    .file(s.getString("file",""))
                    .ascent(s.getInt("ascent",8))
                    .height(s.getInt("height",8))
                    .chars(s.getStringList("chars")).build());
            }
        }
        plugin.getLogger().info("Loaded " + fonts.size() + " custom fonts.");
    }
    public void reload() { loadFonts(); }
    public Collection<NexoFont> getAllFonts() { return Collections.unmodifiableCollection(fonts.values()); }
}
