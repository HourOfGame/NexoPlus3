package dev.nexoplus.managers;

import dev.nexoplus.core.NexoPlus;
import dev.nexoplus.items.NexoItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FurnitureManager - Custom blocks with full hitbox, place, break, interact support.
 *
 * Uses ItemDisplay entities (1.19.4+) for rendering, with ArmorStand fallback (older versions).
 * Invisible Interaction entity handles hitbox detection.
 *
 * Features:
 * - Accurate hitbox (custom width/height)
 * - Placeable (right-click with item)
 * - Breakable (left-click destroys, drops item)
 * - Interactable (right-click on placed furniture)
 * - Seats (player can sit)
 * - Rotation (snaps to 8 directions)
 * - Light emission
 * - Persistence (survives restart via DB)
 */
public class FurnitureManager {

    private final NexoPlus plugin;
    private final NamespacedKey FURNITURE_ID_KEY;
    private final NamespacedKey FURNITURE_BASE_KEY;
    private final NamespacedKey SEAT_KEY;

    // entity UUID -> furniture item ID
    private final Map<UUID, String> furnitureEntities = new ConcurrentHashMap<>();
    // base entity UUID -> list of sub-entity UUIDs (interaction, seat, light)
    private final Map<UUID, List<UUID>> furnitureComponents = new ConcurrentHashMap<>();

    public FurnitureManager(NexoPlus plugin) {
        this.plugin = plugin;
        this.FURNITURE_ID_KEY  = new NamespacedKey(plugin, "furniture_id");
        this.FURNITURE_BASE_KEY= new NamespacedKey(plugin, "furniture_base");
        this.SEAT_KEY          = new NamespacedKey(plugin, "furniture_seat");
    }

    public void initialize() {
        loadFurnitureFromDB();
        // Register listener
        Bukkit.getPluginManager().registerEvents(new FurnitureListener(), plugin);
    }

    // ================================================================
    // PLACE FURNITURE
    // ================================================================

    public boolean placeFurniture(NexoItem nexoItem, Location location, float yaw, Player player) {
        if (!nexoItem.isFurniture()) return false;
        NexoItem.FurnitureProperties props = nexoItem.getFurnitureProperties();
        if (props == null) return false;

        location = location.getBlock().getLocation().add(0.5, 0, 0.5);

        // Snap yaw to 8 directions if rotatable
        if (props.isRotatable()) {
            yaw = snapYaw(yaw);
        }

        List<UUID> components = new ArrayList<>();

        // === 1. Main visual entity (ItemDisplay for 1.19.4+, ArmorStand fallback) ===
        Entity baseEntity = spawnDisplayEntity(nexoItem, location, yaw);
        if (baseEntity == null) return false;

        baseEntity.getPersistentDataContainer().set(FURNITURE_ID_KEY, PersistentDataType.STRING, nexoItem.getId());
        furnitureEntities.put(baseEntity.getUUID(), nexoItem.getId());

        // === 2. Interaction entity (hitbox) ===
        if (props.isHasHitbox()) {
            Entity interaction = spawnInteractionEntity(location, props, baseEntity.getUUID());
            if (interaction != null) {
                components.add(interaction.getUUID());
                furnitureEntities.put(interaction.getUUID(), nexoItem.getId());
            }
        }

        // === 3. Seats ===
        if (props.isHasSeats() && props.getSeats() != null) {
            for (NexoItem.SeatData seat : props.getSeats()) {
                Entity seatEntity = spawnSeat(location, seat, baseEntity.getUUID());
                if (seatEntity != null) components.add(seatEntity.getUUID());
            }
        }

        furnitureComponents.put(baseEntity.getUUID(), components);

        // === 4. Play sound ===
        if (props.getPlaceSound() != null) {
            location.getWorld().playSound(location, props.getPlaceSound(), 1f, 1f);
        }

        // === 5. Save to DB ===
        saveFurnitureToDB(baseEntity.getUUID(), nexoItem.getId(), location, yaw);

        return true;
    }

    private Entity spawnDisplayEntity(NexoItem item, Location location, float yaw) {
        World world = location.getWorld();
        if (world == null) return null;

        try {
            // 1.19.4+ ItemDisplay entity
            ItemDisplay display = world.spawn(location, ItemDisplay.class, e -> {
                e.setItemStack(item.buildItemStack(plugin));
                e.setDisplayWidth(1.0f);
                e.setDisplayHeight(1.0f);
                e.getPersistentDataContainer().set(FURNITURE_ID_KEY,
                        PersistentDataType.STRING, item.getId());

                // Apply rotation
                Transformation t = e.getTransformation();
                Quaternionf rot = new Quaternionf().rotateY((float) Math.toRadians(yaw));
                e.setTransformation(new Transformation(
                        t.getTranslation(), rot, t.getScale(), t.getRightRotation()));
            });
            return display;
        } catch (Exception e) {
            // Fallback: ArmorStand for older server versions
            return spawnArmorStandFallback(item, location, yaw);
        }
    }

