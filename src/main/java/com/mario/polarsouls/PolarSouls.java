package com.mario.polarsouls;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.mario.polarsouls.command.AdminCommand;
import com.mario.polarsouls.command.LeaveLimboCommand;
import com.mario.polarsouls.command.ReviveCommand;
import com.mario.polarsouls.command.SetLimboSpawnCommand;
import com.mario.polarsouls.command.SetLivesCommand;
import com.mario.polarsouls.command.StatusCommand;
import com.mario.polarsouls.command.VisitLimboCommand;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.hrm.ExtraLifeManager;
import com.mario.polarsouls.hrm.HeadDropListener;
import com.mario.polarsouls.hrm.HeadEffectsTask;
import com.mario.polarsouls.hrm.RevivalStructureListener;
import com.mario.polarsouls.hrm.ReviveSkullManager;
import com.mario.polarsouls.listener.LimboServerListener;
import com.mario.polarsouls.listener.MainServerListener;
import com.mario.polarsouls.task.LimboCheckTask;
import com.mario.polarsouls.task.MainReviveCheckTask;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.TimeUtil;
import com.mario.polarsouls.util.UpdateChecker;

@SuppressWarnings("java:S6548")
public final class PolarSouls extends JavaPlugin {

    private static PolarSouls instance;

    private DatabaseManager databaseManager;
    private boolean isLimboServer;
    private boolean debugMode;

    private String mainServerName;
    private String limboServerName;

    private static final String DEFAULT_WORLD = "world";
    private static final String CFG_SPAWN_X = "limbo.spawn.x";
    private static final String CFG_SPAWN_Y = "limbo.spawn.y";
    private static final String CFG_SPAWN_Z = "limbo.spawn.z";
    private static final String CFG_SPAWN_YAW = "limbo.spawn.yaw";
    private static final String CFG_SPAWN_PITCH = "limbo.spawn.pitch";
    public static final String MODE_LIMBO = "limbo";
    public static final String MODE_SPECTATOR = "spectator";
    public static final String MODE_HYBRID = "hybrid";
    private static final String BORDER_EMPTY = "║                                                           ║";
    private static final String BORDER_TOP = "╔═══════════════════════════════════════════════════════════╗";
    private static final String BORDER_BOTTOM = "╚═══════════════════════════════════════════════════════════╝";

    private int defaultLives;
    private long gracePeriodMillis;
    private int livesOnRevive;
    private int maxLives;

    private int sendToLimboDelayTicks;
    private boolean spectatorOnDeath;
    private boolean detectHrmRevive;
    private String deathMode;
    private int hybridTimeoutSeconds;
    private int reviveCooldownSeconds;
    private boolean extraLifeEnabled;

    private boolean hrmEnabled;
    private boolean hrmDropHeads;
    private boolean hrmDeathLocationMsg;
    private boolean hrmStructureRevive;
    private boolean hrmLeaveStructureBase;
    private boolean hrmHeadEffects;
    private boolean hrmReviveSkullRecipe;
    private ReviveSkullManager reviveSkullManager;
    private ExtraLifeManager extraLifeManager;

    private Location limboSpawn;
    private final Set<UUID> limboDeadPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        setInstance(this);
        saveDefaultConfig();
        loadConfigValues();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to connect to MySQL! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();

        if (isLimboServer) {
            enableLimboMode();
        } else {
            enableMainMode();
        }

        String mode = isLimboServer ? "LIMBO SERVER" : "MAIN SERVER";
        String version = getDescription().getVersion();
        
        // you see this ascii art is alot better than the updatechecker one am i right
        getLogger().info("");
        getLogger().info(BORDER_TOP);
        getLogger().info(BORDER_EMPTY);
        getLogger().info("║     ██████╗  ██████╗ ██╗      █████╗ ██████╗            ║");
        getLogger().info("║     ██╔══██╗██╔═══██╗██║     ██╔══██╗██╔══██╗           ║");
        getLogger().info("║     ██████╔╝██║   ██║██║     ███████║██████╔╝           ║");
        getLogger().info("║     ██╔═══╝ ██║   ██║██║     ██╔══██║██╔══██╗           ║");
        getLogger().info("║     ██║     ╚██████╔╝███████╗██║  ██║██║  ██║           ║");
        getLogger().info("║     ╚═╝      ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝           ║");
        getLogger().info(BORDER_EMPTY);
        getLogger().info("║      ███████╗ ██████╗ ██╗   ██╗██╗     ███████╗         ║");
        getLogger().info("║      ██╔════╝██╔═══██╗██║   ██║██║     ██╔════╝         ║");
        getLogger().info("║      ███████╗██║   ██║██║   ██║██║     ███████╗         ║");
        getLogger().info("║      ╚════██║██║   ██║██║   ██║██║     ╚════██║         ║");
        getLogger().info("║      ███████║╚██████╔╝╚██████╔╝███████╗███████║         ║");
        getLogger().info("║      ╚══════╝ ╚═════╝  ╚═════╝ ╚══════╝╚══════╝         ║");
        getLogger().info(BORDER_EMPTY);
        getLogger().log(Level.INFO, "║   Version: {0}║", String.format("%-47s", version));
        getLogger().log(Level.INFO, "║   Mode:    {0}║", String.format("%-47s", mode));
        getLogger().info(BORDER_EMPTY);
        getLogger().info("║   ☠ Hardcore Lives • Limbo Exile • Revive System ☠      ║");
        getLogger().info(BORDER_EMPTY);
        getLogger().info(BORDER_BOTTOM);
        getLogger().info("");

