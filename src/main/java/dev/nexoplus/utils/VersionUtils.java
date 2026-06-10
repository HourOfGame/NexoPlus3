package dev.nexoplus.utils;

import org.bukkit.Bukkit;

/**
 * Version utility supporting Minecraft 1.16.5 through 1.21.x
 */
public class VersionUtils {

    public enum Version {
        v1_16(16), v1_17(17), v1_18(18), v1_19(19), v1_20(20), v1_21(21), UNKNOWN(0);
        final int major;
        Version(int major) { this.major = major; }
    }

    private static String nmsVersion;
    private static int majorVersion = -1;
    private static int minorVersion = -1;

    public static String getServerVersion() {
        if (nmsVersion == null) {
            // Paper 1.20.5+ no longer has versioned packages, use Bukkit version
            String bukkit = Bukkit.getBukkitVersion(); // e.g. "1.21.1-R0.1-SNAPSHOT"
            nmsVersion = bukkit.split("-")[0]; // "1.21.1"
        }
        return nmsVersion;
    }

    public static int getMajorVersion() {
        if (majorVersion == -1) parseVersion();
        return majorVersion;
    }

    public static int getMinorVersion() {
        if (minorVersion == -1) parseVersion();
        return minorVersion;
    }

    private static void parseVersion() {
        try {
            String[] parts = getServerVersion().split("\\.");
            majorVersion = Integer.parseInt(parts[1]); // 21
            minorVersion = parts.length > 2 ? Integer.parseInt(parts[2]) : 0; // 1
        } catch (Exception e) {
            majorVersion = 21;
            minorVersion = 0;
        }
    }

    public static boolean isAtLeast(int major, int minor) {
        return getMajorVersion() > major || (getMajorVersion() == major && getMinorVersion() >= minor);
    }

    public static boolean isAtLeast121() { return isAtLeast(21, 0); }
    public static boolean isAtLeast120() { return isAtLeast(20, 0); }
    public static boolean isAtLeast119() { return isAtLeast(19, 0); }
    public static boolean isAtLeast118() { return isAtLeast(18, 0); }

    /** Pack format by Minecraft version */
    public static int getDefaultPackFormat() {
        int major = getMajorVersion();
        int minor = getMinorVersion();
        if (major == 21 && minor >= 4) return 46;
        if (major == 21 && minor >= 2) return 42;
        if (major == 21 && minor >= 1) return 34;
        if (major == 21) return 34;
        if (major == 20 && minor >= 5) return 22;
        if (major == 20 && minor >= 3) return 22;
        if (major == 20 && minor >= 2) return 18;
        if (major == 20) return 15;
        if (major == 19 && minor >= 4) return 13;
        if (major == 19 && minor >= 3) return 12;
        if (major == 19) return 9;
        if (major == 18 && minor >= 2) return 8;
        if (major == 18) return 8;
        if (major == 17) return 7;
        return 6; // 1.16.x
    }

    public static String getFormattedVersion() {
        return "1." + getMajorVersion() + "." + getMinorVersion();
    }
}
