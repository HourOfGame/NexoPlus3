package dev.nexoplus.content;

import com.google.gson.*;
import dev.nexoplus.core.NexoPlus;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * ContentManager - The heart of NexoPlus's simplicity system.
 *
 * Users just drop their content packs into /content/ and this manager:
 * 1. Scans all packs
 * 2. Reads config.yml for namespace/identity
 * 3. Indexes all textures, models, sounds, item/block/recipe configs
 * 4. Passes everything to ResourcePackManager to auto-build the .zip
 * 5. Passes item/block/recipe configs to their respective managers
 *
 * MUCH simpler than ItemsAdder - no manual CMD numbers, no manual JSON models needed!
 */
public class ContentManager {

    private final NexoPlus plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private File contentFolder;
    private final Map<String, ContentPack> loadedPacks = new ConcurrentHashMap<>();

    public ContentManager(NexoPlus plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        contentFolder = new File(plugin.getDataFolder(), "content");
        if (!contentFolder.exists()) {
            contentFolder.mkdirs();
            createExamplePack();
        }
    }

    // ================================================================
    // SCANNING
    // ================================================================

    /**
     * Scan the entire /content/ folder and load all packs.
     */
    public void loadAll() {
        loadedPacks.clear();

        File[] packFolders = contentFolder.listFiles(File::isDirectory);
        if (packFolders == null || packFolders.length == 0) {
            plugin.getLogger().info("No content packs found in /content/");
            return;
        }

        int loaded = 0;
        for (File packFolder : packFolders) {
            try {
                ContentPack pack = loadPack(packFolder);
                if (pack != null && pack.isEnabled()) {
                    loadedPacks.put(pack.getPackId(), pack);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load content pack '" + packFolder.getName() + "': " + e.getMessage(), e);
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " content pack(s): " + loadedPacks.keySet());
    }

    /**
     * Load a single content pack from its folder.
     *
     * Expected structure:
     * packFolder/
     *   config.yml
     *   packFolder/        (same name)
     *     pack/
     *       textures/
     *       models/
     *       sounds/
     *     items/
     *     blocks/
     *     recipes/
     */
    private ContentPack loadPack(File packFolder) throws Exception {
        String packId = packFolder.getName();

        ContentPack pack = new ContentPack(packId, packFolder);

        // === Read config.yml ===
        File configFile = new File(packFolder, "config.yml");
        if (!configFile.exists()) {
            // Auto-create a default config
            createDefaultPackConfig(configFile, packId);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        pack.setConfigFile(configFile);
        pack.setNamespace(config.getString("namespace", packId).toLowerCase().replace(" ", "_"));
        pack.setDisplayName(config.getString("display_name", packId));
        pack.setAuthor(config.getString("author", "Unknown"));
        pack.setDescription(config.getString("description", ""));
        pack.setVersion(config.getString("version", "1.0.0"));
        pack.setEnabled(config.getBoolean("enabled", true));

        if (!pack.isEnabled()) return pack;

        // === Find the inner data folder (same name as packId) ===
        File packDataFolder = new File(packFolder, packId);
        if (!packDataFolder.exists()) {
            // Also accept direct structure without inner folder
            packDataFolder = packFolder;
        }
        pack.setPackDataFolder(packDataFolder);

        // === Assets folder (pack/) ===
        File assetsFolder = new File(packDataFolder, "pack");
        assetsFolder.mkdirs();
        pack.setPackAssetsFolder(assetsFolder);

        File texturesFolder = new File(assetsFolder, "textures");
        File modelsFolder   = new File(assetsFolder, "models");
        File soundsFolder   = new File(assetsFolder, "sounds");
        File fontsFolder    = new File(assetsFolder, "font");

        texturesFolder.mkdirs();
        modelsFolder.mkdirs();
        soundsFolder.mkdirs();

        pack.setTexturesFolder(texturesFolder);
        pack.setModelsFolder(modelsFolder);
        pack.setSoundsFolder(soundsFolder);
        pack.setFontsFolder(fontsFolder);

        // === Config folders ===
        File itemsFolder   = new File(packDataFolder, "items");
        File blocksFolder  = new File(packDataFolder, "blocks");
        File recipesFolder = new File(packDataFolder, "recipes");

        pack.setItemsFolder(itemsFolder);
        pack.setBlocksFolder(blocksFolder);
        pack.setRecipesFolder(recipesFolder);

        // === Scan all assets ===
        scanTextureFiles(pack);
        scanModelFiles(pack);
        scanSoundFiles(pack);
        scanConfigFiles(pack);

        plugin.getLogger().info("  Pack '" + pack.getDisplayName() + "' [" + pack.getNamespace() + "]: "
                + pack.getTextureFiles().size() + " textures, "
                + pack.getItemConfigs().size() + " item configs, "
                + pack.getBlockConfigs().size() + " block configs");

        return pack;
    }

    private void scanTextureFiles(ContentPack pack) {
        if (pack.getTexturesFolder() == null || !pack.getTexturesFolder().exists()) return;
        scanForExtension(pack.getTexturesFolder(), ".png", pack.getTextureFiles());
    }

    private void scanModelFiles(ContentPack pack) {
        if (pack.getModelsFolder() == null || !pack.getModelsFolder().exists()) return;
        scanForExtension(pack.getModelsFolder(), ".json", pack.getModelFiles());
    }

    private void scanSoundFiles(ContentPack pack) {
        if (pack.getSoundsFolder() == null || !pack.getSoundsFolder().exists()) return;
        scanForExtension(pack.getSoundsFolder(), ".ogg", pack.getSoundFiles());
    }

    private void scanConfigFiles(ContentPack pack) {
        if (pack.getItemsFolder() != null && pack.getItemsFolder().exists())
            scanForExtension(pack.getItemsFolder(), ".yml", pack.getItemConfigs());
        if (pack.getBlocksFolder() != null && pack.getBlocksFolder().exists())
            scanForExtension(pack.getBlocksFolder(), ".yml", pack.getBlockConfigs());
        if (pack.getRecipesFolder() != null && pack.getRecipesFolder().exists())
            scanForExtension(pack.getRecipesFolder(), ".yml", pack.getRecipeConfigs());
    }

    private void scanForExtension(File dir, String ext, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanForExtension(f, ext, result);
            else if (f.getName().toLowerCase().endsWith(ext)) result.add(f);
        }
    }

    // ================================================================
    // RESOURCEPACK GENERATION FROM CONTENT PACKS
    // ================================================================

    /**
     * Build the resourcepack output from all loaded content packs.
     * Called by ResourcePackManager during pack generation.
     *
     * @param assetsOutputFolder  The minecraft/assets/ folder to write into
     */
    public void buildIntoResourcePack(File assetsOutputFolder) throws Exception {
        for (ContentPack pack : loadedPacks.values()) {
            buildPackAssets(pack, assetsOutputFolder);
        }
    }

    private void buildPackAssets(ContentPack pack, File assetsRoot) throws Exception {
        String ns = pack.getNamespace();

        // === 1. Copy textures ===
        if (pack.getTexturesFolder() != null && pack.getTexturesFolder().exists()) {
            File destTextures = new File(assetsRoot, ns + "/textures");
            copyDirectoryContents(pack.getTexturesFolder(), destTextures);
        }

        // === 2. Copy or auto-generate models ===
        buildItemModels(pack, assetsRoot);

        // === 3. Copy sounds ===
        if (pack.getSoundsFolder() != null && pack.getSoundsFolder().exists()) {
            File destSounds = new File(assetsRoot, ns + "/sounds");
            copyDirectoryContents(pack.getSoundsFolder(), destSounds);
        }

        // === 4. Copy fonts ===
        if (pack.getFontsFolder() != null && pack.getFontsFolder().exists()) {
            File destFonts = new File(assetsRoot, ns + "/textures/font");
            copyDirectoryContents(pack.getFontsFolder(), destFonts);
        }

        plugin.getLogger().info("Built assets for pack: " + pack.getNamespace());
    }

    /**
     * For each texture in pack/textures/item/, auto-generate a model JSON
     * pointing to that texture — user does NOT need to write model JSON manually!
     * If a model JSON already exists, use that instead.
     */
    private void buildItemModels(ContentPack pack, File assetsRoot) throws Exception {
        String ns = pack.getNamespace();
        File texturesItemFolder = new File(pack.getTexturesFolder(), "item");
        if (!texturesItemFolder.exists()) return;

        File destModelsItem = new File(assetsRoot, ns + "/models/item");
        destModelsItem.mkdirs();

        File[] textures = texturesItemFolder.listFiles(
                f -> f.getName().toLowerCase().endsWith(".png"));
        if (textures == null) return;

        // Track CMD assignments for this pack
        int cmdStart = getCmdStartForNamespace(ns);

        for (File texture : textures) {
            String baseName = texture.getName().replace(".png", "");

            // Check if user provided a custom model JSON
            File customModel = new File(pack.getModelsFolder(), "item/" + baseName + ".json");
            File destModel   = new File(destModelsItem, baseName + ".json");

            if (customModel.exists()) {
                // Use user's model
                Files.copy(customModel.toPath(), destModel.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Auto-generate flat item model
                JsonObject model = new JsonObject();
                model.addProperty("parent", "item/generated");
                JsonObject texObj = new JsonObject();
                texObj.addProperty("layer0", ns + ":item/" + baseName);
                model.add("textures", texObj);
                writeJson(destModel, model);
            }
        }

        // Auto-generate block models too
        buildBlockModels(pack, assetsRoot, ns);
    }

    private void buildBlockModels(ContentPack pack, File assetsRoot, String ns) throws Exception {
        File texturesBlockFolder = new File(pack.getTexturesFolder(), "block");
        if (!texturesBlockFolder.exists()) return;

        File destModelsBlock = new File(assetsRoot, ns + "/models/block");
        destModelsBlock.mkdirs();

        File[] textures = texturesBlockFolder.listFiles(
                f -> f.getName().toLowerCase().endsWith(".png"));
        if (textures == null) return;

        for (File texture : textures) {
            String baseName = texture.getName().replace(".png", "");
            File customModel = new File(pack.getModelsFolder(), "block/" + baseName + ".json");
            File destModel   = new File(destModelsBlock, baseName + ".json");

            if (customModel.exists()) {
                Files.copy(customModel.toPath(), destModel.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Auto-generate cube_all block model
                JsonObject model = new JsonObject();
                model.addProperty("parent", "block/cube_all");
                JsonObject texObj = new JsonObject();
                texObj.addProperty("all", ns + ":block/" + baseName);
                model.add("textures", texObj);
                writeJson(destModel, model);
            }
        }
    }

    /**
     * Auto-assign CMD range per namespace to avoid conflicts.
     * Each namespace gets a block of 1000 CMD values.
     */
    private int getCmdStartForNamespace(String ns) {
        List<String> namespaces = new ArrayList<>(loadedPacks.keySet());
        Collections.sort(namespaces);
        int idx = namespaces.indexOf(ns);
        return (idx + 1) * 1000; // ns[0]=1000, ns[1]=2000, etc.
    }

    // ================================================================
    // AUTO CMD ASSIGNMENT
    // ================================================================

    /**
     * Scan all item configs in all packs and auto-assign CMD values
     * if custom_model_data is missing. Writes back to the config file.
     */
    public void autoAssignCMDs() {
        for (ContentPack pack : loadedPacks.values()) {
            int cmdCounter = getCmdStartForNamespace(pack.getNamespace());
            for (File configFile : pack.getItemConfigs()) {
                cmdCounter = autoAssignCMDsInFile(configFile, cmdCounter);
            }
        }
    }

    private int autoAssignCMDsInFile(File configFile, int cmdCounter) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            boolean changed = false;
            for (String key : cfg.getKeys(false)) {
                if (!cfg.isConfigurationSection(key)) continue;
                var section = cfg.getConfigurationSection(key);
                int existingCmd = section.getInt("Pack.custom_model_data",
                        section.getInt("custom_model_data", 0));
                if (existingCmd == 0) {
                    section.set("Pack.custom_model_data", cmdCounter++);
                    changed = true;
                    plugin.getLogger().info("Auto-assigned CMD " + (cmdCounter - 1) + " to item '" + key + "'");
                }
            }
            if (changed) cfg.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-assign CMDs in " + configFile.getName());
        }
        return cmdCounter;
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private void copyDirectoryContents(File src, File dest) throws IOException {
        if (!src.exists()) return;
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            File target = new File(dest, f.getName());
            if (f.isDirectory()) copyDirectoryContents(f, target);
            else Files.copy(f.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeJson(File file, JsonObject json) throws Exception {
        file.getParentFile().mkdirs();
        try (Writer w = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(json, w);
        }
    }

    // ================================================================
    // EXAMPLE PACK CREATION
    // ================================================================

    private void createExamplePack() {
        try {
            File exampleRoot = new File(contentFolder, "example_pack");
            File exampleData = new File(exampleRoot, "example_pack");
            File packFolder  = new File(exampleData, "pack");
            File itemTextures = new File(packFolder, "textures/item");
            File blockTextures= new File(packFolder, "textures/block");
            File itemsDir    = new File(exampleData, "items");
            File blocksDir   = new File(exampleData, "blocks");
            File recipesDir  = new File(exampleData, "recipes");

            itemTextures.mkdirs();
            blockTextures.mkdirs();
            itemsDir.mkdirs();
            blocksDir.mkdirs();
            recipesDir.mkdirs();

            // config.yml
            File configFile = new File(exampleRoot, "config.yml");
            try (Writer w = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                w.write("# NexoPlus Content Pack Config\n");
                w.write("namespace: example_pack\n");
                w.write("display_name: \"Example Pack\"\n");
                w.write("author: \"You\"\n");
                w.write("description: \"Example content pack\"\n");
                w.write("version: \"1.0.0\"\n");
                w.write("enabled: true\n");
            }

            // example item config
            File itemConfig = new File(itemsDir, "items.yml");
            try (Writer w = new FileWriter(itemConfig, StandardCharsets.UTF_8)) {
                w.write("# Example item - put ruby_sword.png in pack/textures/item/\n");
                w.write("ruby_sword:\n");
                w.write("  material: DIAMOND_SWORD\n");
                w.write("  display_name: \"<red>Ruby Sword\"\n");
                w.write("  # custom_model_data: auto-assigned if left out!\n");
                w.write("  lore:\n");
                w.write("    - \"<gray>A sword made of ruby\"\n");
                w.write("  glow: true\n");
                w.write("  unbreakable: true\n");
                w.write("  attributes:\n");
                w.write("    GENERIC_ATTACK_DAMAGE:\n");
                w.write("      amount: 12.0\n");
                w.write("      operation: ADD_NUMBER\n");
                w.write("      slot: HAND\n");
            }

            // example block config
            File blockConfig = new File(blocksDir, "blocks.yml");
            try (Writer w = new FileWriter(blockConfig, StandardCharsets.UTF_8)) {
                w.write("# Example block - put ruby_ore.png in pack/textures/block/\n");
                w.write("ruby_ore:\n");
                w.write("  block_type: NOTE_BLOCK\n");
                w.write("  block_data: 1\n");
                w.write("  texture: block/ruby_ore.png\n");
                w.write("  hardness: 3.0\n");
                w.write("  required_tool: pickaxe\n");
                w.write("  drops:\n");
                w.write("    - item_id: \"example_pack:ruby_gem\"\n");
                w.write("      min_amount: 1\n");
                w.write("      max_amount: 3\n");
                w.write("      chance: 1.0\n");
            }

            // example recipe config
            File recipeConfig = new File(recipesDir, "recipes.yml");
            try (Writer w = new FileWriter(recipeConfig, StandardCharsets.UTF_8)) {
                w.write("# Example recipe\n");
                w.write("ruby_sword_recipe:\n");
                w.write("  type: CRAFTING_SHAPED\n");
                w.write("  result:\n");
                w.write("    item: \"example_pack:ruby_sword\"\n");
                w.write("    amount: 1\n");
                w.write("  shape:\n");
                w.write("    - \" R \"\n");
                w.write("    - \" R \"\n");
                w.write("    - \" S \"\n");
                w.write("  ingredients:\n");
                w.write("    R: REDSTONE\n");
                w.write("    S: STICK\n");
            }

            // README in pack folder
            File readme = new File(exampleRoot, "README.txt");
            try (Writer w = new FileWriter(readme, StandardCharsets.UTF_8)) {
                w.write("=== HOW TO USE THIS CONTENT PACK ===\n\n");
                w.write("1. Put your textures (.png) into:\n");
                w.write("   example_pack/pack/textures/item/   <- for items\n");
                w.write("   example_pack/pack/textures/block/  <- for blocks\n\n");
                w.write("2. Edit item definitions in:\n");
                w.write("   example_pack/items/items.yml\n\n");
                w.write("3. Edit block definitions in:\n");
                w.write("   example_pack/blocks/blocks.yml\n\n");
                w.write("4. Run /nexoplus generate in-game\n\n");
                w.write("That's it! NexoPlus builds everything automatically.\n");
                w.write("No need to write model JSON or assign CMD numbers manually!\n");
            }

            plugin.getLogger().info("Created example content pack at content/example_pack/");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not create example pack: " + e.getMessage());
        }
    }

    private void createDefaultPackConfig(File configFile, String packId) throws Exception {
        try (Writer w = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            w.write("namespace: " + packId + "\n");
            w.write("display_name: \"" + packId + "\"\n");
            w.write("author: Unknown\n");
            w.write("description: \"\"\n");
            w.write("version: 1.0.0\n");
            w.write("enabled: true\n");
        }
    }

    // ================================================================
    // GETTERS
    // ================================================================

    public Collection<ContentPack> getAllPacks() {
        return Collections.unmodifiableCollection(loadedPacks.values());
    }

    public ContentPack getPack(String id) {
        return loadedPacks.get(id);
    }

    public int getLoadedCount() {
        return loadedPacks.size();
    }

    public File getContentFolder() {
        return contentFolder;
    }
}
