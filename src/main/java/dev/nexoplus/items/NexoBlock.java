package dev.nexoplus.items;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Represents a NexoPlus custom block.
 *
 * Block types supported:
 * - NOTE_BLOCK (instrument + note combinations = 800 custom blocks)
 * - MUSHROOM_BLOCK variants (brown/red/stem = ~47 combinations)
 * - CHORUS_PLANT (16 combinations)
 * - TRIPWIRE (for leaves/transparent blocks)
 * - CAVE_VINE (light-emitting)
 *
 * Advanced: Block entity integration for tile entities
 */
@Data
@Builder
public class NexoBlock {

    // === IDENTITY ===
    private final String id;
    private final String namespace;
    private final String blockName;

    // === BLOCK MECHANISM ===
    private BlockType blockType;            // Which vanilla block to use as base
    private int blockData;                  // The instrument/note state index

    // === RESOURCEPACK ===
    private String texturePath;             // Default all-sides texture
    private String textureTop;              // Top face texture
    private String textureBottom;           // Bottom face texture
    private String textureNorth;            // North face texture
    private String textureSouth;            // South face texture
    private String textureEast;             // East face texture
    private String textureWest;             // West face texture
    private String modelPath;              // Custom model JSON path
    private boolean isTransparent;         // Render as transparent (glass-like)
    private boolean isCutout;              // Render with cutout (leaves-like)

    // === PHYSICAL PROPERTIES ===
    private float hardness;                // Mining time (default 1.0)
    private float blastResistance;
    private boolean isOccluding;           // Whether this block blocks light/rendering
    private boolean isSolid;
    private boolean isWaterloggable;
    private boolean hasGravity;            // Falls like sand/gravel
    private boolean isLightSource;
    private int lightLevel;                // 0-15

    // === SOUND ===
    private String breakSound;             // Custom break sound
    private String placeSound;             // Custom place sound
    private String stepSound;              // Custom step sound
    private String hitSound;               // Custom hit sound
    private String fallSound;              // Custom fall sound
    private float soundVolume;
    private float soundPitch;

    // === TOOL ===
    private String requiredTool;           // "pickaxe", "axe", "shovel", "none"
    private int requiredToolLevel;         // 0=wood, 1=stone, 2=iron, 3=diamond, 4=netherite
    private boolean requiresCorrectTool;

    // === DROPS ===
    @Singular
    private List<BlockDrop> drops;
    private boolean silkTouchDropsSelf;
    private int fortuneMultiplier;         // Extra drops with fortune
    private int experience;                // XP dropped when mined

    // === GROWTH (for crops) ===
    private boolean isGrowable;
    private int growthStages;
    private List<String> growthTextures;   // Textures per growth stage
    private int growthTickRate;
    private String growthRequirement;      // "light", "water", "soil"

    // === EVENTS ===
    private String onBreak;                // Action when broken
    private String onPlace;                // Action when placed
    private String onInteract;             // Action when right-clicked
    private String onStep;                 // Action when walked on
    private String onExplode;              // Action when caught in explosion
    private String onFall;                 // Action when something falls on it

    // === PERMISSIONS ===
    private String breakPermission;
    private String placePermission;
    private String interactPermission;

    // === TILE ENTITY ===
    private boolean hasTileEntity;
    private String tileEntityType;         // Storage, Furnace, etc.
    private int storageSlots;

    // === TAGS ===
    @Singular
    private List<String> tags;

    // ===== METHODS =====

    /**
     * Get the BlockData that represents this custom block in the world.
     */
    public int getBlockDataValue() { return blockData; }

    public BlockData getBlockData() {
        return switch (blockType) {
            case NOTE_BLOCK -> buildNoteBlockData();
            case BROWN_MUSHROOM_BLOCK -> buildMushroomBlockData(Material.BROWN_MUSHROOM_BLOCK);
            case RED_MUSHROOM_BLOCK -> buildMushroomBlockData(Material.RED_MUSHROOM_BLOCK);
            case MUSHROOM_STEM -> buildMushroomBlockData(Material.MUSHROOM_STEM);
            case CAVE_VINE -> buildCaveVineData();
            default -> Material.STONE.createBlockData();
        };
    }

    private BlockData buildNoteBlockData() {
        // Convert blockData index to instrument + note
        // 25 notes * number of instruments = many combinations
        var noteBlock = (org.bukkit.block.data.type.NoteBlock)
                Material.NOTE_BLOCK.createBlockData();
        int instrumentIndex = blockData / 25;
        int noteIndex = blockData % 25;

        org.bukkit.Instrument[] instruments = org.bukkit.Instrument.values();
        if (instrumentIndex < instruments.length) {
            noteBlock.setInstrument(instruments[instrumentIndex]);
        }
        noteBlock.setNote(new org.bukkit.Note(noteIndex));
        noteBlock.setPowered(false);
        return noteBlock;
    }

    private BlockData buildMushroomBlockData(Material material) {
        var mushroomBlock = (org.bukkit.block.data.MultipleFacing) material.createBlockData();
        // Encode faces from blockData bitmask
        mushroomBlock.setFace(org.bukkit.block.BlockFace.UP,    (blockData & 0x01) != 0);
        mushroomBlock.setFace(org.bukkit.block.BlockFace.DOWN,  (blockData & 0x02) != 0);
        mushroomBlock.setFace(org.bukkit.block.BlockFace.NORTH, (blockData & 0x04) != 0);
        mushroomBlock.setFace(org.bukkit.block.BlockFace.SOUTH, (blockData & 0x08) != 0);
        mushroomBlock.setFace(org.bukkit.block.BlockFace.EAST,  (blockData & 0x10) != 0);
        mushroomBlock.setFace(org.bukkit.block.BlockFace.WEST,  (blockData & 0x20) != 0);
        return mushroomBlock;
    }

    private BlockData buildCaveVineData() {
        var vine = (org.bukkit.block.data.type.CaveVinesPlant)
                Material.CAVE_VINES_PLANT.createBlockData();
        vine.setBerries(lightLevel > 0);
        return vine;
    }

    /**
     * Check if a placed block matches this NexoBlock definition.
     */
    public boolean matches(Block block) {
        if (block == null) return false;
        return block.getBlockData().equals(getBlockData());
    }

    // ===== INNER CLASSES =====

    public enum BlockType {
        NOTE_BLOCK,
        BROWN_MUSHROOM_BLOCK,
        RED_MUSHROOM_BLOCK,
        MUSHROOM_STEM,
        CAVE_VINE,
        CHORUS_PLANT,
        TRIPWIRE
    }

    @Data
    @Builder
    public static class BlockDrop {
        private String itemId;              // NexoItem ID or vanilla material
        private int minAmount;
        private int maxAmount;
        private double chance;              // 0.0 to 1.0
        private boolean needsSilkTouch;
        private int fortuneBonus;           // Additional drops per fortune level
    }
}
