package dev.nexoplus.content;

import lombok.Data;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one content pack inside the /content/ folder.
 *
 * Structure:
 * content/
 *   my_pack/
 *     config.yml          ← pack identity (namespace, author, description)
 *     my_pack/            ← same name as parent folder
 *       pack/             ← textures, models, sounds go here
 *         textures/
 *           item/
 *             my_sword.png
 *         models/
 *           item/
 *             my_sword.json   (optional, auto-generated if missing)
 *         sounds/
 *             my_sound.ogg
 *       items/            ← item definition YAMLs
 *         weapons.yml
 *       blocks/           ← block definition YAMLs
 *         ores.yml
 *       recipes/          ← recipe YAMLs
 *         recipes.yml
 */
@Data
public class ContentPack {

    private final String packId;          // folder name, e.g. "my_pack"
    private final File rootFolder;        // content/my_pack/

    private String namespace;             // from config.yml, defaults to packId
    private String displayName;
    private String author;
    private String description;
    private String version;
    private boolean enabled;

    // Sub-folders
    private File configFile;             // config.yml
    private File packDataFolder;         // my_pack/my_pack/
    private File packAssetsFolder;       // my_pack/my_pack/pack/
    private File texturesFolder;         // pack/textures/
    private File modelsFolder;           // pack/models/
    private File soundsFolder;           // pack/sounds/
    private File itemsFolder;            // my_pack/my_pack/items/
    private File blocksFolder;           // my_pack/my_pack/blocks/
    private File recipesFolder;          // my_pack/my_pack/recipes/
    private File fontsFolder;            // pack/font/

    // Discovered assets
    private List<File> textureFiles = new ArrayList<>();
    private List<File> modelFiles   = new ArrayList<>();
    private List<File> soundFiles   = new ArrayList<>();
    private List<File> itemConfigs  = new ArrayList<>();
    private List<File> blockConfigs = new ArrayList<>();
    private List<File> recipeConfigs= new ArrayList<>();
}
