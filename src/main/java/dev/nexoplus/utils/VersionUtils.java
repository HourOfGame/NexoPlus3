package dev.nexoplus.utils;

import org.bukkit.Bukkit;

public class VersionUtils {
    private static String version;

    public static String getServerVersion() {
        if (version == null) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            version = pkg.substring(pkg.lastIndexOf('.') + 1);
        }
        return version;
    }

    public static int getMajorVersion() {
        String v = getServerVersion(); // e.g. v1_20_R1
        String[] parts = v.split("_");
        try { return Integer.parseInt(parts[1]); } catch (Exception e) { return 20; }
    }

    public static int getMinorVersion() {
        String v = getServerVersion();
        String[] parts = v.split("_");
        try { return Integer.parseInt(parts[2].replace("R", "")); } catch (Exception e) { return 1; }
    }
}
