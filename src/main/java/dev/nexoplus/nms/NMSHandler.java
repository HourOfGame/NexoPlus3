package dev.nexoplus.nms;

import dev.nexoplus.utils.VersionUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * NMS abstraction layer supporting 1.16.5 - 1.21.x
 * Uses reflection to work across all versions without version-specific modules.
 */
public abstract class NMSHandler {

    private static NMSHandler instance;

    public static NMSHandler initialize(String version) {
        instance = new ReflectionNMSHandler();
        return instance;
    }

    public static NMSHandler getInstance() { return instance; }

    public abstract void sendPacket(Player player, Object packet);
    public abstract void setItemGlint(ItemStack item, boolean glint);
    public abstract int getProtocolVersion(Player player);

    // ======================================================
    // Reflection-based implementation (works 1.16 - 1.21.x)
    // ======================================================
    static class ReflectionNMSHandler extends NMSHandler {

        @Override
        public void sendPacket(Player player, Object packet) {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                // Try 1.20.5+ field name first
                Object connection = tryGetField(handle, "connection");
                if (connection == null) connection = tryGetField(handle, "playerConnection");
                if (connection == null) return;

                // Try 1.20.5+ send method
                try {
                    connection.getClass().getMethod("send", packet.getClass()).invoke(connection, packet);
                } catch (NoSuchMethodException e) {
                    connection.getClass().getMethod("sendPacket", packet.getClass()).invoke(connection, packet);
                }
            } catch (Exception ignored) {}
        }

        private Object tryGetField(Object obj, String fieldName) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void setItemGlint(ItemStack item, boolean glint) {
            // Handled via enchantment + HIDE_ENCHANTS flag in ItemManager
        }

        @Override
        public int getProtocolVersion(Player player) {
            // Map Minecraft version to protocol version
            int major = VersionUtils.getMajorVersion();
            int minor = VersionUtils.getMinorVersion();
            if (major == 21 && minor >= 4) return 769;
            if (major == 21 && minor >= 2) return 768;
            if (major == 21 && minor >= 1) return 767;
            if (major == 21) return 767;
            if (major == 20 && minor >= 6) return 766;
            if (major == 20 && minor >= 5) return 766;
            if (major == 20 && minor >= 4) return 765;
            if (major == 20 && minor >= 3) return 765;
            if (major == 20 && minor >= 2) return 764;
            if (major == 20) return 763;
            if (major == 19 && minor >= 4) return 762;
            if (major == 19 && minor >= 3) return 761;
            if (major == 19 && minor >= 1) return 760;
            if (major == 19) return 759;
            if (major == 18 && minor >= 2) return 758;
            if (major == 18) return 757;
            if (major == 17 && minor >= 1) return 756;
            if (major == 17) return 755;
            return 754; // 1.16.x
        }
    }
}
