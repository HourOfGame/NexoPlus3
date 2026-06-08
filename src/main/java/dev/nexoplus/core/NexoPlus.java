package dev.nexoplus.core;

import dev.nexoplus.api.NexoPlusAPI;
import dev.nexoplus.commands.NexoPlusCommand;
import dev.nexoplus.commands.GiveCommand;
import dev.nexoplus.commands.ResourcePackCommand;
import dev.nexoplus.commands.RecipeCommand;
import dev.nexoplus.listeners.*;
import dev.nexoplus.managers.*;
import dev.nexoplus.nms.NMSHandler;
import dev.nexoplus.resourcepack.ResourcePackManager;
import dev.nexoplus.utils.ColorUtils;
import dev.nexoplus.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * NexoPlus - Advanced ResourcePack & Custom Content Plugin
 * Supports Minecraft 1.16.5 - 1.20.x
 *
 * @author NexoPlus Team
 * @version 1.0.0
 */
public final class NexoPlus extends JavaPlugin {

    private static NexoPlus instance;
    private static NexoPlusAPI api;

    // Managers
    private ItemManager itemManager;
    private BlockManager blockManager;
    private RecipeManager recipeManager;
    private HUDManager hudManager;
    private FontManager fontManager;
    private SoundManager soundManager;
    private MechanicsManager mechanicsManager;
    private ResourcePackManager resourcePackManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PacketManager packetManager;
    private WorldInteractionManager worldInteractionManager;

    // NMS Handler
    private NMSHandler nmsHandler;

    @Override
    public void onLoad() {
        instance = this;
        // Pre-load NMS handler before world loads
        try {
            nmsHandler = NMSHandler.initialize(VersionUtils.getServerVersion());
            getLogger().info("NMS Handler loaded for version: " + VersionUtils.getServerVersion());
        } catch (Exception e) {
            getLogger().severe("Failed to load NMS handler! Some features may not work.");
            getLogger().severe(e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        printBanner();

        // Load config first
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize NMS packet handler
        packetManager = new PacketManager(this);
        packetManager.initialize();

        // Initialize all content managers
        itemManager = new ItemManager(this);
        blockManager = new BlockManager(this);
        recipeManager = new RecipeManager(this);
        hudManager = new HUDManager(this);
        fontManager = new FontManager(this);
        soundManager = new SoundManager(this);
        mechanicsManager = new MechanicsManager(this);
        worldInteractionManager = new WorldInteractionManager(this);

        // Load all content
        loadContent();

        // Initialize ResourcePack manager
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.initialize();

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Setup API
        api = new NexoPlusAPI(this);

        // Setup PlaceholderAPI integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new dev.nexoplus.managers.PlaceholderManager(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info(ColorUtils.strip("&a✔ NexoPlus v" + getDescription().getVersion()
                + " enabled successfully in " + elapsed + "ms!"));
        getLogger().info("Items: " + itemManager.getLoadedCount()
                + " | Blocks: " + blockManager.getLoadedCount()
                + " | Recipes: " + recipeManager.getLoadedCount());
    }

    @Override
    public void onDisable() {
        // Save all data
        if (databaseManager != null) databaseManager.shutdown();
        if (resourcePackManager != null) resourcePackManager.shutdown();
        if (packetManager != null) packetManager.shutdown();

        getLogger().info("NexoPlus disabled. Goodbye!");
        instance = null;
    }

    private void loadContent() {
        getLogger().info("Loading content...");
        itemManager.loadItems();
        blockManager.loadBlocks();
        recipeManager.loadRecipes();
        hudManager.loadHUDs();
        fontManager.loadFonts();
        soundManager.loadSounds();
        mechanicsManager.loadMechanics();
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();

        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new RecipeListener(this), this);
        pm.registerEvents(new MechanicsListener(this), this);
        pm.registerEvents(new ResourcePackListener(this), this);
        pm.registerEvents(new FurnitureListener(this), this);
        pm.registerEvents(new ArmorListener(this), this);
        pm.registerEvents(new HatListener(this), this);
        pm.registerEvents(new WorldInteractionListener(this), this);

        getLogger().info("Registered " + 10 + " event listeners.");
    }

    private void registerCommands() {
        getCommand("nexoplus").setExecutor(new NexoPlusCommand(this));
        getCommand("give").setExecutor(new GiveCommand(this));
        getCommand("resourcepack").setExecutor(new ResourcePackCommand(this));
        getCommand("recipe").setExecutor(new RecipeCommand(this));
    }

    private void printBanner() {
        getLogger().info("");
        getLogger().info("  ███╗   ██╗███████╗██╗  ██╗ ██████╗ ");
        getLogger().info("  ████╗  ██║██╔════╝╚██╗██╔╝██╔═══██╗");
        getLogger().info("  ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║");
        getLogger().info("  ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║");
        getLogger().info("  ██║ ╚████║███████╗██╔╝ ██╗╚██████╔╝");
        getLogger().info("  ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ PLUS");
        getLogger().info("  Advanced ResourcePack & Custom Content v" + getDescription().getVersion());
        getLogger().info("  Running on: " + VersionUtils.getServerVersion());
        getLogger().info("");
    }

    // ===== STATIC GETTERS =====

    public static NexoPlus getInstance() {
        return instance;
    }

    public static NexoPlusAPI getAPI() {
        return api;
    }

    public ItemManager getItemManager() { return itemManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public HUDManager getHUDManager() { return hudManager; }
    public FontManager getFontManager() { return fontManager; }
    public SoundManager getSoundManager() { return soundManager; }
    public MechanicsManager getMechanicsManager() { return mechanicsManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PacketManager getPacketManager() { return packetManager; }
    public WorldInteractionManager getWorldInteractionManager() { return worldInteractionManager; }
    public NMSHandler getNMSHandler() { return nmsHandler; }

    public void reload() {
        getLogger().info("Reloading NexoPlus...");
        configManager.reload();
        itemManager.reload();
        blockManager.reload();
        recipeManager.reload();
        hudManager.reload();
        fontManager.reload();
        soundManager.reload();
        mechanicsManager.reload();
        resourcePackManager.reload();
        getLogger().info("Reload complete!");
    }
}
