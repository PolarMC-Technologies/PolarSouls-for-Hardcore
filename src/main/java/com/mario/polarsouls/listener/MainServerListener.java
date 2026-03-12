package com.mario.polarsouls.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.ServerTransferUtil;

public class MainServerListener implements Listener {

    private static final String PERM_BYPASS = "PolarSouls.bypass";
    private static final String MSG_SENT_TO_LIMBO = "death-sent-to-limbo";
    private static final String MSG_NOW_SPECTATOR = "death-now-spectator";

    private final PolarSouls plugin;
    private final DatabaseManager db;
    private final Set<UUID> pendingLimbo = ConcurrentHashMap.newKeySet();
    private final Set<UUID> expectedGamemodeChanges = new HashSet<>();
    private final Set<UUID> hybridWindowUsed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> hybridPendingTransfers = new HashMap<>();
    private final Map<UUID, Long> reviveCooldowns = new ConcurrentHashMap<>();

    public MainServerListener(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(PERM_BYPASS)) {
            plugin.debug(player.getName() + " has bypass permission, skipping checks.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handleJoinAsync(player));
    }

    private void handleJoinAsync(Player player) {
        PlayerData data = db.getPlayer(player.getUniqueId());

        if (data == null) {
            handleFirstJoin(player);
            return;
        }

        if (!data.getUsername().equals(player.getName())) {
            data.setUsername(player.getName());
            db.savePlayer(data);
        }

        if (data.isDead()) {
            redirectToLimbo(player);
        } else {
            // gamemode check must happen on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    plugin.debug(player.getName() + " returned alive, restoring to survival.");
                    UUID uuid = player.getUniqueId();
                    grantReviveCooldown(uuid);
                    hybridWindowUsed.remove(uuid);
                    expectedGamemodeChanges.add(uuid);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(MessageUtil.get("revive-success"));
                }
            });
        }
    }

    private void handleFirstJoin(Player player) {
        PlayerData data = PlayerData.createNew(player.getUniqueId(), player.getName(),
                plugin.getDefaultLives());
        db.savePlayer(data);
        plugin.debug("Created new player record for " + player.getName());

        final PlayerData finalData = data;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                String timeRemaining = finalData.getGraceTimeRemaining(plugin.getGracePeriodMillis());
                player.sendMessage(MessageUtil.get("death-grace-period",
                        "time_remaining", timeRemaining));
            }
        });
    }

    private void redirectToLimbo(Player player) {
        String deathMode = plugin.getDeathMode();
        plugin.debug(player.getName() + " is dead (mode: " + deathMode + ")");

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            switch (deathMode) {
                case PolarSouls.MODE_SPECTATOR -> {
                    player.sendMessage(MessageUtil.get(MSG_NOW_SPECTATOR));
                    expectedGamemodeChanges.add(player.getUniqueId());
                    player.setGameMode(GameMode.SPECTATOR);
                }
                case PolarSouls.MODE_HYBRID -> {
                    if (hybridWindowUsed.contains(player.getUniqueId())) {
                        sendDirectToLimbo(player);
                    } else {
                        applyHybridOnJoin(player, player.getUniqueId());
                    }
                }
                default -> sendDirectToLimbo(player);
            }
        });
    }

    private void sendDirectToLimbo(Player player) {
        player.sendMessage(MessageUtil.get(MSG_SENT_TO_LIMBO));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ServerTransferUtil.sendToLimbo(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission(PERM_BYPASS)) return;

        UUID uuid = player.getUniqueId();
        if (pendingLimbo.contains(uuid)) return;

        // Skip if still in post-revive immunity
        Long cooldownExpiry = reviveCooldowns.get(uuid);
        if (cooldownExpiry != null && System.currentTimeMillis() < cooldownExpiry) {
            plugin.debug(player.getName() + " death ignored (revive cooldown active)");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(MessageUtil.get("death-cooldown"));
                }
            });
            return;
        }

        // mark for processing before async DB check
        pendingLimbo.add(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handleDeathAsync(player, uuid));
    }

    private void handleDeathAsync(Player player, UUID uuid) {
        PlayerData data = db.getPlayer(uuid);
        if (data == null) {
            data = PlayerData.createNew(uuid, player.getName(), plugin.getDefaultLives());
        }

        if (data.isInGracePeriod(plugin.getGracePeriodMillis())) {
            pendingLimbo.remove(uuid);
            restoreIfAccidentalSpectator(player, uuid);
            notifyGracePeriod(player, data);
            return;
        }

        int remainingLives = data.decrementLife();
        db.savePlayer(data);
        plugin.debug(player.getName() + " died. Lives remaining: " + remainingLives
                + ", isDead: " + data.isDead());

        if (data.isDead()) {
            // UUID stays in pendingLimbo
            handleFinalDeath(player, uuid);
        } else {
            pendingLimbo.remove(uuid);
            restoreIfAccidentalSpectator(player, uuid);
            notifyLifeLost(player, remainingLives);
        }
    }

    private void restoreIfAccidentalSpectator(Player player, UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
                expectedGamemodeChanges.add(uuid);
                player.setGameMode(GameMode.SURVIVAL);
                cancelHybridTransfer(uuid);
                plugin.debug(player.getName() + " had lives — restored from spectator.");
            }
        });
    }

    private void notifyGracePeriod(Player player, PlayerData data) {
        String timeRemaining = data.getGraceTimeRemaining(plugin.getGracePeriodMillis());
        final String msg = MessageUtil.get("death-grace-period", "time_remaining", timeRemaining);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(msg);
            }
        });
    }

    private void notifyLifeLost(Player player, int remainingLives) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (remainingLives == 1) {
                player.sendMessage(MessageUtil.get("death-last-life"));
            } else {
                player.sendMessage(MessageUtil.get("death-life-lost", "lives", remainingLives));
            }
        });
    }

    private void handleFinalDeath(Player player, UUID uuid) {
        String deathMode = plugin.getDeathMode();

        // send death message only, gamemode change sent to onPlayerRespawn
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                pendingLimbo.remove(uuid);
                return;
            }

            switch (deathMode) {
                case PolarSouls.MODE_SPECTATOR ->
                    player.sendMessage(MessageUtil.get(MSG_NOW_SPECTATOR));
                case PolarSouls.MODE_HYBRID ->
                    player.sendMessage(MessageUtil.get("death-hybrid-warning",
                            "timeout", formatTime(plugin.getHybridTimeoutSeconds())));
                default ->
                    player.sendMessage(MessageUtil.get(MSG_SENT_TO_LIMBO));
            }
        });
    }

    private void applyHybridOnJoin(Player player, UUID uuid) {
        hybridWindowUsed.add(uuid);
        player.sendMessage(MessageUtil.get("death-hybrid-warning",
                "timeout", formatTime(plugin.getHybridTimeoutSeconds())));
        expectedGamemodeChanges.add(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        scheduleHybridTimeout(player, uuid);
    }

    private void scheduleHybridTimeout(Player player, UUID uuid) {
        int timeoutSeconds = plugin.getHybridTimeoutSeconds();
        int delayTicks = timeoutSeconds * 20;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            hybridPendingTransfers.remove(uuid);
            if (player.isOnline()) {
                player.sendMessage(MessageUtil.get(MSG_SENT_TO_LIMBO));
                ServerTransferUtil.sendToLimbo(player);
            }
        }, delayTicks);
        hybridPendingTransfers.put(uuid, task);
    }

    private static String formatTime(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
        if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // only handle players who died their final actual death
        if (!pendingLimbo.remove(uuid)) return;

        String deathMode = plugin.getDeathMode();

        // 1 tick delay so client doesn lag behind
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            switch (deathMode) {
                case PolarSouls.MODE_SPECTATOR -> {
                    expectedGamemodeChanges.add(uuid);
                    player.setGameMode(GameMode.SPECTATOR);
                }
                case PolarSouls.MODE_HYBRID -> {
                    hybridWindowUsed.add(uuid);
                    expectedGamemodeChanges.add(uuid);
                    player.setGameMode(GameMode.SPECTATOR);
                    scheduleHybridTimeout(player, uuid);
                }
                default -> {
                    if (plugin.isSpectatorOnDeath()) {
                        expectedGamemodeChanges.add(uuid);
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ServerTransferUtil.sendToLimbo(player);
                        }
                        expectedGamemodeChanges.remove(uuid);
                    }, plugin.getSendToLimboDelayTicks());
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // detect external SPECTATOR->SURVIVAL change (HRM or other plugin revive)
        String deathMode = plugin.getDeathMode();
        boolean shouldDetect = !PolarSouls.MODE_LIMBO.equals(deathMode) || plugin.isDetectHrmRevive();
        if (!shouldDetect) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (expectedGamemodeChanges.remove(uuid)) return;

        if (player.getGameMode() == GameMode.SPECTATOR
                && event.getNewGameMode() == GameMode.SURVIVAL) {

            plugin.debug("Detected gamemode change SPECTATOR->SURVIVAL for "
                    + player.getName() + " (possible HRM revive)");

            // cancel any pending hybrid transfer
            cancelHybridTransfer(uuid);

            grantReviveCooldown(uuid);
            hybridWindowUsed.remove(uuid);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerData data = db.getPlayer(uuid);
                if (data != null && data.isDead()) {
                    plugin.getLogger().log(Level.INFO,
                            "HRM revive detected for {0}! Updating database.",
                            player.getName());
                    db.revivePlayer(uuid, plugin.getLivesOnRevive());
                }
            });
        }
    }

    private void grantReviveCooldown(UUID uuid) {
        int seconds = plugin.getReviveCooldownSeconds();
        if (seconds > 0) {
            reviveCooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
            plugin.debug("Granted " + seconds + "s revive cooldown to " + uuid);
        }
    }

    private void cancelHybridTransfer(UUID uuid) {
        BukkitTask task = hybridPendingTransfers.remove(uuid);
        if (task != null) {
            task.cancel();
            plugin.debug("Cancelled pending hybrid transfer for " + uuid);
        }
    }
}
