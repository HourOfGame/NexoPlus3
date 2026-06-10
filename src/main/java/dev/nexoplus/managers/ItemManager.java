package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoItem;
import dev.nexoplus.items.NexoItem.FurnitureProperties;
import dev.nexoplus.items.NexoItem.AttributeModifierData;
import dev.nexoplus.items.NexoItem.ToolType;
import dev.nexoplus.mechanics.ItemMechanic;
import dev.nexoplus.mechanics.MechanicFactory;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all custom NexoPlus items.
 * Scans and loads item definitions from /items/ directory (recursive).
 */
public class ItemManager {

    private final NexoPlus plugin;

    // id -> NexoItem mapping
    private final Map<String, NexoItem> items = new ConcurrentHashMap<>();

    // customModelData -> id (for quick lookup)
    private final Map<Integer, String> cmdToId = new ConcurrentHashMap<>();

    // material -> list of items using that material
    private final Map<Material, List<String>> materialToItems = new ConcurrentHashMap<>();

    private File itemsFolder;

    public ItemManager(NexoPlus plugin) {
        this.plugin = plugin;
    }

    public void loadItems() {
        items.clear();
        cmdToId.clear();
        materialToItems.clear();

        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            createDefaultItems();
        }

        int loaded = scanAndLoadDirectory(itemsFolder);
        plugin.getLogger().info("Loaded " + loaded + " custom items.");
    }

    public void reload() {
        loadItems();
    }

    /**
     * Recursively scan directory for item YAML files.
     */
    private int scanAndLoadDirectory(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                count += scanAndLoadDirectory(file);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                count += loadItemFile(file);
            }
        }
        return count;
    }

    /**
     * Load all items from a single YAML file.
     * One file can define many items.
     */
    private int loadItemFile(File file) {
        int count = 0;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Determine namespace from file path relative to items folder
        String relPath = itemsFolder.toURI().relativize(file.toURI()).getPath();
        String namespace = relPath.contains("/")
                ? relPath.substring(0, relPath.lastIndexOf("/")).replace("/", "_")
                : "default";

        for (String key : config.getKeys(false)) {
            if (!config.isConfigurationSection(key)) continue;
            ConfigurationSection section = config.getConfigurationSection(key);

            try {
                NexoItem item = parseItem(namespace, key, section);
                registerItem(item);
                count++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load item '" + key + "' in " + file.getName() + ": " + e.getMessage(), e);
            }
        }
        return count;
    }

    /**
     * Parse a ConfigurationSection into a NexoItem.
     */
    private NexoItem parseItem(String namespace, String itemName, ConfigurationSection cfg) {
        String id = namespace + ":" + itemName;

        // === BASE ===
        Material material = Material.valueOf(cfg.getString("material", "PAPER").toUpperCase());
        int customModelData = cfg.getInt("Pack.custom_model_data", cfg.getInt("custom_model_data", 0));
        String displayName = cfg.getString("display_name", cfg.getString("displayname", itemName));
        List<String> lore = cfg.getStringList("lore");

        // === RESOURCEPACK ===
        String texturePath = cfg.getString("Pack.textures", cfg.getString("texture", null));
        String modelPath = cfg.getString("Pack.model", null);
        boolean generate3D = cfg.getBoolean("Pack.generate_3d_model", false);

        // === ITEM PROPERTIES ===
        boolean unbreakable = cfg.getBoolean("unbreakable", false);
        int durability = cfg.getInt("durability", -1);
        boolean glow = cfg.getBoolean("glow", false);

        // === MAX STACK SIZE ===
        int maxStackSize = cfg.getInt("max_stack_size", 64);

        // === ENCHANTMENTS ===
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchSection = cfg.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(
                        org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase()));
                if (ench != null) {
                    enchantments.put(ench, enchSection.getInt(enchName));
                }
            }
        }

        // === ITEM FLAGS ===
        Set<ItemFlag> itemFlags = new HashSet<>();
        List<String> flagList = cfg.getStringList("item_flags");
        for (String flagName : flagList) {
            try {
                itemFlags.add(ItemFlag.valueOf(flagName.toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (cfg.getBoolean("hide_enchantments", false)) itemFlags.add(ItemFlag.HIDE_ENCHANTS);
        if (cfg.getBoolean("hide_attributes", false)) itemFlags.add(ItemFlag.HIDE_ATTRIBUTES);
        if (cfg.getBoolean("hide_unbreakable", false)) itemFlags.add(ItemFlag.HIDE_UNBREAKABLE);

        // === ATTRIBUTES ===
        Map<Attribute, AttributeModifierData> attributes = new HashMap<>();
        ConfigurationSection attrSection = cfg.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String attrName : attrSection.getKeys(false)) {
                try {
                    Attribute attr = Attribute.valueOf(attrName.toUpperCase());
                    ConfigurationSection attrData = attrSection.getConfigurationSection(attrName);
                    if (attrData != null) {
                        AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(
                                attrData.getString("operation", "ADD_NUMBER").toUpperCase());
                        double amount = attrData.getDouble("amount", 0.0);
                        String slotStr = attrData.getString("slot", "HAND");
                        EquipmentSlot slot = EquipmentSlot.valueOf(slotStr.toUpperCase());
                        attributes.put(attr, AttributeModifierData.builder()
                                .amount(amount).operation(op).slot(slot).build());
                    }
                } catch (Exception ignored) {}
            }
        }

        // === TAGS ===
        Set<String> tags = new HashSet<>(cfg.getStringList("tags"));

        // === ARMOR ===
        boolean isArmor = cfg.getBoolean("armor.enabled", false);
        String armorTexturePath = cfg.getString("armor.texture", null);
        String armorColor = cfg.getString("armor.color", null);

        // === FOOD ===
        boolean isFood = cfg.getBoolean("food.enabled", false);
        int foodValue = cfg.getInt("food.nutrition", 0);
        float satValue = (float) cfg.getDouble("food.saturation", 0.0);
        boolean canAlwaysEat = cfg.getBoolean("food.can_always_eat", false);

        // === TOOL ===
        boolean isTool = cfg.getBoolean("tool.enabled", false);
        ToolType toolType = null;
        if (isTool) {
            try { toolType = ToolType.valueOf(cfg.getString("tool.type", "PICKAXE").toUpperCase()); }
            catch (Exception ignored) {}
        }
        int toolLevel = cfg.getInt("tool.level", 0);
        float miningSpeed = (float) cfg.getDouble("tool.mining_speed", 1.0);

        // === WEAPON ===
        boolean isWeapon = cfg.getBoolean("weapon.enabled", false);
        double attackDamage = cfg.getDouble("weapon.attack_damage", 1.0);
        double attackSpeed = cfg.getDouble("weapon.attack_speed", 4.0);

        // === FURNITURE ===
        boolean isFurniture = cfg.getBoolean("furniture.enabled", false);
        FurnitureProperties furnitureProps = null;
        if (isFurniture) {
            ConfigurationSection fSection = cfg.getConfigurationSection("furniture");
            if (fSection != null) {
                furnitureProps = FurnitureProperties.builder()
                        .solid(fSection.getBoolean("solid", false))
                        .hasHitbox(fSection.getBoolean("hitbox.enabled", true))
                        .hitboxType(fSection.getString("hitbox.type", "SMALL"))
                        .hitboxWidth(fSection.getDouble("hitbox.width", 0.5))
                        .hitboxHeight(fSection.getDouble("hitbox.height", 0.5))
                        .hasLight(fSection.getBoolean("light.enabled", false))
                        .lightLevel(fSection.getInt("light.level", 0))
                        .rotatable(fSection.getBoolean("rotatable", true))
                        .breakSound(fSection.getString("break_sound", null))
                        .placeSound(fSection.getString("place_sound", null))
                        .build();
            }
        }

        // === MECHANICS ===
        List<ItemMechanic> mechanics = new ArrayList<>();
        ConfigurationSection mechSection = cfg.getConfigurationSection("mechanics");
        if (mechSection != null) {
            for (String mechName : mechSection.getKeys(false)) {
                ConfigurationSection mechData = mechSection.getConfigurationSection(mechName);
                ItemMechanic mechanic = MechanicFactory.create(mechName, mechData);
                if (mechanic != null) mechanics.add(mechanic);
            }
        }

        // === CUSTOM NBT ===
        Map<String, String> customNBT = new HashMap<>();
        ConfigurationSection nbtSection = cfg.getConfigurationSection("custom_nbt");
        if (nbtSection != null) {
            for (String nbtKey : nbtSection.getKeys(false)) {
                customNBT.put(nbtKey, nbtSection.getString(nbtKey, ""));
            }
        }

        // === EVENTS ===
        ConfigurationSection events = cfg.getConfigurationSection("events");

        return NexoItem.builder()
                .id(id)
                .namespace(namespace)
                .itemName(itemName)
                .material(material)
                .customModelData(customModelData)
                .displayName(displayName)
                .lore(lore)
                .texturePath(texturePath)
                .modelPath(modelPath)
                .generate3DModel(generate3D)
                .unbreakable(unbreakable)
                .durability(durability)
                .glow(glow)
                .maxStackSize(maxStackSize)
                .enchantments(enchantments)
                .attributes(attributes)
                .itemFlags(itemFlags)
                .tags(tags)
                .isArmor(isArmor)
                .armorTexturePath(armorTexturePath)
                .armorColor(armorColor)
                .isFood(isFood)
                .foodValue(foodValue)
                .saturationValue(satValue)
                .canAlwaysEat(canAlwaysEat)
                .isTool(isTool)
                .toolType(toolType)
                .toolLevel(toolLevel)
                .miningSpeed(miningSpeed)
                .isWeapon(isWeapon)
                .attackDamage(attackDamage)
                .attackSpeed(attackSpeed)
                .isFurniture(isFurniture)
                .furnitureProperties(furnitureProps)
                .mechanics(mechanics)
                .customNBT(customNBT)
                .onRightClick(events != null ? events.getString("right_click") : null)
                .onLeftClick(events != null ? events.getString("left_click") : null)
                .onEquip(events != null ? events.getString("equip") : null)
                .onBreakBlock(events != null ? events.getString("break_block") : null)
                .onAttackEntity(events != null ? events.getString("attack_entity") : null)
                .usePermission(cfg.getString("permissions.use", null))
                .pickupPermission(cfg.getString("permissions.pickup", null))
                .dropPermission(cfg.getString("permissions.drop", null))
                .build();
    }

    private void registerItem(NexoItem item) {
        items.put(item.getId(), item);
        if (item.getCustomModelData() > 0) {
            cmdToId.put(item.getCustomModelData(), item.getId());
        }
        materialToItems.computeIfAbsent(item.getMaterial(), k -> new ArrayList<>())
                .add(item.getId());
    }

    // ===== PUBLIC API =====

    public NexoItem getItem(String id) {
        return items.get(id);
    }

    public NexoItem getItemByCmd(Material material, int customModelData) {
        String id = cmdToId.get(customModelData);
        if (id == null) return null;
        NexoItem item = items.get(id);
        if (item != null && item.getMaterial() == material) return item;
        return null;
    }

    public NexoItem getItemFromStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        String id = NexoItem.getItemId(stack, plugin);
        if (id == null) return null;
        return items.get(id);
    }

    public boolean isNexoItem(ItemStack stack) {
        return getItemFromStack(stack) != null;
    }

    public ItemStack buildItemStack(String id) {
        NexoItem item = getItem(id);
        if (item == null) return null;
        return item.buildItemStack(plugin);
    }

    public ItemStack buildItemStack(String id, int amount) {
        ItemStack stack = buildItemStack(id);
        if (stack == null) return null;
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));
        return stack;
    }

    public Collection<NexoItem> getAllItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public List<String> getItemIdsByMaterial(Material material) {
        return Collections.unmodifiableList(
                materialToItems.getOrDefault(material, Collections.emptyList()));
    }

    public List<NexoItem> getItemsByTag(String tag) {
        List<NexoItem> result = new ArrayList<>();
        for (NexoItem item : items.values()) {
            if (item.getTags() != null && item.getTags().contains(tag)) {
                result.add(item);
            }
        }
        return result;
    }

    public int getLoadedCount() {
        return items.size();
    }

    public Set<String> getAllIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    /**
     * Get next available CustomModelData for a material.
     * Automatically assigns CMD values to avoid conflicts.
     */
    public int getNextAvailableCMD(Material material) {
        Set<Integer> usedCMDs = new HashSet<>();
        for (String id : materialToItems.getOrDefault(material, Collections.emptyList())) {
            NexoItem item = items.get(id);
            if (item != null && item.getCustomModelData() > 0) {
                usedCMDs.add(item.getCustomModelData());
            }
        }
        int next = 1;
        while (usedCMDs.contains(next)) next++;
        return next;
    }

    private void createDefaultItems() {
        // Create example items YAML
        File exampleFile = new File(itemsFolder, "example_items.yml");
        plugin.saveResource("items/example_items.yml", false);
    }
}
