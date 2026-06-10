package dev.nexoplus.managers;

import com.google.gson.*;
import dev.nexoplus.core.NexoPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlyphManager - Apply images to font characters (HUD, icons, emojis).
 *
 * Extremely simple API:
 *
 * In your glyphs.yml:
 *   my_heart:
 *     texture: gui/heart.png   # put in content/mypack/pack/textures/font/
 *     height: 18               # display size in pixels
 *     ascent: 8                # vertical offset (-16 to 16)
 *     char: "\uE001"           # Unicode char to use
 *
 * Then use in any display name, lore, or chat:
 *   display_name: "<glyph:my_heart> Health"
 *
 * NexoPlus auto-generates the font JSON in the resourcepack.
 * Much simpler than ItemsAdder's bitmap font setup!
 */
public class GlyphManager {

    private final NexoPlus plugin;
    private final Map<String, GlyphData> glyphs = new ConcurrentHashMap<>();

    // Auto-assign unicode chars starting from Private Use Area
    private static final int CHAR_START = 0xE000;
    private int nextCharCode = CHAR_START;

    public GlyphManager(NexoPlus plugin) {
        this.plugin = plugin;
    }

    public void loadGlyphs() {
        glyphs.clear();
        nextCharCode = CHAR_START;

        // Scan all content packs for glyph configs
        File contentFolder = new File(plugin.getDataFolder(), "content");
        if (!contentFolder.exists()) return;

        for (File packFolder : Objects.requireNonNull(contentFolder.listFiles(File::isDirectory))) {
            File packData = new File(packFolder, packFolder.getName());
            File glyphsFile = new File(packData.exists() ? packData : packFolder, "glyphs.yml");

            if (!glyphsFile.exists()) continue;

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(glyphsFile);
            String namespace = packFolder.getName();

            for (String key : cfg.getKeys(false)) {
                if (!cfg.isConfigurationSection(key)) continue;
                ConfigurationSection s = cfg.getConfigurationSection(key);
                loadGlyph(namespace, key, s);
            }
        }

        // Also load from /glyphs/ folder (legacy)
        File glyphsFolder = new File(plugin.getDataFolder(), "glyphs");
        if (glyphsFolder.exists()) {
            for (File f : Objects.requireNonNull(glyphsFolder.listFiles(
                    file -> file.getName().endsWith(".yml")))) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                for (String key : cfg.getKeys(false)) {
                    if (!cfg.isConfigurationSection(key)) continue;
                    loadGlyph("nexoplus", key, cfg.getConfigurationSection(key));
                }
            }
        }

