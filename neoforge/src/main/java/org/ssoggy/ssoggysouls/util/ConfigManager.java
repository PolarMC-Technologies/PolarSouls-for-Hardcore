package org.ssoggy.ssoggysouls.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

public class ConfigManager {

    private ConfigManager() {
        // Utility class
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig config;
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "ssoggysouls.json");

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                org.ssoggy.ssoggysouls.SSoggySoulsMod.LOGGER.error("Failed to load config file", e);
                config = new ModConfig();
                save();
            }
        } else {
            config = new ModConfig();
            save();
            org.ssoggy.ssoggysouls.SSoggySoulsMod.LOGGER.info("""
                    
                    ===============================================================
                                           SSOGGY SOULS
                    ===============================================================
                     The mod is using SQLite (single-server mode) by default.
                     
                     If you are setting up a Dual-Server Network (Main + Limbo),
                     you MUST stop the server, open ssoggysouls.json, and change the
                     'databaseType' to 'mysql', then fill in your DB details.
                     
                     If you are using a single server, you can ignore this message.
                    ===============================================================
                    """);
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            org.ssoggy.ssoggysouls.SSoggySoulsMod.LOGGER.error("Failed to save config file", e);
        }
    }

    @NotNull
    public static ModConfig getConfig() {
        if (config == null) load();
        return config;
    }

    public static long parseGracePeriod(String input) {
        if (input == null || input.equals("0") || input.isEmpty() || input.length() > 64) {
            return 0;
        }
        long millis = TimeUtil.parseTimeToMillis(input);
        return millis < 0 ? 0 : millis;
    }

    public static class ModConfig {
        // --- Core Lives System ---
        private int defaultLives = 3;
        private int onReviveLives = 1;
        private int maxLives = 5;
        private String gracePeriod = "0"; // e.g., "24h"
        private int reviveCooldownSeconds = 30;

        // --- Database & Proxy (Multiplayer) ---
        private boolean isLimboServer = false;
        private String mainServerName = "main";
        private String limboServerName = "limbo";
        private boolean sendToLimboOnDeath = false;
        private String limboSpawnWorld = "minecraft:overworld";
        private double limboSpawnX = 0;
        private double limboSpawnY = 100;
        private double limboSpawnZ = 0;
        private float limboSpawnYaw = 0;
        private float limboSpawnPitch = 0;

        // --- Security ---
        private boolean limboOpSecurityCheck = true;
        private java.util.List<String> limboTrustedAdmins = new java.util.ArrayList<>();

        // --- Database Connection ---
        private String databaseType = "sqlite"; // "sqlite" or "mysql"
        private String databaseHost = "localhost";
        private int databasePort = 3306;
        private String databaseName = "minecraft";
        private String databaseUsername = "minecraft";
        private String databasePassword = "";
        private String databaseTableName = "hardcore_players";
        private int databasePoolSize = 5;

        // --- HRM (Revival) Features ---
        private boolean hrmEnabled = true;
        private boolean dropHeads = true;
        private boolean headPlaceAsBlock = true;
        private boolean headNoDespawn = true;
        private boolean headFireproof = true;
        private boolean headWearingEffects = true;
        private boolean ritualLightningStrike = true;
        private boolean ritualTotemEffect = true;
        private boolean leaveStructureBase = true;

        // --- DLC / Ghost Mode ---
        private boolean loseInventory = false;
        private boolean ghostModeParticles = false;
        @com.google.gson.annotations.SerializedName("spectatorHeadrestrictRadius")
        private int spectatorHeadRestrictRadius = 16;
        private boolean restrictMenuAccess = true;
        private boolean creativePlayersDropHeads = false;
        private boolean headBurnsInLava = false;
        private int trustedObituaryAfter = 60;
        private int friendsObituaryAfter = 600;
        private int publicObituaryAfter = 3600;
        private int reviveResistanceTicks = 100;
        private int reviveGlowingTicks = 100;

        // --- Messages ---
        private String messagePrefix = "§8[§4☠§8] §r";
        private java.util.Map<String, String> messages = new java.util.HashMap<>();

        // --- Structure Block Tags ---
        @com.google.gson.annotations.SerializedName("soulSandBlocktag")
        private java.util.List<String> soulSandBlockTag = new java.util.ArrayList<>(java.util.Arrays.asList("CRYING_OBSIDIAN", "OBSIDIAN"));
        @com.google.gson.annotations.SerializedName("flowerBlocktag")
        private java.util.List<String> flowerBlockTag = new java.util.ArrayList<>(java.util.Arrays.asList("SOUL_TORCH", "REDSTONE_TORCH"));
        @com.google.gson.annotations.SerializedName("oreBlocktag")
        private java.util.List<String> oreBlockTag = new java.util.ArrayList<>(java.util.Arrays.asList("ENCHANTING_TABLE"));
        @com.google.gson.annotations.SerializedName("fenceBlocktag")
        private java.util.List<String> fenceBlockTag = new java.util.ArrayList<>(java.util.Arrays.asList("OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE", "ACACIA_FENCE", "DARK_OAK_FENCE", "MANGROVE_FENCE", "CHERRY_FENCE", "BAMBOO_FENCE", "CRIMSON_FENCE", "WARPED_FENCE", "NETHER_BRICK_FENCE"));
        @com.google.gson.annotations.SerializedName("stairBlocktag")
        private java.util.List<String> stairBlockTag = new java.util.ArrayList<>(java.util.Arrays.asList("MAGMA_BLOCK"));

        // --- Debug ---
        private boolean debug = false;
        private boolean checkForUpdates = true;

        public ModConfig() {
            // Default messages
            messages.put("death-life-lost", "§cYou lost a life! §7Remaining: §e%lives%");
            messages.put("death-last-life", "§c§l⚠ FINAL WARNING! §cYou are on your last life. Be careful!");
            messages.put("revive-success", "§a§l✦ REVIVED! §aReturning to the world of the living...");
            messages.put("grace-remaining", "§eYou are protected for §a%time_remaining%");
            messages.put("admin-players-only", "§cThis command can only be run by a player.");
            messages.put("status-self", "§7You have §e%lives% §7lives remaining.");
            messages.put("status-not-found", "§cPlayer §e%player% §cnot found.");
            messages.put("status-other-dead", "§e%player% §cis dead.");
            messages.put("status-other-alive", "§e%player% §7has §e%lives% §7lives.");
            messages.put("revive-not-found", "§cPlayer §e%player% §cnot found.");
            messages.put("revive-already-alive", "§e%player% §cis already alive.");
            messages.put("admin-revive-success", "§aSuccessfully revived §e%player%§a.");
            messages.put("revive-from-limbo", "§a%player% §7has been revived and will return from Limbo shortly.");
            messages.put("extra-life-dead", "§cYou cannot use an Extra Life while dead!");
            messages.put("extra-life-at-max", "§cYou are already at the maximum number of lives!");
            messages.put("extra-life-gained", "§aYou gained an extra life! You now have §e%lives% §alives.");
            messages.put("ghost-mode-active", "§7You are a ghost!");
            messages.put("death-sending-to-limbo", "§cYou have died! Sending to Limbo...");
            messages.put("death-now-ghost", "§cYou have died! You are now a ghost.");
            messages.put("limbo-welcome-visitor", "§eWelcome to Limbo as a visitor!");
            messages.put("limbo-welcome-dead", "§cWelcome to Limbo. You are dead!");
            messages.put("limbo-cannot-leave", "§cYou are trapped in Limbo. Only revival can free you.");
            messages.put("revival-structure-incomplete", "§cThe revival structure is incomplete!");
            messages.put("admin-setlives-success", "§aSet §e%player%§a's lives to §e%lives%§a.");
        }

        // Getters
        public int getDefaultLives() { return defaultLives; }
        public int getOnReviveLives() { return onReviveLives; }
        public int getMaxLives() { return maxLives; }
        public String getGracePeriod() { return gracePeriod; }
        public int getReviveCooldownSeconds() { return reviveCooldownSeconds; }
        public boolean isLimboServer() { return !isSingleServerDatabase() && isLimboServer; }
        public String getMainServerName() { return mainServerName; }
        public String getLimboServerName() { return limboServerName; }
        public boolean isSendToLimboOnDeath() { return !isSingleServerDatabase() && sendToLimboOnDeath; }
        public String getLimboSpawnWorld() { return limboSpawnWorld; }
        public double getLimboSpawnX() { return limboSpawnX; }
        public double getLimboSpawnY() { return limboSpawnY; }
        public double getLimboSpawnZ() { return limboSpawnZ; }
        public float getLimboSpawnYaw() { return limboSpawnYaw; }
        public float getLimboSpawnPitch() { return limboSpawnPitch; }
        public boolean isLimboOpSecurityCheck() { return limboOpSecurityCheck; }
        public java.util.List<String> getLimboTrustedAdmins() { return limboTrustedAdmins; }
        public String getDatabaseType() { return databaseType; }
        public String getDatabaseHost() { return databaseHost; }
        public int getDatabasePort() { return databasePort; }
        public String getDatabaseName() { return databaseName; }
        public String getDatabaseUsername() { return databaseUsername; }
        public String getDatabasePassword() { return databasePassword; }
        public String getDatabaseTableName() { return databaseTableName; }
        public int getDatabasePoolSize() { return databasePoolSize; }
        public boolean isHrmEnabled() { return hrmEnabled; }
        public boolean isDropHeads() { return dropHeads; }
        public boolean isHeadPlaceAsBlock() { return headPlaceAsBlock; }
        public boolean isHeadNoDespawn() { return headNoDespawn; }
        public boolean isHeadFireproof() { return headFireproof; }
        public boolean isHeadWearingEffects() { return headWearingEffects; }
        public boolean isRitualLightningStrike() { return ritualLightningStrike; }
        public boolean isRitualTotemEffect() { return ritualTotemEffect; }
        public boolean isLeaveStructureBase() { return leaveStructureBase; }
        public boolean isLoseInventory() { return loseInventory; }
        public boolean isGhostModeParticles() { return ghostModeParticles; }
        public int getSpectatorHeadRestrictRadius() { return spectatorHeadRestrictRadius; }
        /**
         * @deprecated Use {@link #getSpectatorHeadRestrictRadius()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public int getSpectatorHeadrestrictRadius() { return getSpectatorHeadRestrictRadius(); }
        public boolean isRestrictMenuAccess() { return restrictMenuAccess; }
        public boolean isCreativePlayersDropHeads() { return creativePlayersDropHeads; }
        public boolean isHeadBurnsInLava() { return headBurnsInLava; }
        public int getTrustedObituaryAfter() { return trustedObituaryAfter; }
        public int getFriendsObituaryAfter() { return friendsObituaryAfter; }
        public int getPublicObituaryAfter() { return publicObituaryAfter; }
        public int getReviveResistanceTicks() { return reviveResistanceTicks; }
        public int getReviveGlowingTicks() { return reviveGlowingTicks; }
        public String getMessagePrefix() { return messagePrefix; }
        public java.util.Map<String, String> getMessages() { return messages; }
        public java.util.List<String> getSoulSandBlockTag() { return soulSandBlockTag; }
        public java.util.List<String> getFlowerBlockTag() { return flowerBlockTag; }
        public java.util.List<String> getOreBlockTag() { return oreBlockTag; }
        public java.util.List<String> getFenceBlockTag() { return fenceBlockTag; }
        public java.util.List<String> getStairBlockTag() { return stairBlockTag; }
        /**
         * @deprecated Use {@link #getSoulSandBlockTag()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public java.util.List<String> getSoulSandBlocktag() { return getSoulSandBlockTag(); }

        /**
         * @deprecated Use {@link #getFlowerBlockTag()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public java.util.List<String> getFlowerBlocktag() { return getFlowerBlockTag(); }

        /**
         * @deprecated Use {@link #getOreBlockTag()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public java.util.List<String> getOreBlocktag() { return getOreBlockTag(); }

        /**
         * @deprecated Use {@link #getFenceBlockTag()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public java.util.List<String> getFenceBlocktag() { return getFenceBlockTag(); }

        /**
         * @deprecated Use {@link #getStairBlockTag()} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public java.util.List<String> getStairBlocktag() { return getStairBlockTag(); }
        public boolean isDebug() { return debug; }
        public boolean isCheckForUpdates() { return checkForUpdates; }

        public boolean isSingleServerDatabase() {
            return isSqliteDatabaseType(databaseType);
        }

        private static boolean isSqliteDatabaseType(String type) {
            if (type == null) {
                return true;
            }
            String normalized = type.trim();
            return normalized.isEmpty()
                    || "sqlite".equalsIgnoreCase(normalized)
                    || "local".equalsIgnoreCase(normalized);
        }

        public String getConfigString(String path, String defaultValue) {
            return switch (path) {
                case "database.host" -> databaseHost;
                case "database.name" -> databaseName;
                case "database.username" -> databaseUsername;
                case "database.password" -> databasePassword;
                case "database.table-name" -> databaseTableName;
                case "database.type" -> databaseType;
                default -> defaultValue;
            };
        }

        public int getConfigInt(String path, int defaultValue) {
            return switch (path) {
                case "database.port" -> databasePort;
                case "database.pool-size" -> databasePoolSize;
                default -> defaultValue;
            };
        }

        // Setters for ModMenu / Initialization
        public void setLimboServer(boolean limboServer) { isLimboServer = limboServer; }
        public void setSendToLimboOnDeath(boolean send) { sendToLimboOnDeath = send; }
        public void setDefaultLives(int lives) { defaultLives = lives; }
        public void setOnReviveLives(int lives) { onReviveLives = lives; }
        public void setMaxLives(int max) { maxLives = max; }
        public void setGracePeriod(String period) { gracePeriod = period; }
        public void setReviveCooldownSeconds(int seconds) { reviveCooldownSeconds = seconds; }
        public void setMainServerName(String name) { mainServerName = name; }
        public void setLimboServerName(String name) { limboServerName = name; }
        public void setLimboSpawnWorld(String world) { limboSpawnWorld = world; }
        public void setLimboSpawnX(double x) { limboSpawnX = x; }
        public void setLimboSpawnY(double y) { limboSpawnY = y; }
        public void setLimboSpawnZ(double z) { limboSpawnZ = z; }
        public void setLimboSpawnYaw(float yaw) { limboSpawnYaw = yaw; }
        public void setLimboSpawnPitch(float pitch) { limboSpawnPitch = pitch; }
        public void setLimboOpSecurityCheck(boolean check) { limboOpSecurityCheck = check; }
        public void setLimboTrustedAdmins(java.util.Collection<String> admins) { limboTrustedAdmins = normalizeStringList(admins); }
        public void setDatabaseType(String type) { databaseType = type; }
        public void setDatabaseHost(String host) { databaseHost = host; }
        public void setDatabasePort(int port) { databasePort = port; }
        public void setDatabaseName(String name) { databaseName = name; }
        public void setDatabaseUsername(String username) { databaseUsername = username; }
        public void setDatabasePassword(String password) { databasePassword = password; }
        public void setDatabaseTableName(String name) { databaseTableName = name; }
        public void setDatabasePoolSize(int size) { databasePoolSize = size; }
        public void setHrmEnabled(boolean enabled) { hrmEnabled = enabled; }
        public void setDropHeads(boolean drop) { dropHeads = drop; }
        public void setHeadPlaceAsBlock(boolean place) { headPlaceAsBlock = place; }
        public void setHeadNoDespawn(boolean noDespawn) { headNoDespawn = noDespawn; }
        public void setHeadFireproof(boolean fireproof) { headFireproof = fireproof; }
        public void setHeadWearingEffects(boolean effects) { headWearingEffects = effects; }
        public void setRitualLightningStrike(boolean strike) { ritualLightningStrike = strike; }
        public void setRitualTotemEffect(boolean effect) { ritualTotemEffect = effect; }
        public void setLeaveStructureBase(boolean leave) { leaveStructureBase = leave; }
        public void setLoseInventory(boolean lose) { loseInventory = lose; }
        public void setGhostModeParticles(boolean particles) { ghostModeParticles = particles; }
        public void setSpectatorHeadRestrictRadius(int radius) { spectatorHeadRestrictRadius = radius; }
        /**
         * @deprecated Use {@link #setSpectatorHeadRestrictRadius(int)} instead.
         */
        @Deprecated(since = "4.4.8")
        @SuppressWarnings({"java:S1133", "java:S1201", "java:S1845"})
        public void setSpectatorHeadrestrictRadius(int radius) { setSpectatorHeadRestrictRadius(radius); }
        public void setRestrictMenuAccess(boolean restrict) { restrictMenuAccess = restrict; }
        public void setCreativePlayersDropHeads(boolean drop) { creativePlayersDropHeads = drop; }
        public void setHeadBurnsInLava(boolean burns) { headBurnsInLava = burns; }
        public void setTrustedObituaryAfter(int seconds) { trustedObituaryAfter = seconds; }
        public void setFriendsObituaryAfter(int seconds) { friendsObituaryAfter = seconds; }
        public void setPublicObituaryAfter(int seconds) { publicObituaryAfter = seconds; }
        public void setReviveResistanceTicks(int ticks) { reviveResistanceTicks = ticks; }
        public void setReviveGlowingTicks(int ticks) { reviveGlowingTicks = ticks; }
        public void setSoulSandBlocktag(java.util.Collection<String> blocks) { soulSandBlockTag = normalizeBlockList(blocks); }
        public void setFlowerBlocktag(java.util.Collection<String> blocks) { flowerBlockTag = normalizeBlockList(blocks); }
        public void setOreBlocktag(java.util.Collection<String> blocks) { oreBlockTag = normalizeBlockList(blocks); }
        public void setFenceBlocktag(java.util.Collection<String> blocks) { fenceBlockTag = normalizeBlockList(blocks); }
        public void setStairBlocktag(java.util.Collection<String> blocks) { stairBlockTag = normalizeBlockList(blocks); }
        public void setDebug(boolean d) { debug = d; }
        public void setCheckForUpdates(boolean check) { checkForUpdates = check; }

        private static java.util.List<String> normalizeBlockList(java.util.Collection<String> blocks) {
            java.util.List<String> normalized = new java.util.ArrayList<>();
            for (String block : blocks) {
                if (block != null && !block.isBlank()) {
                    normalized.add(block.trim().toUpperCase(java.util.Locale.ROOT));
                }
            }
            return normalized;
        }

        private static java.util.List<String> normalizeStringList(java.util.Collection<String> items) {
            java.util.List<String> normalized = new java.util.ArrayList<>();
            for (String item : items) {
                if (item != null && !item.isBlank()) {
                    normalized.add(item.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
            return normalized;
        }
    }
}
