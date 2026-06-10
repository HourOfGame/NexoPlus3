package dev.nexoplus.items;

import dev.nexoplus.mechanics.ItemMechanic;
import dev.nexoplus.utils.ColorUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Represents a NexoPlus custom item definition.
 * Supports all ItemsAdder features + advanced capabilities.
 */
@Data
@Builder
public class NexoItem {

    // === IDENTITY ===
    private final String id;                    // Unique item ID (e.g. "my_namespace:my_sword")
    private final String namespace;             // Namespace (e.g. "my_namespace")
    private final String itemName;              // Unique item name within namespace

    // === BASE ===
    private Material material;                  // Base vanilla material
    private int customModelData;                // CustomModelData value for texture override
    private String displayName;                 // Display name (MiniMessage supported)
    private List<String> lore;                  // Lore lines (MiniMessage supported)

    // === RESOURCEPACK ===
    private String texturePath;                 // Path to texture in resourcepack
    private String modelPath;                   // Path to JSON model (optional custom)
    private boolean generate3DModel;            // Auto-generate 3D model from texture

    // === ITEM PROPERTIES ===
    private boolean unbreakable;
    private int durability;
    private boolean glow;                       // Glowing effect without enchantment
    private boolean hideEnchantments;
    private boolean hideAttributes;
    private boolean hideUnbreakable;
    private boolean hideDye;
    private boolean hidePotionEffects;
    private boolean hideArmorTrim;

    // === STACK ===
    private int maxStackSize;                   // Override max stack size (1-127)

    // === ENCHANTMENTS ===
    @Singular
    private Map<Enchantment, Integer> enchantments;

    // === ATTRIBUTES ===
    @Singular
    private Map<Attribute, AttributeModifierData> attributes;

    // === ITEM FLAGS ===
    @Singular
    private Set<ItemFlag> itemFlags;

    // === ARMOR ===
    private boolean isArmor;
    private String armorTexturePath;            // Custom armor texture (layer1, layer2)
    private String armorColor;                  // Hex color for leather armor
    private int armorEquipSound;                // Custom equip sound CMD

    // === FOOD ===
    private boolean isFood;
    private int foodValue;                      // Nutrition value
    private float saturationValue;
    private boolean canAlwaysEat;               // Eat even when not hungry
    private String eatAnimation;                // Custom eat animation

    // === TOOL ===
    private boolean isTool;
    private ToolType toolType;
    private int toolLevel;                      // Mining level
    private float miningSpeed;
    private List<Material> effectiveMaterials;

    // === WEAPON ===
    private boolean isWeapon;
    private double attackDamage;
    private double attackSpeed;
    private double attackKnockback;

    // === WEARABLE ===
    private boolean isWearable;
    private EquipmentSlot wearableSlot;

    // === FURNITURE ===
    private boolean isFurniture;
    private FurnitureProperties furnitureProperties;

    // === BLOCK ===
    private boolean isPlaceable;
    private String blockId;                     // If this item places a custom block

    // === HUD ===
    private String hudTexture;                  // Custom HUD texture when held

    // === PERMISSION ===
    private String usePermission;
    private String pickupPermission;
    private String dropPermission;

    // === MECHANICS ===
    @Singular
    private List<ItemMechanic> mechanics;       // Click, equip, trigger mechanics

    // === EVENTS ===
    private String onRightClick;                // Action when right-clicked
    private String onLeftClick;                 // Action when left-clicked
    private String onEquip;                     // Action when equipped
    private String onUnequip;                   // Action when unequipped
    private String onPickup;                    // Action when picked up
    private String onDrop;                      // Action when dropped
    private String onBreakBlock;                // Action when breaking block
    private String onAttackEntity;              // Action when attacking entity

    // === TAGS ===
    @Singular
    private Set<String> tags;                   // Custom tags for categorization/filtering

    // === METADATA ===
    private Map<String, String> customNBT;      // Extra NBT data to store