        if (getConfig().getBoolean("check-for-updates", true)) {
            new UpdateChecker(this).checkForUpdates();
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        if (reviveSkullManager != null) {
            reviveSkullManager.unregisterRecipe();
        }
        if (extraLifeManager != null) {
            extraLifeManager.unregisterRecipe();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("PolarSouls disabled.");
        setInstance(null);
    }

    private static void setInstance(PolarSouls value) {
        instance = value;
    }

    private void enableMainMode() {
        getLogger().info("Registering MAIN server listeners...");
        getServer().getPluginManager().registerEvents(
                new MainServerListener(this), this);

        int intervalSeconds = getConfig().getInt("limbo.check-interval-seconds", 3);
        int intervalTicks = intervalSeconds * 20;
        new MainReviveCheckTask(this).runTaskTimerAsynchronously(this, 60L, intervalTicks);
        getLogger().log(Level.INFO, "Main revive check task started (every {0}s).", intervalSeconds);

        if (hrmEnabled) {
            getServer().getPluginManager().registerEvents(
                    new HeadDropListener(this), this);
            getServer().getPluginManager().registerEvents(
                    new RevivalStructureListener(this), this);

            if (hrmHeadEffects) {
                new HeadEffectsTask(this).runTaskTimer(this, 20L, 20L);
                getLogger().info("HRM head-wearing effects task started.");
            }

            if (hrmReviveSkullRecipe) {
                reviveSkullManager = new ReviveSkullManager(this);
                reviveSkullManager.registerRecipe();
                getServer().getPluginManager().registerEvents(reviveSkullManager, this);
                getLogger().info("HRM revive skull recipe and menu registered.");
            }

            getLogger().info("Built-in HRM features enabled.");
        }

        if (extraLifeEnabled) {
            extraLifeManager = new ExtraLifeManager(this);
            extraLifeManager.registerRecipe();
            getServer().getPluginManager().registerEvents(extraLifeManager, this);
            getLogger().info("Extra Life recipe and listener registered.");
        }
    }

    private void enableLimboMode() {
        getLogger().info("Registering LIMBO server listeners and tasks...");
        getServer().getPluginManager().registerEvents(
                new LimboServerListener(this), this);

        int intervalSeconds = getConfig().getInt("limbo.check-interval-seconds", 3);
        int intervalTicks = intervalSeconds * 20;
        new LimboCheckTask(this).runTaskTimerAsynchronously(this, 60L, intervalTicks);
        getLogger().log(Level.INFO, "Limbo check task started (every {0}s).", intervalSeconds);
    }

    private void registerCommands() {
        ReviveCommand reviveCmd = new ReviveCommand(this);
        PluginCommand revive = Objects.requireNonNull(getCommand("revive"));
        revive.setExecutor(reviveCmd);
        revive.setTabCompleter(reviveCmd);

        StatusCommand statusCmd = new StatusCommand(this);
        PluginCommand hlstatus = Objects.requireNonNull(getCommand("pstatus"));
        hlstatus.setExecutor(statusCmd);
        hlstatus.setTabCompleter(statusCmd);

        SetLivesCommand setLivesCmd = new SetLivesCommand(this);
        PluginCommand hlsetlives = Objects.requireNonNull(getCommand("psetlives"));
        hlsetlives.setExecutor(setLivesCmd);
        hlsetlives.setTabCompleter(setLivesCmd);

        AdminCommand adminCmd = new AdminCommand(this);
        PluginCommand hladmin = Objects.requireNonNull(getCommand("psadmin"));
        hladmin.setExecutor(adminCmd);
        hladmin.setTabCompleter(adminCmd);

        if (isLimboServer) {
            PluginCommand setSpawn = Objects.requireNonNull(getCommand("setlimbospawn"));
            setSpawn.setExecutor(new SetLimboSpawnCommand(this));

            PluginCommand leaveLimbo = Objects.requireNonNull(getCommand("leavelimbo"));
            leaveLimbo.setExecutor(new LeaveLimboCommand(this));
        } else {
            PluginCommand visitLimbo = Objects.requireNonNull(getCommand(MODE_LIMBO));
            visitLimbo.setExecutor(new VisitLimboCommand(this));
        }
    }

    public void loadConfigValues() {
        reloadConfig();
        FileConfiguration cfg = getConfig();

        isLimboServer       = cfg.getBoolean("is-limbo-server", false);
        debugMode           = cfg.getBoolean("debug", false);
        mainServerName      = cfg.getString("main-server-name", "main");
        limboServerName     = cfg.getString("limbo-server-name", MODE_LIMBO);

        defaultLives        = cfg.getInt("lives.default", 2);
        gracePeriodMillis   = loadGracePeriod(cfg);
        livesOnRevive       = cfg.getInt("lives.on-revive", 1);
        maxLives            = cfg.getInt("lives.max-lives", 5);

        sendToLimboDelayTicks = cfg.getInt("main.send-to-limbo-delay-ticks", 60);
        spectatorOnDeath    = cfg.getBoolean("main.spectator-on-death", true);
        detectHrmRevive     = cfg.getBoolean("main.detect-hrm-revive", true);
        deathMode           = cfg.getString("main.death-mode", MODE_HYBRID);
        hybridTimeoutSeconds = cfg.getInt("main.hybrid-timeout-seconds", 300);
        reviveCooldownSeconds = cfg.getInt("lives.revive-cooldown-seconds", 30);
        extraLifeEnabled    = cfg.getBoolean("extra-life.enabled", true);

        hrmEnabled            = cfg.getBoolean("hrm.enabled", true);
        hrmDropHeads          = cfg.getBoolean("hrm.drop-heads", true);
        hrmDeathLocationMsg   = cfg.getBoolean("hrm.death-location-message", true);
        hrmStructureRevive    = cfg.getBoolean("hrm.structure-revive", true);
        hrmLeaveStructureBase = cfg.getBoolean("hrm.leave-structure-base", true);
        hrmHeadEffects        = cfg.getBoolean("hrm.head-wearing-effects", true);
        hrmReviveSkullRecipe  = cfg.getBoolean("hrm.revive-skull-recipe", true);

        if (isLimboServer) {
            loadLimboSpawn();
        }

        MessageUtil.loadMessages(cfg);
    }

    private long loadGracePeriod(FileConfiguration cfg) {
        // Try new string format first (e.g., "1h30m")
        String gracePeriodStr = cfg.getString("lives.grace-period");
        if (gracePeriodStr != null) {
            long millis = TimeUtil.parseTimeToMillis(gracePeriodStr);
            if (millis >= 0) {
                return millis;
            }
            getLogger().warning("Invalid grace-period format: " + gracePeriodStr + ". Using default of 24h.");
        }

        // Fall back to old format (hours as integer) for backward compatibility
        int hours = cfg.getInt("lives.grace-period-hours", -1);
        if (hours >= 0) {
            return hours * 3600_000L;
        }

        // Default to 24 hours
        return 24 * 3600_000L;
    }

    private void loadLimboSpawn() {
        FileConfiguration cfg = getConfig();
        String worldName = cfg.getString("limbo.spawn.world", DEFAULT_WORLD);

        if (worldName == null) {
            worldName = DEFAULT_WORLD;
        }

        final String resolvedWorldName = worldName;
        World world = Bukkit.getWorld(resolvedWorldName);

        if (world == null) { // world not loaded yet, defer
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

    public void saveLimboSpawn(Location loc) {
        this.limboSpawn = loc;
        FileConfiguration cfg = getConfig();
        World world = loc.getWorld();
        cfg.set("limbo.spawn.world", world != null ? world.getName() : DEFAULT_WORLD);
        cfg.set(CFG_SPAWN_X, loc.getX());
        cfg.set(CFG_SPAWN_Y, loc.getY());
        cfg.set(CFG_SPAWN_Z, loc.getZ());
        cfg.set(CFG_SPAWN_YAW, (double) loc.getYaw());
        cfg.set(CFG_SPAWN_PITCH, (double) loc.getPitch());
        saveConfig();
    }

    public void debug(String message) {
        if (debugMode && getLogger().isLoggable(Level.INFO)) {
            getLogger().log(Level.INFO, "[DEBUG] {0}", message);
        }
    }

    public static PolarSouls getInstance() {
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

    public long getGracePeriodMillis() {
        return gracePeriodMillis;
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

    public String getDeathMode() {
        return deathMode;
    }

    public int getHybridTimeoutSeconds() {
        return hybridTimeoutSeconds;
    }

    public int getReviveCooldownSeconds() {
        return reviveCooldownSeconds;
    }

    public Location getLimboSpawn() {
        return limboSpawn;
    }

    public Set<UUID> getLimboDeadPlayers() {
        return limboDeadPlayers;
    }

    public boolean isHrmEnabled() {
        return hrmEnabled;
    }

    public boolean isHrmDropHeads() {
        return hrmEnabled && hrmDropHeads;
    }

    public boolean isHrmDeathLocationMsg() {
        return hrmEnabled && hrmDeathLocationMsg;
    }

    public boolean isHrmStructureRevive() {
        return hrmEnabled && hrmStructureRevive;
    }

    public boolean isHrmLeaveStructureBase() {
        return hrmLeaveStructureBase;
    }

    public boolean isHrmHeadEffects() {
        return hrmEnabled && hrmHeadEffects;
    }

    public boolean isHrmReviveSkullRecipe() {
        return hrmEnabled && hrmReviveSkullRecipe;
    }
}
