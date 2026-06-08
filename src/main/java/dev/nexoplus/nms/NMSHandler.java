package dev.nexoplus.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * NMS abstraction layer for multi-version support.
 */
public abstract class NMSHandler {

    public static NMSHandler initialize(String version) {
        // Return a default reflection-based handler for all versions
        return new ReflectionNMSHandler();
    }

    public abstract void sendPacket(Player player, Object packet);
    public abstract Object createFakeBlock(int x, int y, int z, Object blockData);
    public abstract void setItemGlint(ItemStack item, boolean glint);
    public abstract int getProtocolVersion(Player player);

    // ===== Default Reflection-based implementation =====
    static class ReflectionNMSHandler extends NMSHandler {
        @Override
        public void sendPacket(Player player, Object packet) {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = handle.getClass().getField("playerConnection").get(handle);
                connection.getClass().getMethod("sendPacket", packet.getClass()).invoke(connection, packet);
            } catch (Exception e) {
                // Try newer field names (1.20+)
                try {
                    Object handle = player.getClass().getMethod("getHandle").invoke(player);
                    Object connection = handle.getClass().getField("connection").get(handle);
                    connection.getClass().getMethod("send", packet.getClass()).invoke(connection, packet);
                } catch (Exception ignored) {}
            }
        }

        @Override
        public Object createFakeBlock(int x, int y, int z, Object blockData) {
            return null; // Implemented via Bukkit API where possible
        }

        @Override
        public void setItemGlint(ItemStack item, boolean glint) {
            // Handled via enchantment + HIDE_ENCHANTS flag in ItemManager
        }

        @Override
        public int getProtocolVersion(Player player) {
            return 764; // Default to 1.20.1
        }
    }
}