    /**
     * Builds the Bukkit ItemStack from this definition.
     */
    public ItemStack buildItemStack(Plugin plugin) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) return itemStack;

        // Display name
        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.translate(displayName));
        }

        // Lore
        if (lore != null && !lore.isEmpty()) {
            List<String> translatedLore = new ArrayList<>();
            for (String line : lore) {
                translatedLore.add(ColorUtils.translate(line));
            }
            meta.setLore(translatedLore);
        }

        // CustomModelData
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        // Unbreakable
        meta.setUnbreakable(unbreakable);

        // Enchantments
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        }

        // Glow effect (hidden enchantment)
        if (glow && (enchantments == null || enchantments.isEmpty())) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Item flags
        if (itemFlags != null && !itemFlags.isEmpty()) {
            meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        }

        // Attribute modifiers
        if (attributes != null) {
            for (Map.Entry<Attribute, AttributeModifierData> entry : attributes.entrySet()) {
                AttributeModifierData data = entry.getValue();
                org.bukkit.NamespacedKey modKey = new org.bukkit.NamespacedKey(
                        "nexoplus", entry.getKey().getKey().getKey() + "_" + UUID.randomUUID().toString().substring(0, 8));
                AttributeModifier modifier = new AttributeModifier(
                        modKey,
                        data.getAmount(),
                        data.getOperation(),
                        data.getSlot() != null
                            ? org.bukkit.inventory.EquipmentSlotGroup.ANY
                            : org.bukkit.inventory.EquipmentSlotGroup.ANY
                );
                meta.addAttributeModifier(entry.getKey(), modifier);
            }
        }

        // Store NexoPlus ID in PersistentDataContainer
        NamespacedKey nexoKey = new NamespacedKey(plugin, "nexoplus_id");
        meta.getPersistentDataContainer().set(nexoKey, PersistentDataType.STRING, id);

        // Store custom tags
        if (tags != null && !tags.isEmpty()) {
            NamespacedKey tagsKey = new NamespacedKey(plugin, "nexoplus_tags");
            meta.getPersistentDataContainer().set(tagsKey, PersistentDataType.STRING,
                    String.join(",", tags));
        }

        // Store custom NBT
        if (customNBT != null) {
            for (Map.Entry<String, String> entry : customNBT.entrySet()) {
                NamespacedKey key = new NamespacedKey(plugin, "nexoplus_" + entry.getKey());
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, entry.getValue());
            }
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Check if an ItemStack is this NexoItem.
     */
    public boolean matches(ItemStack itemStack, Plugin plugin) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey nexoKey = new NamespacedKey(plugin, "nexoplus_id");
        if (!meta.getPersistentDataContainer().has(nexoKey, PersistentDataType.STRING)) return false;

        String storedId = meta.getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
        return id.equals(storedId);
    }

    /**
     * Get NexoItem ID from an ItemStack.
     */
    public static String getItemId(ItemStack itemStack, Plugin plugin) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        NamespacedKey nexoKey = new NamespacedKey(plugin, "nexoplus_id");
        return meta.getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
    }

    /**
     * Check if an ItemStack has a specific tag.
     */
    public static boolean hasTag(ItemStack itemStack, String tag, Plugin plugin) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        NamespacedKey tagsKey = new NamespacedKey(plugin, "nexoplus_tags");
        String tagsData = meta.getPersistentDataContainer().get(tagsKey, PersistentDataType.STRING);
        if (tagsData == null) return false;

        return Arrays.asList(tagsData.split(",")).contains(tag);
    }

    // ===== INNER CLASSES =====

    @Data
    @Builder
    public static class AttributeModifierData {
        private double amount;
        private AttributeModifier.Operation operation;
        private EquipmentSlot slot;
    }

    @Data
    @Builder
    public static class FurnitureProperties {
        private boolean solid;
        private boolean hasHitbox;
        private String hitboxType;          // "SMALL", "MEDIUM", "LARGE", "CUSTOM"
        private double hitboxWidth;
        private double hitboxHeight;
        private boolean hasSeats;
        private List<SeatData> seats;
        private String breakSound;
        private String placeSound;
        private boolean hasLight;
        private int lightLevel;
        private boolean rotatable;
        private boolean wallAttachable;
        private boolean ceilingAttachable;
    }

    @Data
    @Builder
    public static class SeatData {
        private double offsetX;
        private double offsetY;
        private double offsetZ;
        private float yaw;
    }

    public enum ToolType {
        PICKAXE, AXE, SHOVEL, HOE, SWORD, SHEARS, FISHING_ROD, BOW, CROSSBOW, TRIDENT, SHIELD
    }
}
