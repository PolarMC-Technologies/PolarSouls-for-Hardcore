package com.mario.hardcorelimbo;

import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.mario.hardcorelimbo.command.ReviveCommand;
import com.mario.hardcorelimbo.command.SetLimboSpawnCommand;
import com.mario.hardcorelimbo.command.SetLivesCommand;
import com.mario.hardcorelimbo.command.StatusCommand;
import com.mario.hardcorelimbo.database.DatabaseManager;
import com.mario.hardcorelimbo.listener.LimboServerListener;
import com.mario.hardcorelimbo.listener.MainServerListener;
import com.mario.hardcorelimbo.task.LimboCheckTask;
import com.mario.hardcorelimbo.util.MessageUtil;

/**
 * HardcoreLimbo - Custom Hardcore system for Velocity proxy networks.
 *
 * This plugin runs on BOTH the Main and Limbo servers, switching
 * behaviour based on the 'is-limbo-server' config flag.
 *
 * Main server: handles death, life tracking, and HRM integration.
 * Limbo server: traps dead players and auto-releases them on revive.
 */
public final class HardcoreLimbo extends JavaPlugin {

    private static HardcoreLimbo instance;

    private DatabaseManager databaseManager;
    private boolean isLimboServer;
    private boolean debugMode;

    // Server names for BungeeCord messaging
    private String mainServerName;
    private String limboServerName;

    // Config key constants to avoid duplication
    private static final String CFG_SPAWN_X = "limbo.spawn.x";
    private static final String CFG_SPAWN_Y = "limbo.spawn.y";
    private static final String CFG_SPAWN_Z = "limbo.spawn.z";
    private static final String CFG_SPAWN_YAW = "limbo.spawn.yaw";
    private static final String CFG_SPAWN_PITCH = "limbo.spawn.pitch";

    // Lives config
    private int defaultLives;
    private int gracePeriodHours;
    private int livesOnRevive;
    private int maxLives;

    // Main-server config
    private int sendToLimboDelayTicks;
    private boolean spectatorOnDeath;
    private boolean detectHrmRevive;

    // Limbo spawn location (only used on Limbo server)
    private Location limboSpawn;

    // -------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------

    @Override
    public void onEnable() {
        setInstance(this);

        // Save default config if it doesn't exist, then load
        saveDefaultConfig();
        loadConfigValues();

        // Register BungeeCord plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Initialize database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to connect to MySQL! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands (available on both servers)
        registerCommands();

        // Register listeners and tasks based on server mode
        if (isLimboServer) {
            enableLimboMode();
        } else {
            enableMainMode();
        }

        getLogger().info("======================================");
        getLogger().log(Level.INFO, "  HardcoreLimbo v{0} enabled!", getDescription().getVersion());
        getLogger().log(Level.INFO, "  Mode: {0}", isLimboServer ? "LIMBO SERVER" : "MAIN SERVER");
        getLogger().info("======================================");
    }

    @Override
    public void onDisable() {
        // Unregister messaging channel
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("HardcoreLimbo disabled.");
        setInstance(null);
    }

    private static void setInstance(HardcoreLimbo value) {
        instance = value;
    }

    // -------------------------------------------------------
    // Server-mode setup
    // -------------------------------------------------------

    private void enableMainMode() {
        getLogger().info("Registering MAIN server listeners...");
        getServer().getPluginManager().registerEvents(
                new MainServerListener(this), this);
    }

    private void enableLimboMode() {
        getLogger().info("Registering LIMBO server listeners and tasks...");
        getServer().getPluginManager().registerEvents(
                new LimboServerListener(this), this);

        // Start the periodic revive-check task
        int intervalSeconds = getConfig().getInt("limbo.check-interval-seconds", 3);
        int intervalTicks = intervalSeconds * 20;
        new LimboCheckTask(this).runTaskTimerAsynchronously(this, 60L, intervalTicks);
        getLogger().log(Level.INFO, "Limbo check task started (every {0}s).", intervalSeconds);
    }

    // -------------------------------------------------------
    // Commands
    // -------------------------------------------------------

    private void registerCommands() {
        ReviveCommand reviveCmd = new ReviveCommand(this);
        PluginCommand revive = Objects.requireNonNull(getCommand("revive"));
        revive.setExecutor(reviveCmd);
        revive.setTabCompleter(reviveCmd);

        StatusCommand statusCmd = new StatusCommand(this);
        PluginCommand hlstatus = Objects.requireNonNull(getCommand("hlstatus"));
        hlstatus.setExecutor(statusCmd);
        hlstatus.setTabCompleter(statusCmd);

        SetLivesCommand setLivesCmd = new SetLivesCommand(this);
        PluginCommand hlsetlives = Objects.requireNonNull(getCommand("hlsetlives"));
        hlsetlives.setExecutor(setLivesCmd);
        hlsetlives.setTabCompleter(setLivesCmd);

        // Only register /setlimbospawn on the Limbo server
        if (isLimboServer) {
            PluginCommand setSpawn = Objects.requireNonNull(getCommand("setlimbospawn"));
            setSpawn.setExecutor(new SetLimboSpawnCommand(this));
        }
    }

