package com.mario.polarsouls.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.model.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private static final String COL_IS_DEAD = "is_dead";
    private static final String SELECT_ALL = "SELECT uuid, username, lives, is_dead, first_join, last_death, last_seen, grace_until FROM ";
    private static final String UPDATE = "UPDATE ";
    private static final int MYSQL_DUPLICATE_COLUMN = 1060;
    
    // Simple cache for death status with TTL to reduce DB queries
    private static final long CACHE_TTL_MS = 2000; // 2 second cache
    private final Map<UUID, CachedDeathStatus> deathStatusCache = new ConcurrentHashMap<>();

    private final PolarSouls plugin;
    private HikariDataSource dataSource;
    private String tableName;
    
    private static class CachedDeathStatus {
        final boolean isDead;
        final long timestamp;
        
        CachedDeathStatus(boolean isDead) {
            this.isDead = isDead;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public DatabaseManager(PolarSouls plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            String host   = plugin.getConfig().getString("database.host", "localhost");
            int port      = plugin.getConfig().getInt("database.port", 3306);
            String dbName = plugin.getConfig().getString("database.name", "minecraft");
            String user   = plugin.getConfig().getString("database.username", "minecraft");
            String pass   = plugin.getConfig().getString("database.password", "changeme");
            int poolSize  = plugin.getConfig().getInt("database.pool-size", 5);
            tableName     = plugin.getConfig().getString("database.table-name", "hardcore_players");

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true"
                    + "&characterEncoding=UTF-8&useUnicode=true";

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(300_000);
            config.setMaxLifetime(600_000);
            config.setPoolName("PolarSouls-Pool");

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "64");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            createTable();

            plugin.getLogger().log(Level.INFO, "MySQL connection established ({0}:{1}/{2})",
                    new Object[]{host, port, dbName});
            return true;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL initialization failed!", e);
            return false;
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL connection pool closed.");
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "username VARCHAR(16) NOT NULL, "
                + "lives INT NOT NULL DEFAULT " + plugin.getDefaultLives() + ", "
                + "is_dead BOOLEAN NOT NULL DEFAULT FALSE, "
                + "first_join BIGINT NOT NULL, "
                + "last_death BIGINT NOT NULL DEFAULT 0, "
                + "last_seen BIGINT NOT NULL DEFAULT 0, "
                + "grace_until BIGINT NOT NULL DEFAULT 0"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
              Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            ensureLastSeenColumn(conn);
            ensureGraceUntilColumn(conn);
            plugin.debug("Table '" + tableName + "' verified/created.");
        }
    }

    private void ensureLastSeenColumn(Connection conn) {
        ensureColumn(conn, "last_seen", "BIGINT NOT NULL DEFAULT 0");
    }

    private void ensureGraceUntilColumn(Connection conn) {
        ensureColumn(conn, "grace_until", "BIGINT NOT NULL DEFAULT 0");
    }

    /**
     * Ensure a column exists in the table. If it already exists, the error is silently ignored.
     *
     * @param conn       Database connection
     * @param columnName Name of the column to add
     * @param definition SQL definition of the column (e.g., "BIGINT NOT NULL DEFAULT 0")
     */
    private void ensureColumn(Connection conn, String columnName, String definition) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            plugin.debug("Added " + columnName + " column to '" + tableName + "'.");
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            boolean duplicateColumn = e.getErrorCode() == MYSQL_DUPLICATE_COLUMN
                    || "42S21".equals(sqlState);
            if (!duplicateColumn) {
                plugin.getLogger().log(Level.WARNING, "Failed to ensure " + columnName + " column", e);
            }
        }
    }

    private PlayerData mapResultSet(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getInt("lives"),
                rs.getBoolean(COL_IS_DEAD),
                rs.getLong("first_join"),
                rs.getLong("last_death"),
                rs.getLong("last_seen"),
                rs.getLong("grace_until")
        );
    }

    public PlayerData getPlayer(UUID uuid) {
        String sql = SELECT_ALL + tableName + " WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to get player " + uuid);
        }
        return null;
    }

    public PlayerData getPlayerByName(String username) {
        String sql = SELECT_ALL + tableName + " WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to get player by name: " + username);
        }
        return null;
    }

    public void savePlayer(PlayerData data) {
        String sql = "INSERT INTO " + tableName
                + " (uuid, username, lives, is_dead, first_join, last_death, last_seen, grace_until) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "username = VALUES(username), "
                + "lives = VALUES(lives), "
                + "is_dead = VALUES(is_dead), "
                + "last_death = VALUES(last_death), "
                + "last_seen = VALUES(last_seen), "
                + "grace_until = VALUES(grace_until)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getUsername());
            ps.setInt(3, data.getLives());
            ps.setBoolean(4, data.isDead());
            ps.setLong(5, data.getFirstJoin());
            ps.setLong(6, data.getLastDeath());
            ps.setLong(7, data.getLastSeen());
            ps.setLong(8, data.getGraceUntil());

            ps.executeUpdate();
            
            // Avoid string concatenation overhead unless debug is enabled
            if (plugin.isDebugMode()) {
                plugin.debug("Saved player data: " + data);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to save player " + data.getUuid());
        } finally {
            // Always invalidate cache after save attempt to ensure consistency
            // This handles both success and failure cases
            deathStatusCache.remove(data.getUuid());
        }
    }

    public boolean isPlayerDead(UUID uuid) {
        // Check cache first
        CachedDeathStatus cached = deathStatusCache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.isDead;
        }
        
        // Cache miss or expired, query database
        String sql = "SELECT is_dead FROM " + tableName + " WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean isDead = rs.getBoolean(COL_IS_DEAD);
                    // Update cache
                    deathStatusCache.put(uuid, new CachedDeathStatus(isDead));
                    return isDead;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to check death status for " + uuid);
        }
        return true;
    }

    public boolean revivePlayer(UUID uuid, int livesToRestore) {
        String sql = UPDATE + tableName
                + " SET is_dead = FALSE, lives = ? WHERE uuid = ? AND is_dead = TRUE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, livesToRestore);
            ps.setString(2, uuid.toString());

            int rows = ps.executeUpdate();
            
            // Invalidate cache on death status change
            if (rows > 0) {
                deathStatusCache.remove(uuid);
            }
            
            // Avoid string concatenation overhead unless debug is enabled
            if (plugin.isDebugMode()) {
                plugin.debug("Revived player " + uuid + " (rows affected: " + rows + ")");
            }
            return rows > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to revive player " + uuid);
            return false;
        }
    }

    public void setLives(UUID uuid, int lives) {
        String sql = UPDATE + tableName + " SET lives = ?, is_dead = ? WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            boolean dead = lives <= 0;
            ps.setInt(1, Math.max(0, lives));
            ps.setBoolean(2, dead);
            ps.setString(3, uuid.toString());

            ps.executeUpdate();
            
            // Invalidate cache on death status change
            deathStatusCache.remove(uuid);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to set lives for " + uuid);
        }
    }

    public void setFirstJoin(UUID uuid, long firstJoin) {
        String sql = UPDATE + tableName + " SET first_join = ? WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, firstJoin);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to set first_join for " + uuid);
        }
    }

    public void setLastSeen(UUID uuid, long lastSeen) {
        String sql = UPDATE + tableName + " SET last_seen = ? WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, lastSeen);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to set last_seen for " + uuid);
        }
    }

    public void setGraceUntil(UUID uuid, long graceUntil) {
        String sql = UPDATE + tableName + " SET grace_until = ? WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, graceUntil);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to set grace_until for " + uuid);
        }
    }

    /**
     * Manually invalidate the death status cache for a player.
     * This should be called if death status changes through non-standard means
     * (e.g., direct database manipulation, external tools, etc.).
     * Note: savePlayer(), revivePlayer(), and setLives() already invalidate the cache.
     */
    public void invalidateDeathStatusCache(UUID uuid) {
        deathStatusCache.remove(uuid);
    }

    public List<PlayerData> getDeadPlayers() {
        String sql = SELECT_ALL + tableName + " WHERE is_dead = TRUE ORDER BY username";

        List<PlayerData> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to get dead players");
        }
        return result;
    }

    // gets plugin version from db, returns null if first time running
    public String getPluginVersion() {
        String metaTable = "polarsouls_meta";
        try (Connection conn = dataSource.getConnection()) {
            // Check if metadata table exists (and create if needed)
            createMetadataTableIfNeeded(conn, metaTable);

            String sql = "SELECT version FROM " + metaTable + " WHERE key_ = 'plugin_version'";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("version");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to get plugin version from database");
        }
        return null;
    }

    public void savePluginVersion(String version) {
        String metaTable = "polarsouls_meta";
        try (Connection conn = dataSource.getConnection()) {
            createMetadataTableIfNeeded(conn, metaTable);

            String sql = "INSERT INTO " + metaTable + " (key_, version) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE version = VALUES(version)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "plugin_version");
                ps.setString(2, version);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to save plugin version to database");
        }
    }

    private void createMetadataTableIfNeeded(Connection conn, String metaTable) throws SQLException {
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + metaTable + " ("
                + "key_ VARCHAR(50) PRIMARY KEY,"
                + "version VARCHAR(50)"
                + ") DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }
}