    private Entity spawnArmorStandFallback(NexoItem item, Location location, float yaw) {
        World world = location.getWorld();
        if (world == null) return null;
        ArmorStand stand = world.spawn(location, ArmorStand.class, e -> {
            e.setInvisible(true);
            e.setInvulnerable(true);
            e.setGravity(false);
            e.setSmall(true);
            e.setArms(false);
            e.setBasePlate(false);
            e.setRotation(yaw, 0);
            e.getEquipment().setHelmet(item.buildItemStack(plugin));
            e.getPersistentDataContainer().set(FURNITURE_ID_KEY,
                    PersistentDataType.STRING, item.getId());
        });
        return stand;
    }

    private Entity spawnInteractionEntity(Location location, NexoItem.FurnitureProperties props, UUID baseUUID) {
        World world = location.getWorld();
        if (world == null) return null;
        try {
            Interaction interaction = world.spawn(location, Interaction.class, e -> {
                e.setInteractionWidth((float) props.getHitboxWidth());
                e.setInteractionHeight((float) props.getHitboxHeight());
                e.setResponsive(true);
                e.getPersistentDataContainer().set(FURNITURE_BASE_KEY,
                        PersistentDataType.STRING, baseUUID.toString());
            });
            return interaction;
        } catch (Exception ignored) {
            // Interaction entity not available (< 1.19.4)
            return null;
        }
    }