    // -------------------------------------------------------
    // Config loading
    // -------------------------------------------------------

    public void loadConfigValues() {
        reloadConfig();
        FileConfiguration cfg = getConfig();

        isLimboServer       = cfg.getBoolean("is-limbo-server", false);
        debugMode           = cfg.getBoolean("debug", false);
        mainServerName      = cfg.getString("main-server-name", "main");
        limboServerName     = cfg.getString("limbo-server-name", "limbo");

        defaultLives        = cfg.getInt("lives.default", 2);
        gracePeriodHours    = cfg.getInt("lives.grace-period-hours", 24);
        livesOnRevive       = cfg.getInt("lives.on-revive", 1);
        maxLives            = cfg.getInt("lives.max-lives", 5);

        sendToLimboDelayTicks = cfg.getInt("main.send-to-limbo-delay-ticks", 60);
        spectatorOnDeath    = cfg.getBoolean("main.spectator-on-death", true);
        detectHrmRevive     = cfg.getBoolean("main.detect-hrm-revive", true);

        // Load limbo spawn location
        if (isLimboServer) {
            loadLimboSpawn();
        }

        // Load messages into MessageUtil
        MessageUtil.loadMessages(cfg);
    }

    private void loadLimboSpawn() {
        FileConfiguration cfg = getConfig();
        String worldName = cfg.getString("limbo.spawn.world", "world");

        if (worldName == null) {
            worldName = "world";
        }

        final String resolvedWorldName = worldName;
        World world = Bukkit.getWorld(resolvedWorldName);

        // World may not be loaded yet during onEnable; schedule for later
        if (world == null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                World w = Bukkit.getWorld(resolvedWorldName);
                if (w != null) {
                    limboSpawn = buildSpawnLocation(w, cfg);
                    debug("Limbo spawn loaded: " + limboSpawn);
                } else {
                    getLogger().log(Level.WARNING, "Limbo spawn world ''{0}'' not found!", resolvedWorldName);
                }
            }, 20L);
        } else {
            limboSpawn = buildSpawnLocation(world, cfg);
        }
    }

    private Location buildSpawnLocation(World world, FileConfiguration cfg) {
        return new Location(world,
                cfg.getDouble(CFG_SPAWN_X, 0.5),
                cfg.getDouble(CFG_SPAWN_Y, 65.0),
                cfg.getDouble(CFG_SPAWN_Z, 0.5),
                (float) cfg.getDouble(CFG_SPAWN_YAW, 0.0),
                (float) cfg.getDouble(CFG_SPAWN_PITCH, 0.0));
    }

    /**
     * Saves the limbo spawn location to config.yml
     */
    public void saveLimboSpawn(Location loc) {
        this.limboSpawn = loc;
        FileConfiguration cfg = getConfig();
        World world = loc.getWorld();
        cfg.set("limbo.spawn.world", world != null ? world.getName() : "world");
        cfg.set(CFG_SPAWN_X, loc.getX());
        cfg.set(CFG_SPAWN_Y, loc.getY());
        cfg.set(CFG_SPAWN_Z, loc.getZ());
        cfg.set(CFG_SPAWN_YAW, (double) loc.getYaw());
        cfg.set(CFG_SPAWN_PITCH, (double) loc.getPitch());
        saveConfig();
    }

    // -------------------------------------------------------
    // Debug helper
    // -------------------------------------------------------

    public void debug(String message) {
        if (debugMode && getLogger().isLoggable(Level.INFO)) {
            getLogger().log(Level.INFO, "[DEBUG] {0}", message);
        }
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public static HardcoreLimbo getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public boolean isLimboServer() {
        return isLimboServer;
    }

    public String getMainServerName() {
        return mainServerName;
    }

    public String getLimboServerName() {
        return limboServerName;
    }

    public int getDefaultLives() {
        return defaultLives;
    }

    public int getGracePeriodHours() {
        return gracePeriodHours;
    }

    public int getLivesOnRevive() {
        return livesOnRevive;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public int getSendToLimboDelayTicks() {
        return sendToLimboDelayTicks;
    }

    public boolean isSpectatorOnDeath() {
        return spectatorOnDeath;
    }

    public boolean isDetectHrmRevive() {
        return detectHrmRevive;
    }

    public Location getLimboSpawn() {
        return limboSpawn;
    }
}
