package dev.nexoplus.managers;
import dev.nexoplus.core.NexoPlus;
import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final NexoPlus plugin;
    private Connection connection;

    public DatabaseManager(NexoPlus p) { this.plugin = p; }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("Database initialized (SQLite).");
        } catch (Exception e) {
            plugin.getLogger().warning("Database init failed: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS nexoplus_player_data (" +
                    "uuid TEXT PRIMARY KEY, data TEXT, updated_at INTEGER)");
            s.execute("CREATE TABLE IF NOT EXISTS nexoplus_block_data (" +
                    "world TEXT, x INTEGER, y INTEGER, z INTEGER, block_id TEXT, " +
                    "PRIMARY KEY (world, x, y, z))");
        }
    }

    public Connection getConnection() { return connection; }

    public void shutdown() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (Exception ignored) {}
    }
}
