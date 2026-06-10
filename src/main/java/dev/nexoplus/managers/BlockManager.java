package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BlockManager {
    private final NexoPlus plugin;
    private final Map<String, NexoBlock> blocks = new ConcurrentHashMap<>();
    private File blocksFolder;

    public BlockManager(NexoPlus plugin) { this.plugin = plugin; }

    public void loadBlocks() {
        blocks.clear();
        blocksFolder = new File(plugin.getDataFolder(), "blocks");
        if (!blocksFolder.exists()) { blocksFolder.mkdirs(); return; }
        scanDirectory(blocksFolder);
        plugin.getLogger().info("Loaded " + blocks.size() + " custom blocks.");
    }

    public void reload() { loadBlocks(); }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDirectory(f);
            else if (f.getName().endsWith(".yml")) loadFile(f);
        }
    }

    private void loadFile(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String ns = file.getName().replace(".yml", "");
        for (String key : cfg.getKeys(false)) {
            if (!cfg.isConfigurationSection(key)) continue;
            try {
                ConfigurationSection s = cfg.getConfigurationSection(key);
                NexoBlock block = parseBlock(ns, key, s);
                blocks.put(block.getId(), block);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load block '" + key + "': " + e.getMessage());
            }
        }
    }

    private NexoBlock parseBlock(String ns, String name, ConfigurationSection s) {
        NexoBlock.BlockType type = NexoBlock.BlockType.NOTE_BLOCK;
        try { type = NexoBlock.BlockType.valueOf(s.getString("block_type", "NOTE_BLOCK").toUpperCase()); }
        catch (Exception ignored) {}

        List<NexoBlock.BlockDrop> drops = new ArrayList<>();
        for (Map<?, ?> dropMap : s.getMapList("drops")) {
            Object minObj = dropMap.get("min_amount");
            Object maxObj = dropMap.get("max_amount");
            Object chanceObj = dropMap.get("chance");
            int minAmount = minObj != null ? Integer.parseInt(String.valueOf(minObj)) : 1;
            int maxAmount = maxObj != null ? Integer.parseInt(String.valueOf(maxObj)) : 1;
            double chance = chanceObj != null ? Double.parseDouble(String.valueOf(chanceObj)) : 1.0;
            drops.add(NexoBlock.BlockDrop.builder()
                .itemId(String.valueOf(dropMap.get("item_id")))
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .chance(chance)
                .build());
        }

        return NexoBlock.builder()
            .id(ns + ":" + name)
            .namespace(ns)
            .blockName(name)
            .blockType(type)
            .blockData(s.getInt("block_data", 0))
            .texturePath(s.getString("texture", s.getString("texture_path")))
            .textureTop(s.getString("texture_top"))
            .textureBottom(s.getString("texture_bottom"))
            .textureNorth(s.getString("texture_north"))
            .textureSouth(s.getString("texture_south"))
            .textureEast(s.getString("texture_east"))
            .textureWest(s.getString("texture_west"))
            .hardness((float) s.getDouble("hardness", 1.5))
            .blastResistance((float) s.getDouble("blast_resistance", 6.0))
            .requiredTool(s.getString("required_tool", "none"))
            .requiredToolLevel(s.getInt("required_tool_level", 0))
            .requiresCorrectTool(s.getBoolean("requires_correct_tool", false))
            .breakSound(s.getString("break_sound"))
            .placeSound(s.getString("place_sound"))
            .experience(s.getInt("experience", 0))
            .drops(drops)
            .build();
    }

    public NexoBlock getBlock(String id) { return blocks.get(id); }

    public NexoBlock getBlockAt(Block block) {
        for (NexoBlock nb : blocks.values()) {
            if (nb.matches(block)) return nb;
        }
        return null;
    }

    public boolean isNexoBlock(Block block) { return getBlockAt(block) != null; }
    public Collection<NexoBlock> getAllBlocks() { return Collections.unmodifiableCollection(blocks.values()); }
    public int getLoadedCount() { return blocks.size(); }
}
