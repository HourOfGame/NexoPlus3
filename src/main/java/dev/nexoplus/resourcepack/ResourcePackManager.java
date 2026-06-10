package dev.nexoplus.resourcepack;

import com.google.gson.*;
import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoBlock;
import dev.nexoplus.items.NexoItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.*;

/**
 * NexoPlus ResourcePack Generation Engine.
 *
 * Automatically generates:
 * - Item models with CustomModelData overrides
 * - Block model state overrides (NOTE_BLOCK, MUSHROOM_BLOCK)
 * - Custom armor textures (layer1, layer2)
 * - Custom sounds (sounds.json)
 * - Custom fonts (unicode/bitmap)
 * - Custom particle textures
 * - Custom HUD textures via font characters
 * - Glyph maps for emoji/icons
 *
 * Advanced features vs ItemsAdder:
 * - Auto-detects and resolves CMD conflicts
 * - Incremental pack generation (only rebuilds changed items)
 * - Texture atlas optimization
 * - Automatic 3D model generation from 2D textures
 * - Built-in pack hosting (HTTP server)
 * - SHA1 verification and forced updates
 */
public class ResourcePackManager {

    private final NexoPlus plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Pack output
    private File packFolder;
    private File packZip;
    private String packSha1;
    private String packUrl;

    // Built-in HTTP server for pack hosting
    private PackHostServer hostServer;

    // State tracking for incremental builds
    private final Map<String, Long> lastBuildTimestamps = new HashMap<>();

