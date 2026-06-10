package dev.nexoplus.core;

import dev.nexoplus.api.NexoPlusAPI;
import dev.nexoplus.commands.*;
import dev.nexoplus.content.ContentManager;
import dev.nexoplus.listeners.*;
import dev.nexoplus.managers.*;
import dev.nexoplus.nms.NMSHandler;
import dev.nexoplus.resourcepack.ResourcePackManager;
import dev.nexoplus.utils.ColorUtils;
import dev.nexoplus.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexoPlus extends JavaPlugin {

    private static NexoPlus instance;
    private static NexoPlusAPI api;

    // Managers
    private ConfigManager configManager;
    private ContentManager contentManager;
    private ItemManager itemManager;
    private BlockManager blockManager;
    private RecipeManager recipeManager;
    private HUDManager hudManager;
    private FontManager fontManager;
    private SoundManager soundManager;
    private MechanicsManager mechanicsManager;
    private GlyphManager glyphManager;
    private FurnitureManager furnitureManager;
    private ResourcePackManager resourcePackManager;
    private DatabaseManager databaseManager;
    private PacketManager packetManager;
    private WorldInteractionManager worldInteractionManager;
    private CompatibilityManager compatibilityManager;
    private NMSHandler nmsHandler;

    @Override
    public void onLoad() {
        instance = this;
        try {
            nmsHandler = NMSHandler.initialize(VersionUtils.getServerVersion());
        } catch (Exception e) {
            getLogger().warning("NMS handler warning: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        printBanner();

        // Config
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        // Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Packet handler
        packetManager = new PacketManager(this);
        packetManager.initialize();

        // Content system (scans /content/ packs)
        contentManager = new ContentManager(this);
        contentManager.initialize();
        contentManager.loadAll();
        contentManager.autoAssignCMDs();

        // Managers
        itemManager     = new ItemManager(this);
        blockManager    = new BlockManager(this);
        recipeManager   = new RecipeManager(this);
        hudManager      = new HUDManager(this);
        fontManager     = new FontManager(this);
        soundManager    = new SoundManager(this);
        mechanicsManager= new MechanicsManager(this);
        glyphManager    = new GlyphManager(this);
        furnitureManager= new FurnitureManager(this);
        worldInteractionManager = new WorldInteractionManager(this);

        // Load content
        loadContent();

        // ResourcePack
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.initialize();

        // Compatibility
        compatibilityManager = new CompatibilityManager(this);
        compatibilityManager.initialize();

        // Furniture (after DB is ready)
        furnitureManager.initialize();

        // Listeners & Commands
        registerListeners();
        registerCommands();

        // API
        api = new NexoPlusAPI(this);

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info(ColorUtils.strip(
            "&aEnabled in " + elapsed + "ms | " +
            "Items: " + itemManager.getLoadedCount() +
            " | Blocks: " + blockManager.getLoadedCount() +
            " | Glyphs: " + glyphManager.getLoadedCount() +
            " | Packs: " + contentManager.getLoadedCount()
        ));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.shutdown();
        if (resourcePackManager != null) resourcePackManager.shutdown();
        if (packetManager != null) packetManager.shutdown();
        instance = null;
    }

    private void loadContent() {
        itemManager.loadItems();
        blockManager.loadBlocks();
        recipeManager.loadRecipes();
        hudManager.loadHUDs();
        fontManager.loadFonts();
        soundManager.loadSounds();
        mechanicsManager.loadMechanics();
        glyphManager.loadGlyphs();
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new RecipeListener(this), this);
        pm.registerEvents(new MechanicsListener(this), this);
        pm.registerEvents(new ResourcePackListener(this), this);
        pm.registerEvents(new ArmorListener(this), this);
        pm.registerEvents(new HatListener(this), this);
        pm.registerEvents(new WorldInteractionListener(this), this);
    }

    private void registerCommands() {
        getCommand("nexoplus").setExecutor(new NexoPlusCommand(this));
        getCommand("give").setExecutor(new GiveCommand(this));
        getCommand("resourcepack").setExecutor(new ResourcePackCommand(this));
        getCommand("recipe").setExecutor(new RecipeCommand(this));
    }

    private void printBanner() {
        getLogger().info("  _   _           ___  _");
        getLogger().info(" | \\ | | _____  _/ _ \\| |_   _ ___");
        getLogger().info(" |  \\| |/ _ \\ \\/ / | | | | | / __|");
        getLogger().info(" | |\\  |  __/>  <| |_| | | |_| \\__ \\");
        getLogger().info(" |_| \\_|\\___/_/\\_\\\\___/|_|\\__,_|___/ PLUS");
        getLogger().info(" v" + getDescription().getVersion() + " | MC " + VersionUtils.getFormattedVersion());
    }

    public void reload() {
        getLogger().info("Reloading NexoPlus...");
        configManager.reload();
        contentManager.loadAll();
        contentManager.autoAssignCMDs();
        itemManager.reload();
        blockManager.reload();
        recipeManager.reload();
        hudManager.reload();
        fontManager.reload();
        soundManager.reload();
        glyphManager.loadGlyphs();
        resourcePackManager.reload();
        getLogger().info("Reload complete!");
    }

    // === STATIC GETTERS ===
    public static NexoPlus getInstance() { return instance; }
    public static NexoPlusAPI getAPI()   { return api; }

    public ConfigManager getConfigManager()           { return configManager; }
    public ContentManager getContentManager()         { return contentManager; }
    public ItemManager getItemManager()               { return itemManager; }
    public BlockManager getBlockManager()             { return blockManager; }
    public RecipeManager getRecipeManager()           { return recipeManager; }
    public HUDManager getHUDManager()                 { return hudManager; }
    public FontManager getFontManager()               { return fontManager; }
    public SoundManager getSoundManager()             { return soundManager; }
    public MechanicsManager getMechanicsManager()     { return mechanicsManager; }
    public GlyphManager getGlyphManager()             { return glyphManager; }
    public FurnitureManager getFurnitureManager()     { return furnitureManager; }
    public ResourcePackManager getResourcePackManager(){ return resourcePackManager; }
    public DatabaseManager getDatabaseManager()       { return databaseManager; }
    public PacketManager getPacketManager()           { return packetManager; }
    public WorldInteractionManager getWorldInteractionManager() { return worldInteractionManager; }
    public CompatibilityManager getCompatibilityManager() { return compatibilityManager; }
    public NMSHandler getNMSHandler()                 { return nmsHandler; }
}