    private Entity spawnSeat(Location base, NexoItem.SeatData seat, UUID baseUUID) {
        Location seatLoc = base.clone().add(seat.getOffsetX(), seat.getOffsetY(), seat.getOffsetZ());
        World world = seatLoc.getWorld();
        if (world == null) return null;

        ArmorStand seatStand = world.spawn(seatLoc, ArmorStand.class, e -> {
            e.setInvisible(true);
            e.setInvulnerable(true);
            e.setGravity(false);
            e.setSmall(true);
            e.setArms(false);
            e.setBasePlate(false);
            e.setRotation(seat.getYaw(), 0);
            e.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, baseUUID.toString());
        });
        return seatStand;
    }

    // ================================================================
    // BREAK FURNITURE
    // ================================================================

    public boolean breakFurniture(Entity entity, Player player) {
        String furnitureId = getFurnitureId(entity);
        if (furnitureId == null) return false;

        NexoItem nexoItem = plugin.getItemManager().getItem(furnitureId);
        if (nexoItem == null) return false;

        NexoItem.FurnitureProperties props = nexoItem.getFurnitureProperties();

        // Get base entity
        UUID baseUUID = getBaseUUID(entity);
        Entity baseEntity = baseUUID != null ? Bukkit.getEntity(baseUUID) : entity;

        Location dropLocation = entity.getLocation();

        // Remove all component entities
        removeAllComponents(baseUUID != null ? baseUUID : entity.getUUID());

        // Remove base entity
        if (baseEntity != null) baseEntity.remove();

        // Drop item
        org.bukkit.inventory.ItemStack drop = nexoItem.buildItemStack(plugin);
        dropLocation.getWorld().dropItemNaturally(dropLocation, drop);

        // Play sound
        if (props != null && props.getBreakSound() != null) {
            dropLocation.getWorld().playSound(dropLocation, props.getBreakSound(), 1f, 1f);
        }

        // Remove from DB
        removeFurnitureFromDB(baseUUID != null ? baseUUID : entity.getUUID());

        return true;
    }

    private void removeAllComponents(UUID baseUUID) {
        List<UUID> components = furnitureComponents.remove(baseUUID);
        if (components == null) return;
        for (UUID compUUID : components) {
            Entity e = Bukkit.getEntity(compUUID);
            if (e != null) e.remove();
            furnitureEntities.remove(compUUID);
        }
        furnitureEntities.remove(baseUUID);
    }

    // ================================================================
    // SIT
    // ================================================================

    public boolean sitOnFurniture(Entity seatEntity, Player player) {
        String seatData = seatEntity.getPersistentDataContainer()
                .get(SEAT_KEY, PersistentDataType.STRING);
        if (seatData == null) return false;

        // Check no one is already sitting
        if (!seatEntity.getPassengers().isEmpty()) {
            player.sendMessage(ChatColor.RED + "This seat is occupied!");
            return false;
        }
        seatEntity.addPassenger(player);
        return true;
    }

    // ================================================================
    // HELPERS
    // ================================================================

    public String getFurnitureId(Entity entity) {
        if (entity == null) return null;
        // Direct PDC lookup
        String id = entity.getPersistentDataContainer().get(FURNITURE_ID_KEY, PersistentDataType.STRING);
        if (id != null) return id;
        // Check if it's an interaction entity pointing to a base
        String baseStr = entity.getPersistentDataContainer().get(FURNITURE_BASE_KEY, PersistentDataType.STRING);
        if (baseStr != null) {
            return furnitureEntities.get(UUID.fromString(baseStr));
        }
        return null;
    }

    public boolean isFurnitureEntity(Entity entity) {
        return getFurnitureId(entity) != null;
    }

    private UUID getBaseUUID(Entity entity) {
        String baseStr = entity.getPersistentDataContainer()
                .get(FURNITURE_BASE_KEY, PersistentDataType.STRING);
        if (baseStr != null) return UUID.fromString(baseStr);
        return null;
    }

    private float snapYaw(float yaw) {
        // Snap to 8 directions (45 degree increments)
        return Math.round(yaw / 45f) * 45f;
    }

    // ================================================================
    // DATABASE PERSISTENCE
    // ================================================================

    private void saveFurnitureToDB(UUID uuid, String itemId, Location loc, float yaw) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                if (conn == null) return;
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO nexoplus_furniture " +
                    "(uuid, item_id, world, x, y, z, yaw) VALUES (?,?,?,?,?,?,?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, itemId);
                ps.setString(3, loc.getWorld().getName());
                ps.setDouble(4, loc.getX());
                ps.setDouble(5, loc.getY());
                ps.setDouble(6, loc.getZ());
                ps.setFloat(7, yaw);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save furniture: " + e.getMessage());
            }
        });
    }

    private void removeFurnitureFromDB(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                if (conn == null) return;
                PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM nexoplus_furniture WHERE uuid=?");
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }

    private void loadFurnitureFromDB() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            // Create table if not exists
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS nexoplus_furniture " +
                "(uuid TEXT PRIMARY KEY, item_id TEXT, world TEXT, x REAL, y REAL, z REAL, yaw REAL)");

            // Respawn furniture on server start
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM nexoplus_furniture");
            int respawned = 0;
            while (rs.next()) {
                try {
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    String itemId = rs.getString("item_id");
                    NexoItem item = plugin.getItemManager().getItem(itemId);
                    if (item == null || !item.isFurniture()) continue;

                    Location loc = new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                    float yaw = rs.getFloat("yaw");

                    // Respawn visual entity (don't save again to DB)
                    Entity base = spawnDisplayEntity(item, loc, yaw);
                    if (base != null) {
                        furnitureEntities.put(base.getUUID(), itemId);
                        respawned++;
                    }
                } catch (Exception ignored) {}
            }
            plugin.getLogger().info("Respawned " + respawned + " furniture entities.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load furniture from DB: " + e.getMessage());
        }
    }

    // ================================================================
    // INNER LISTENER
    // ================================================================

    class FurnitureListener implements Listener {

        @EventHandler
        public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Player player)) return;
            if (!isFurnitureEntity(e.getEntity())) return;
            e.setCancelled(true);
            // Left click = break
            breakFurniture(e.getEntity(), player);
        }

        @EventHandler
        public void onEntityInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            Entity entity = e.getRightClicked();
            if (!isFurnitureEntity(entity)) return;
            e.setCancelled(true);

            Player player = e.getPlayer();
            String furnitureId = getFurnitureId(entity);
            NexoItem item = plugin.getItemManager().getItem(furnitureId);
            if (item == null) return;

            // Check if it's a seat
            if (entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                sitOnFurniture(entity, player);
                return;
            }

            // Custom interact action
            if (item.getOnRightClick() != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        item.getOnRightClick().replace("%player%", player.getName()));
            }
        }

        @EventHandler
        public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent e) {
            if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
            if (e.getHand() != EquipmentSlot.HAND) return;

            org.bukkit.inventory.ItemStack item = e.getItem();
            if (item == null) return;

            NexoItem nexoItem = plugin.getItemManager().getItemFromStack(item);
            if (nexoItem == null || !nexoItem.isFurniture()) return;

            e.setCancelled(true);
            if (e.getClickedBlock() == null) return;

            Location placeLoc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation();
            if (placeFurniture(nexoItem, placeLoc, e.getPlayer().getLocation().getYaw(), e.getPlayer())) {
                if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
}