    public ResourcePackManager(NexoPlus plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        packFolder = new File(plugin.getDataFolder(), "resourcepack/output");
        packFolder.mkdirs();

        // Start built-in HTTP host if configured
        boolean useBuiltinHost = plugin.getConfigManager().isUseBuiltinPackHost();
        if (useBuiltinHost) {
            int port = plugin.getConfigManager().getPackHostPort();
            hostServer = new PackHostServer(plugin, port);
            try {
                hostServer.start();
                plugin.getLogger().info("Pack host server started on port " + port);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to start pack host: " + e.getMessage());
            }
        }

        // Auto-generate pack on startup if configured
        if (plugin.getConfigManager().isAutoGeneratePack()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                generatePack();
                if (plugin.getConfigManager().isAutoSendPack()) {
                    Bukkit.getScheduler().runTask(plugin, this::sendPackToAll);
                }
            });
        }
    }

    public void reload() {
        initialize();
    }

    public void shutdown() {
        if (hostServer != null) hostServer.stop();
    }

    // ===================================================================
    // PACK GENERATION
    // ===================================================================

    /**
     * Full resourcepack generation - the heart of NexoPlus.
     * Builds a complete, valid Minecraft resource pack from all registered content.
     */
    public synchronized void generatePack() {
        long start = System.currentTimeMillis();
        plugin.getLogger().info("Generating ResourcePack...");

        try {
            // 1. Setup directory structure
            File assetsFolder = new File(packFolder, "assets");
            File minecraftFolder = new File(assetsFolder, "minecraft");
            File modelsFolder = new File(minecraftFolder, "models");
            File texturesFolder = new File(minecraftFolder, "textures");
            File blockstatesFolder = new File(minecraftFolder, "blockstates");
            File soundsFolder = new File(minecraftFolder, "sounds");
            File fontsFolder = new File(minecraftFolder, "font");

            modelsFolder.mkdirs();
            texturesFolder.mkdirs();
            blockstatesFolder.mkdirs();
            soundsFolder.mkdirs();
            fontsFolder.mkdirs();

            // 2. Write pack.mcmeta
            writePackMcmeta(packFolder);

            // 3. Generate item models (CustomModelData overrides)
            generateItemModels(modelsFolder, texturesFolder);

            // 4. Generate block state overrides
            generateBlockStates(blockstatesFolder, modelsFolder, texturesFolder);

            // 5. Generate custom armor layer textures
            generateArmorTextures(texturesFolder);

            // 6. Generate custom sounds.json
            generateSoundsJson(minecraftFolder);

            // 7. Generate custom fonts (HUD elements, icons)
            generateFonts(fontsFolder, texturesFolder);

            // 8. Copy user-provided assets from /resourcepack/assets/
            copyUserAssets(assetsFolder);

            // 9. Zip the pack
            packZip = zipPack(packFolder);

            // 10. Calculate SHA1
            packSha1 = calculateSha1(packZip);

            // 11. Update host URL
            updatePackUrl();

            long elapsed = System.currentTimeMillis() - start;
            plugin.getLogger().info("ResourcePack generated in " + elapsed + "ms! SHA1: " + packSha1);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate ResourcePack!", e);
        }
    }

    // ===================================================================
    // ITEM MODELS
    // ===================================================================

    private void generateItemModels(File modelsFolder, File texturesFolder) throws Exception {
        // Group items by material
        Map<Material, List<NexoItem>> itemsByMaterial = new HashMap<>();
        for (NexoItem item : plugin.getItemManager().getAllItems()) {
            if (item.getCustomModelData() > 0) {
                itemsByMaterial.computeIfAbsent(item.getMaterial(), k -> new ArrayList<>()).add(item);
            }
        }

        File itemModelsFolder = new File(modelsFolder, "item");
        itemModelsFolder.mkdirs();

        for (Map.Entry<Material, List<NexoItem>> entry : itemsByMaterial.entrySet()) {
            Material material = entry.getKey();
            List<NexoItem> items = entry.getValue();

            // Sort by CMD
            items.sort(Comparator.comparingInt(NexoItem::getCustomModelData));

            // Build override entries
            JsonArray overrides = new JsonArray();

            for (NexoItem item : items) {
                // Copy texture to pack
                String texturePath = item.getTexturePath();
                if (texturePath != null) {
                    copyItemTexture(item, texturesFolder);
                }

                // Write custom model JSON
                String modelRef = writeItemModel(item, itemModelsFolder);

                // Add override
                JsonObject override = new JsonObject();
                JsonObject predicate = new JsonObject();
                predicate.addProperty("custom_model_data", item.getCustomModelData());
                override.add("predicate", predicate);
                override.addProperty("model", modelRef);
                overrides.add(override);
            }

            // Write the base material model with all overrides
            String materialName = material.getKey().getKey().toLowerCase();
            JsonObject baseModel = new JsonObject();
            baseModel.addProperty("parent", "item/handheld");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "item/" + materialName);
            baseModel.add("textures", textures);
            baseModel.add("overrides", overrides);

            File materialModelFile = new File(itemModelsFolder, materialName + ".json");
            writeJson(materialModelFile, baseModel);
        }
    }

    private String writeItemModel(NexoItem item, File modelsFolder) throws Exception {
        String namespace = item.getNamespace();
        String name = item.getItemName();
        String modelRef = namespace + ":item/" + name;

        File nsFolder = new File(modelsFolder.getParentFile().getParentFile(),
                "assets/" + namespace + "/models/item");
        nsFolder.mkdirs();

        File modelFile = new File(nsFolder, name + ".json");

        JsonObject model;
        if (item.getModelPath() != null) {
            // User provided custom model - use it as parent
            model = new JsonObject();
            model.addProperty("parent", item.getModelPath());
        } else if (item.isGenerate3DModel()) {
            // Auto-generate a layered cube model
            model = generateCubeModel(item);
        } else {
            // Standard 2D flat item
            model = new JsonObject();
            model.addProperty("parent", "item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", namespace + ":item/" + item.getItemName());
            model.add("textures", textures);
        }

        writeJson(modelFile, model);
        return modelRef;
    }

    private JsonObject generateCubeModel(NexoItem item) {
        // Generate a simple cube 3D model from the flat texture
        JsonObject model = new JsonObject();
        model.addProperty("parent", "block/cube_all");
        JsonObject textures = new JsonObject();
        String texRef = item.getNamespace() + ":item/" + item.getItemName();
        textures.addProperty("all", texRef);
        model.add("textures", textures);
        return model;
    }

    private void copyItemTexture(NexoItem item, File texturesFolder) throws Exception {
        String texturePath = item.getTexturePath();
        if (texturePath == null) return;

        // Source: plugin data folder
        File sourceTexture = new File(plugin.getDataFolder(), "textures/" + texturePath);
        if (!sourceTexture.exists()) return;

        // Destination: resourcepack assets
        File destFolder = new File(texturesFolder.getParentFile().getParentFile(),
                "assets/" + item.getNamespace() + "/textures/item");
        destFolder.mkdirs();
        File destFile = new File(destFolder, item.getItemName() + ".png");

        Files.copy(sourceTexture.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ===================================================================
    // BLOCK STATES
    // ===================================================================

    private void generateBlockStates(File blockstatesFolder, File modelsFolder, File texturesFolder) throws Exception {
        // Group custom blocks by their block type
        Map<String, List<NexoBlock>> blocksByType = new HashMap<>();
        for (NexoBlock block : plugin.getBlockManager().getAllBlocks()) {
            String typeKey = block.getBlockType().name().toLowerCase();
            blocksByType.computeIfAbsent(typeKey, k -> new ArrayList<>()).add(block);
        }

        // Generate note_block.json blockstate override
        if (blocksByType.containsKey("note_block")) {
            generateNoteBlockState(blocksByType.get("note_block"),
                    blockstatesFolder, modelsFolder, texturesFolder);
        }

        // Generate mushroom block overrides
        for (String mushroomType : List.of("brown_mushroom_block", "red_mushroom_block", "mushroom_stem")) {
            if (blocksByType.containsKey(mushroomType)) {
                generateMushroomBlockState(mushroomType, blocksByType.get(mushroomType),
                        blockstatesFolder, modelsFolder, texturesFolder);
            }
        }
    }

    private void generateNoteBlockState(List<NexoBlock> blocks, File blockstatesFolder,
                                         File modelsFolder, File texturesFolder) throws Exception {
        JsonObject variants = new JsonObject();

        // Add default vanilla note block variant
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", "block/note_block");
        variants.add("instrument=harp,note=0,powered=false", defaultVariant);

        for (NexoBlock block : blocks) {
            // Generate block model
            writeBlockModel(block, modelsFolder, texturesFolder);

            // Calculate instrument and note from blockData
            String[] instruments = {
                "harp","basedrum","snare","hat","bass","flute","bell","guitar","chime","xylophone",
                "iron_xylophone","cow_bell","didgeridoo","bit","banjo","pling"
            };
            int instrIdx = block.getBlockDataValue() / 25;
            int note = block.getBlockDataValue() % 25;
            String instrument = instrIdx < instruments.length ? instruments[instrIdx] : "harp";

            String variantKey = "instrument=" + instrument + ",note=" + note + ",powered=false";
            JsonObject variant = new JsonObject();
            variant.addProperty("model", block.getNamespace() + ":block/" + block.getBlockName());
            variants.add(variantKey, variant);
        }

        JsonObject blockstate = new JsonObject();
        blockstate.add("variants", variants);
        writeJson(new File(blockstatesFolder, "note_block.json"), blockstate);
    }

    private void generateMushroomBlockState(String blockTypeName, List<NexoBlock> blocks,
                                             File blockstatesFolder, File modelsFolder,
                                             File texturesFolder) throws Exception {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        for (NexoBlock block : blocks) {
            writeBlockModel(block, modelsFolder, texturesFolder);
            // Each face combination creates a variant
            int bd = block.getBlockDataValue();
            String key = buildMushroomVariantKey(bd);
            JsonObject variant = new JsonObject();
            variant.addProperty("model", block.getNamespace() + ":block/" + block.getBlockName());
            variants.add(key, variant);
        }

        blockstate.add("variants", variants);
        writeJson(new File(blockstatesFolder, blockTypeName + ".json"), blockstate);
    }

    private String buildMushroomVariantKey(int bitmask) {
        return "down=" + ((bitmask & 0x02) != 0) +
               ",east=" + ((bitmask & 0x10) != 0) +
               ",north=" + ((bitmask & 0x04) != 0) +
               ",south=" + ((bitmask & 0x08) != 0) +
               ",up=" + ((bitmask & 0x01) != 0) +
               ",west=" + ((bitmask & 0x20) != 0);
    }

    private void writeBlockModel(NexoBlock block, File modelsFolder, File texturesFolder) throws Exception {
        File nsFolder = new File(modelsFolder.getParentFile().getParentFile(),
                "assets/" + block.getNamespace() + "/models/block");
        nsFolder.mkdirs();

        // Copy textures
        copyBlockTextures(block, texturesFolder);

        // Build model JSON
        JsonObject model;
        if (block.getModelPath() != null) {
            model = new JsonObject();
            model.addProperty("parent", block.getModelPath());
        } else {
            model = buildBlockModelJson(block);
        }

        writeJson(new File(nsFolder, block.getBlockName() + ".json"), model);
    }

    private JsonObject buildBlockModelJson(NexoBlock block) {
        JsonObject model = new JsonObject();
        String ns = block.getNamespace();
        String name = block.getBlockName();

        if (block.getTextureTop() != null || block.getTextureBottom() != null) {
            // Multi-face block
            model.addProperty("parent", "block/cube");
            JsonObject textures = new JsonObject();
            String def = ns + ":block/" + name;
            textures.addProperty("up",     block.getTextureTop()    != null ? ns + ":block/" + block.getBlockName() + "_top"    : def);
            textures.addProperty("down",   block.getTextureBottom() != null ? ns + ":block/" + block.getBlockName() + "_bottom" : def);
            textures.addProperty("north",  block.getTextureNorth()  != null ? ns + ":block/" + block.getBlockName() + "_north"  : def);
            textures.addProperty("south",  block.getTextureSouth()  != null ? ns + ":block/" + block.getBlockName() + "_south"  : def);
            textures.addProperty("east",   block.getTextureEast()   != null ? ns + ":block/" + block.getBlockName() + "_east"   : def);
            textures.addProperty("west",   block.getTextureWest()   != null ? ns + ":block/" + block.getBlockName() + "_west"   : def);
            textures.addProperty("particle", def);
            model.add("textures", textures);
        } else {
            // Simple cube_all
            model.addProperty("parent", "block/cube_all");
            JsonObject textures = new JsonObject();
            textures.addProperty("all", ns + ":block/" + name);
            model.add("textures", textures);
        }

        return model;
    }

    private void copyBlockTextures(NexoBlock block, File texturesFolder) throws Exception {
        File destFolder = new File(texturesFolder.getParentFile().getParentFile(),
                "assets/" + block.getNamespace() + "/textures/block");
        destFolder.mkdirs();

        String[][] faceTextures = {
            {block.getTexturePath(),   block.getBlockName()},
            {block.getTextureTop(),    block.getBlockName() + "_top"},
            {block.getTextureBottom(), block.getBlockName() + "_bottom"},
            {block.getTextureNorth(),  block.getBlockName() + "_north"},
            {block.getTextureSouth(),  block.getBlockName() + "_south"},
            {block.getTextureEast(),   block.getBlockName() + "_east"},
            {block.getTextureWest(),   block.getBlockName() + "_west"},
        };

        for (String[] face : faceTextures) {
            if (face[0] == null) continue;
            File src = new File(plugin.getDataFolder(), "textures/" + face[0]);
            if (!src.exists()) continue;
            File dest = new File(destFolder, face[1] + ".png");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ===================================================================
    // ARMOR TEXTURES
    // ===================================================================

    private void generateArmorTextures(File texturesFolder) throws Exception {
        File armorFolder = new File(texturesFolder, "models/armor");
        armorFolder.mkdirs();

        for (NexoItem item : plugin.getItemManager().getAllItems()) {
            if (!item.isArmor() || item.getArmorTexturePath() == null) continue;

            // Copy armor layer1 and layer2 textures
            File srcLayer1 = new File(plugin.getDataFolder(), "textures/" + item.getArmorTexturePath() + "_layer_1.png");
            File srcLayer2 = new File(plugin.getDataFolder(), "textures/" + item.getArmorTexturePath() + "_layer_2.png");

            if (srcLayer1.exists()) {
                Files.copy(srcLayer1.toPath(),
                        new File(armorFolder, item.getNamespace() + "_" + item.getItemName() + "_layer_1.png").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            if (srcLayer2.exists()) {
                Files.copy(srcLayer2.toPath(),
                        new File(armorFolder, item.getNamespace() + "_" + item.getItemName() + "_layer_2.png").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ===================================================================
    // SOUNDS
    // ===================================================================

    private void generateSoundsJson(File minecraftFolder) throws Exception {
        JsonObject soundsJson = new JsonObject();

        for (dev.nexoplus.items.NexoSound sound : plugin.getSoundManager().getAllSounds()) {
            JsonObject soundEntry = new JsonObject();
            soundEntry.addProperty("category", sound.getCategory());

            JsonArray soundsArray = new JsonArray();
            for (String file : sound.getFiles()) {
                soundsArray.add(sound.getNamespace() + ":" + file);
            }
            soundEntry.add("sounds", soundsArray);

            soundsJson.add(sound.getNamespace() + "." + sound.getSoundName(), soundEntry);
        }

        writeJson(new File(minecraftFolder, "sounds.json"), soundsJson);

        // Copy actual sound files
        File soundsFolder = new File(minecraftFolder, "sounds");
        soundsFolder.mkdirs();
        for (dev.nexoplus.items.NexoSound sound : plugin.getSoundManager().getAllSounds()) {
            for (String file : sound.getFiles()) {
                File src = new File(plugin.getDataFolder(), "sounds/" + file + ".ogg");
                if (!src.exists()) continue;
                File dest = new File(soundsFolder, file + ".ogg");
                dest.getParentFile().mkdirs();
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ===================================================================
    // FONTS (HUD, Icons)
    // ===================================================================

    private void generateFonts(File fontsFolder, File texturesFolder) throws Exception {
        // default.json font for HUD elements
        JsonObject fontJson = new JsonObject();
        JsonArray providers = new JsonArray();

        for (dev.nexoplus.items.NexoFont font : plugin.getFontManager().getAllFonts()) {
            JsonObject provider = new JsonObject();
            provider.addProperty("type", font.getType());
            provider.addProperty("file", font.getNamespace() + ":" + font.getFile());
            provider.addProperty("ascent", font.getAscent());
            provider.addProperty("height", font.getHeight());

            JsonArray chars = new JsonArray();
            for (String row : font.getChars()) {
                chars.add(row);
            }
            provider.add("chars", chars);
            providers.add(provider);

            // Copy font texture
            File srcTexture = new File(plugin.getDataFolder(), "textures/font/" + font.getFile());
            if (srcTexture.exists()) {
                File destFolder = new File(texturesFolder.getParentFile().getParentFile(),
                        "assets/" + font.getNamespace() + "/textures/font");
                destFolder.mkdirs();
                Files.copy(srcTexture.toPath(),
                        new File(destFolder, font.getFile()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        fontJson.add("providers", providers);
        writeJson(new File(fontsFolder, "default.json"), fontJson);
    }

    // ===================================================================
    // PACK UTILITIES
    // ===================================================================

    private void writePackMcmeta(File packFolder) throws Exception {
        int packFormat = plugin.getConfigManager().getPackFormat();
        JsonObject mcmeta = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", packFormat);
        pack.addProperty("description", plugin.getConfigManager().getPackDescription());
        mcmeta.add("pack", pack);
        writeJson(new File(packFolder, "pack.mcmeta"), mcmeta);
    }

    private void copyUserAssets(File destAssetsFolder) throws Exception {
        File userAssetsFolder = new File(plugin.getDataFolder(), "resourcepack/assets");
        if (!userAssetsFolder.exists()) return;

        copyDirectory(userAssetsFolder, destAssetsFolder);
    }

    private void copyDirectory(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] files = src.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyDirectory(file, new File(dest, file.getName()));
                }
            }
        } else {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File zipPack(File packFolder) throws Exception {
        File zipFile = new File(plugin.getDataFolder(), "resourcepack/NexoPlus_ResourcePack.zip");
        zipFile.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectory(packFolder, packFolder, zos);
        }
        return zipFile;
    }

    private void zipDirectory(File root, File current, ZipOutputStream zos) throws Exception {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(root, file, zos);
            } else {
                String entryName = root.toURI().relativize(file.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private String calculateSha1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void updatePackUrl() {
        String configUrl = plugin.getConfigManager().getPackUrl();
        if (configUrl != null && !configUrl.isEmpty()) {
            packUrl = configUrl;
        } else if (hostServer != null) {
            packUrl = "http://localhost:" + plugin.getConfigManager().getPackHostPort()
                    + "/NexoPlus_ResourcePack.zip";
        }
    }

    private void writeJson(File file, JsonObject json) throws Exception {
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        }
    }

    // ===================================================================
    // PACK SENDING
    // ===================================================================

    public void sendPackToAll() {
        if (packUrl == null || packSha1 == null) {
            plugin.getLogger().warning("Cannot send pack: no URL or SHA1 configured.");
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPackToPlayer(player);
        }
    }

    public void sendPackToPlayer(Player player) {
        if (packUrl == null) return;
        boolean required = plugin.getConfigManager().isPackRequired();
        String prompt = plugin.getConfigManager().getPackPromptMessage();
        try {
            if (packSha1 != null) {
                // 1.21 API: setResourcePack(url, sha1String, boolean required, Component prompt)
                player.setResourcePack(packUrl, packSha1, required,
                        net.kyori.adventure.text.Component.text(prompt));
            } else {
                player.setResourcePack(packUrl);
            }
        } catch (Exception e1) {
            // Fallback: send without SHA1
            try {
                player.setResourcePack(packUrl);
            } catch (Exception e2) {
                plugin.getLogger().warning("Could not send resource pack to " + player.getName());
            }
        }
    }


    // ===== GETTERS =====

    public String getPackSha1() { return packSha1; }
    public String getPackUrl() { return packUrl; }
    public File getPackZip() { return packZip; }

    public boolean isPackGenerated() {
        return packZip != null && packZip.exists();
    }
}