        plugin.getLogger().info("Loaded " + glyphs.size() + " glyphs.");
    }

    private void loadGlyph(String namespace, String id, ConfigurationSection s) {
        String fullId = namespace + ":" + id;

        // Auto-assign char if not set
        String charStr = s.getString("char", null);
        char glyphChar;
        if (charStr != null && !charStr.isEmpty()) {
            glyphChar = charStr.charAt(0);
        } else {
            glyphChar = (char) nextCharCode++;
        }

        GlyphData glyph = new GlyphData(
            fullId, namespace, id, glyphChar,
            s.getString("texture", "font/" + id + ".png"),
            s.getInt("height", 16),
            s.getInt("ascent", 8)
        );

        glyphs.put(fullId, glyph);
        glyphs.put(id, glyph); // short ID lookup
    }

    // ================================================================
    // FONT JSON GENERATION
    // ================================================================

    /**
     * Generate the font JSON providers for the resourcepack.
     * Called by ResourcePackManager during pack build.
     */
    public JsonArray buildFontProviders(String namespace) {
        JsonArray providers = new JsonArray();

        for (GlyphData glyph : glyphs.values()) {
            if (!glyph.getNamespace().equals(namespace)) continue;

            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", namespace + ":font/" + glyph.getId() + ".png");
            provider.addProperty("ascent", glyph.getAscent());
            provider.addProperty("height", glyph.getHeight());

            JsonArray chars = new JsonArray();
            chars.add(String.valueOf(glyph.getGlyphChar()));
            provider.add("chars", chars);

            providers.add(provider);
        }

        return providers;
    }

    /**
     * Copy glyph textures from content pack into resourcepack output.
     */
    public void copyGlyphTextures(String namespace, File destFontFolder) throws IOException {
        File contentFolder = new File(plugin.getDataFolder(), "content");

        for (GlyphData glyph : glyphs.values()) {
            if (!glyph.getNamespace().equals(namespace)) continue;

            // Find texture in content pack
            File packData = new File(contentFolder, namespace + "/" + namespace);
            File texturesSrc = new File(packData.exists() ? packData : new File(contentFolder, namespace),
                    "pack/textures");
            File texFile = new File(texturesSrc, glyph.getTexturePath());

            if (!texFile.exists()) {
                texFile = new File(texturesSrc, "font/" + glyph.getId() + ".png");
            }

            if (texFile.exists()) {
                destFontFolder.mkdirs();
                Files.copy(texFile.toPath(),
                        new File(destFontFolder, glyph.getId() + ".png").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ================================================================
    // TEXT PARSING
    // ================================================================

    /**
     * Parse <glyph:id> tags in text and replace with actual glyph characters.
     * Works in display names, lore, chat, titles, etc.
     *
     * Example: "<glyph:my_pack:heart> Health" → "♥ Health"
     */
    public String parseGlyphs(String text) {
        if (text == null || !text.contains("<glyph:")) return text;

        StringBuilder sb = new StringBuilder(text);
        int start;
        while ((start = sb.indexOf("<glyph:")) != -1) {
            int end = sb.indexOf(">", start);
            if (end == -1) break;

            String glyphId = sb.substring(start + 7, end);
            GlyphData glyph = glyphs.get(glyphId);

            if (glyph != null) {
                sb.replace(start, end + 1, String.valueOf(glyph.getGlyphChar()));
            } else {
                sb.replace(start, end + 1, "");
            }
        }
        return sb.toString();
    }

    /**
     * Get a glyph as an Adventure Component for modern text handling.
     */
    public Component getGlyphComponent(String glyphId) {
        GlyphData glyph = glyphs.get(glyphId);
        if (glyph == null) return Component.empty();
        return Component.text(String.valueOf(glyph.getGlyphChar()));
    }

    /**
     * Get a colored glyph component.
     */
    public Component getGlyphComponent(String glyphId, TextColor color) {
        GlyphData glyph = glyphs.get(glyphId);
        if (glyph == null) return Component.empty();
        return Component.text(String.valueOf(glyph.getGlyphChar())).color(color);
    }

    public GlyphData getGlyph(String id) { return glyphs.get(id); }
    public Collection<GlyphData> getAllGlyphs() { return Collections.unmodifiableCollection(glyphs.values()); }
    public int getLoadedCount() { return glyphs.size(); }

    // ================================================================
    // GLYPH DATA
    // ================================================================

    public static class GlyphData {
        private final String fullId;
        private final String namespace;
        private final String id;
        private final char glyphChar;
        private final String texturePath;
        private final int height;
        private final int ascent;

        public GlyphData(String fullId, String namespace, String id, char glyphChar,
                         String texturePath, int height, int ascent) {
            this.fullId = fullId;
            this.namespace = namespace;
            this.id = id;
            this.glyphChar = glyphChar;
            this.texturePath = texturePath;
            this.height = height;
            this.ascent = ascent;
        }

        public String getFullId()       { return fullId; }
        public String getNamespace()    { return namespace; }
        public String getId()           { return id; }
        public char   getGlyphChar()    { return glyphChar; }
        public String getTexturePath()  { return texturePath; }
        public int    getHeight()       { return height; }
        public int    getAscent()       { return ascent; }

        @Override
        public String toString() {
            return "Glyph{id=" + fullId + ", char=" + (int)glyphChar + ", size=" + height + "}";
        }
    }
}
