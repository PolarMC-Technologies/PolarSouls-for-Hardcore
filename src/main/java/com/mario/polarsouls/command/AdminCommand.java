package com.mario.polarsouls.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.CommandUtil;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.PermissionUtil;
import com.mario.polarsouls.util.PlayerRevivalUtil;
import com.mario.polarsouls.util.ServerTransferUtil;
import com.mario.polarsouls.util.TabCompleteUtil;
import com.mario.polarsouls.util.TimeUtil;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String KEY_PLAYER = "player";
    private static final String KEY_LIVES = "lives";
    private static final String KEY_ACTION = "action";
    private static final String SUB_LIVES = "lives";
    private static final String SUB_GRACE = "grace";
    private static final String SUB_REVIVE = "revive";
    private static final String ERR_NUMBER = "&cInvalid number: ";

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            SUB_LIVES, SUB_GRACE, "kill", SUB_REVIVE, "reset", "info", "reload", "confirm");
    private static final List<String> LIVES_ACTIONS = Arrays.asList("set", "give", "take");
    private static final List<String> GRACE_ACTIONS = Arrays.asList("set", "remove");
    private static final List<String> CONFIRM_ACTIONS = Arrays.asList("overwrite", "stack", "cancel");

    private final PolarSouls plugin;
    private final DatabaseManager databaseManager;

    // Tracks pending grace confirmations per sender confirmation key (class + name via getConfirmationKey)
    private final Map<String, PendingGrace> pendingGraceConfirmations = new ConcurrentHashMap<>();

    /**
     * Stores pending grace confirmations so we can ask before overwriting.
     * @param targetUuid The UUID of the player whose grace is being set
     * @param targetName The username of the target player
     * @param requestedMillis The requested grace period duration in milliseconds
     * @param existingGraceUntil The existing grace_until timestamp (if any)
     * @param createdAt Timestamp when this pending confirmation was created (for TTL cleanup)
     */
    private record PendingGrace(UUID targetUuid, String targetName, long requestedMillis, long existingGraceUntil, long createdAt) {}

    public AdminCommand(PolarSouls plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        // Periodically clean up stale pending confirmations (every 5 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupStalePendingConfirmations, 6000L, 6000L);
    }

    /**
     * Generate a consistent key for pending grace confirmations.
     * Uses a stable identifier to avoid key changes from proxy/plugin wrappers.
     */
    private String getConfirmationKey(CommandSender sender) {
        if (sender instanceof Player player) {
            // For players, use their UUID which is stable
            return "player:" + player.getUniqueId();
        } else {
            // For console and other non-player senders, use a stable name-based key
            // This avoids instability from proxy/plugin wrapper classes
            return "sender:" + sender.getName();
        }
    }
    /**
     * Remove pending confirmations older than 5 minutes to prevent memory leaks.
     */
    private void cleanupStalePendingConfirmations() {
        long now = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000L;
        pendingGraceConfirmations.entrySet().removeIf(entry -> (now - entry.getValue().createdAt()) > fiveMinutes);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.checkPermission(sender, "polarsouls.admin")) {
            return true;
        }

        // Security check: Prevent Limbo-only OP from using this command
        if (PermissionUtil.isBlockedByLimboOpSecurity(sender, plugin)) {
            PermissionUtil.sendSecurityBlockMessage(sender);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
        } else {
            dispatch(sender, args);
        }
        return true;
    }

    private void dispatch(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase()) {
            case SUB_LIVES  -> handleLives(sender, args);
            case SUB_GRACE  -> handleGrace(sender, args);
            case "kill"     -> handleKill(sender, args);
            case SUB_REVIVE -> handleRevive(sender, args);
            case "reset"    -> handleReset(sender, args);
            case "info"     -> handleInfo(sender, args);
            case "reload"   -> handleReload(sender);
            case "confirm"  -> handleGraceConfirm(sender, args);
            default -> sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin <subcommand> [args]"));
        }
    }

    private void handleLives(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin lives <set|give|take> <player> <amount>"));
            return;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];
        int amount = parseIntOrError(sender, args[3]);
        if (amount < 0) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeLivesChange(sender, targetName, action, amount));
    }

    private void executeLivesChange(CommandSender sender, String targetName,
                                     String action, int amount) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        int newLives = computeNewLives(playerData.getLives(), action, amount);
        int maxLives = plugin.getMaxLives();
        if (maxLives > 0 && newLives > maxLives) {
            newLives = maxLives;
        }

        databaseManager.setLives(playerData.getUuid(), newLives);

        plugin.getLogger().log(Level.INFO, "{0} {1} lives for {2}: now {3}",
                new Object[]{sender.getName(), action, playerData.getUsername(), newLives});

        sender.sendMessage(MessageUtil.get("admin-lives-updated",
                KEY_PLAYER, playerData.getUsername(),
                KEY_ACTION, action,
                KEY_LIVES, newLives));

        if (newLives <= 0) {
            sender.sendMessage(MessageUtil.colorize(
                    "&c" + playerData.getUsername() + " is now dead (0 lives)."));
        } else if (playerData.isDead()) {
            restoreOnlineSpectator(playerData);
        }
    }

    private static int computeNewLives(int current, String action, int amount) {
        return switch (action) {
            case "give" -> current + amount;
            case "take" -> Math.max(0, current - amount);
            default -> amount; // "set"
        };
    }

    private void handleGrace(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin grace <set|remove> <player> [time]"));
            return;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeGraceChange(sender, args, targetName, action));
    }

    private void executeGraceChange(CommandSender sender, String[] args,
                                     String targetName, String action) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        if ("remove".equals(action)) {
            long now = System.currentTimeMillis();
            // IMPORTANT: We use direct timestamp check instead of isInGracePeriod() here.
            // Reason: isInGracePeriod() has legacy fallback logic (firstJoin + global config).
            // For the "grace remove" command, we only want to remove explicit grace periods
            // (grace_until > 0), not affect legacy grace calculations.
            if (playerData.getGraceUntil() <= 0 || playerData.getGraceUntil() <= now) {
                sender.sendMessage(MessageUtil.colorize(
                        "&e" + playerData.getUsername() + " &7does not have an active grace period."));
                return;
            }
            // Set to 0L (not -1L) to explicitly mark grace as removed while preserving
            // the legacy grace fallback behavior if needed. This prevents the legacy
            // grace calculation from being re-triggered after explicit removal.
            databaseManager.setGraceUntil(playerData.getUuid(), 0L);
            plugin.getLogger().log(Level.INFO, "{0} removed grace period for {1}",
                    new Object[]{sender.getName(), playerData.getUsername()});
            sender.sendMessage(MessageUtil.get("admin-grace-removed",
                    KEY_PLAYER, playerData.getUsername()));
        } else {
            applyGraceSet(sender, args, playerData);
        }
    }

    private void applyGraceSet(CommandSender sender, String[] args, PlayerData data) {
        if (args.length < 4) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin grace set <player> <time> (e.g., 1h30m, 2h, 90m)"));
            return;
        }
        String timeStr = args[3];
        long millis = TimeUtil.parseTimeToMillis(timeStr);
        
        if (millis < 0) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cInvalid time format: " + timeStr + ". Use formats like: 1h30m, 2h, 90m"));
            return;
        }

        long now = System.currentTimeMillis();

        // Issue #21: Check if grace is already active and prompt for confirmation
        if (data.getGraceUntil() > now) {
            String remaining = data.getGraceTimeRemaining(plugin.getGracePeriodMillis());
            String confirmationKey = getConfirmationKey(sender);
            pendingGraceConfirmations.put(confirmationKey,
                    new PendingGrace(data.getUuid(), data.getUsername(), millis, data.getGraceUntil(), now));

            sender.sendMessage(MessageUtil.colorize(
                    "&e" + data.getUsername() + " &7already has an active grace period (&e" + remaining + " &7remaining)."));
            sender.sendMessage(MessageUtil.colorize("&7Choose an option:"));
            sendGraceConfirmOptions(sender);
            return;
        }

        // No existing grace — apply directly
        long graceUntil = now + millis;
        databaseManager.setGraceUntil(data.getUuid(), graceUntil);

        String formattedTime = TimeUtil.formatTime(millis);
        plugin.getLogger().log(Level.INFO, "{0} set grace period for {1} ({2})",
                new Object[]{sender.getName(), data.getUsername(), formattedTime});
        sender.sendMessage(MessageUtil.colorize(
                "&aGrace period set for &e" + data.getUsername() + "&a (" + formattedTime + " from now)."));
    }

    private static void sendGraceConfirmOptions(CommandSender sender) {
        if (sender instanceof Player player) {
            TextComponent overwrite = new TextComponent(MessageUtil.colorize("  &a[&lOverwrite&a]"));
            overwrite.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/psadmin confirm overwrite"));
            overwrite.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(MessageUtil.colorize("&7Overwrite with new grace period"))));

            TextComponent stack = new TextComponent(MessageUtil.colorize(" &e[&lStack&e]"));
            stack.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/psadmin confirm stack"));
            stack.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(MessageUtil.colorize("&7Stack (add time to existing)"))));

            TextComponent cancel = new TextComponent(MessageUtil.colorize(" &c[&lCancel&c]"));
            cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/psadmin confirm cancel"));
            cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(MessageUtil.colorize("&7Cancel the operation"))));

            player.spigot().sendMessage(overwrite, stack, cancel);
        } else {
            // Console fallback: plain text instructions
            sender.sendMessage(MessageUtil.colorize(
                    "  &a/psadmin confirm overwrite &7- Overwrite with new grace period"));
            sender.sendMessage(MessageUtil.colorize(
                    "  &a/psadmin confirm stack &7- Stack (add time to existing)"));
            sender.sendMessage(MessageUtil.colorize(
                    "  &a/psadmin confirm cancel &7- Cancel the operation"));
        }
    }

    private void handleGraceConfirm(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin confirm <overwrite|stack|cancel>"));
            return;
        }

        PendingGrace pending = pendingGraceConfirmations.remove(getConfirmationKey(sender));
        if (pending == null) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cNo pending grace confirmation found. Use /psadmin grace set first."));
            return;
        }

        // Check if confirmation has expired (older than 2 minutes)
        long confirmationAge = System.currentTimeMillis() - pending.createdAt();
        long twoMinutesMillis = 2 * 60 * 1000L;
        if (confirmationAge > twoMinutesMillis) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cGrace confirmation expired. Please run /psadmin grace set again."));
            return;
        }

        String choice = args[1].toLowerCase();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeGraceConfirm(sender, pending, choice));
    }

    private void executeGraceConfirm(CommandSender sender, PendingGrace pending, String choice) {
        long now = System.currentTimeMillis();

        switch (choice) {
            case "overwrite" -> {
                long graceUntil = now + pending.requestedMillis();
                databaseManager.setGraceUntil(pending.targetUuid(), graceUntil);

                String formattedTime = TimeUtil.formatTime(pending.requestedMillis());
                plugin.getLogger().log(Level.INFO, "{0} overwrote grace period for {1} ({2})",
                        new Object[]{sender.getName(), pending.targetName(), formattedTime});
                // Build message before scheduling to reduce work on main thread
                String message = MessageUtil.colorize(
                        "&aGrace period overwritten for &e" + pending.targetName()
                        + "&a (" + formattedTime + " from now).");
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
            case "stack" -> {
                // If existing grace expired, stack from now; otherwise add to existing end time
                long baseTime = Math.max(pending.existingGraceUntil(), now);
                long graceUntil = baseTime + pending.requestedMillis();
                databaseManager.setGraceUntil(pending.targetUuid(), graceUntil);

                String totalRemaining = TimeUtil.formatTime(graceUntil - now);
                plugin.getLogger().log(Level.INFO, "{0} stacked grace period for {1} (total: {2})",
                        new Object[]{sender.getName(), pending.targetName(), totalRemaining});
                // Build message before scheduling to reduce work on main thread
                String message = MessageUtil.colorize(
                        "&aGrace period stacked for &e" + pending.targetName()
                        + "&a (total remaining: " + totalRemaining + ").");
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
            case "cancel" -> {
                // Build message before scheduling to reduce work on main thread
                String message = MessageUtil.colorize("&7Grace period operation cancelled.");
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
            default -> {
                // Invalid choice, re-add the pending confirmation with updated timestamp
                // to prevent premature cleanup
                PendingGrace renewed = new PendingGrace(
                    pending.targetUuid(), 
                    pending.targetName(), 
                    pending.requestedMillis(), 
                    pending.existingGraceUntil(), 
                    System.currentTimeMillis()
                );
                pendingGraceConfirmations.put(getConfirmationKey(sender), renewed);
                // Build message before scheduling to reduce work on main thread
                String message = MessageUtil.colorize(
                        "&cInvalid option. Use: /psadmin confirm <overwrite|stack|cancel>");
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
        }
    }

    private void handleKill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /psadmin kill <player>"));
            return;
        }

        String targetName = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeKill(sender, targetName));
    }

    private void executeKill(CommandSender sender, String targetName) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        if (playerData.isDead()) {
            sender.sendMessage(MessageUtil.colorize(
                    "&c" + playerData.getUsername() + " is already dead."));
            return;
        }

        databaseManager.setLives(playerData.getUuid(), 0);

        plugin.getLogger().log(Level.INFO, "{0} force-killed {1}",
                new Object[]{sender.getName(), playerData.getUsername()});
        sender.sendMessage(MessageUtil.get("admin-killed",
                KEY_PLAYER, playerData.getUsername()));

        Player target = Bukkit.getPlayer(playerData.getUuid());
        if (target != null && target.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    applyDeathTransition(target));
        }
    }

    private void applyDeathTransition(Player target) {
        if (!target.isOnline()) return;

        String deathMode = plugin.getDeathMode();
        switch (deathMode) {
            case PolarSouls.MODE_SPECTATOR -> {
                target.setGameMode(GameMode.SPECTATOR);
                target.sendMessage(MessageUtil.get("death-now-spectator"));
            }
            case PolarSouls.MODE_HYBRID -> {
                target.setGameMode(GameMode.SPECTATOR);
                // Use hybrid-specific warning message with timeout info (consistent with normal death flow)
                int timeout = plugin.getHybridTimeoutSeconds();
                String timeoutStr = formatTime(timeout);
                target.sendMessage(MessageUtil.get("death-hybrid-warning",
                        "timeout", timeoutStr));

                // Schedule the hybrid timeout and register it for proper cancellation
                UUID targetUuid = target.getUniqueId();
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Remove from tracking when task executes
                    plugin.getMainServerListener().cancelHybridTransfer(targetUuid);
                    if (target.isOnline()) {
                        target.sendMessage(MessageUtil.get("death-sent-to-limbo"));
                        ServerTransferUtil.sendToLimbo(target);
                    }
                }, plugin.getHybridTimeoutSeconds() * 20L);

                // Register task with MainServerListener so it can be cancelled if player is revived
                plugin.getMainServerListener().registerHybridTransfer(targetUuid, task);
            }
            default -> {
                target.sendMessage(MessageUtil.get("death-sent-to-limbo"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (target.isOnline()) {
                        ServerTransferUtil.sendToLimbo(target);
                    }
                }, plugin.getSendToLimboDelayTicks());
            }
        }
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

    private void handleRevive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                    "&cUsage: /psadmin revive <player> [lives]"));
            return;
        }

        String targetName = args[1];
        int livesToRestore = args.length >= 3
                ? parseIntOrError(sender, args[2])
                : plugin.getLivesOnRevive();
        if (livesToRestore < 0) return;

        final int lives = livesToRestore;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeRevive(sender, targetName, lives));
    }

    private void executeRevive(CommandSender sender, String targetName, int livesToRestore) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        if (!playerData.isDead()) {
            sender.sendMessage(MessageUtil.get("revive-not-dead",
                    KEY_PLAYER, playerData.getUsername()));
            return;
        }

        boolean success = databaseManager.revivePlayer(playerData.getUuid(), livesToRestore);
        if (success) {
            plugin.getLogger().log(Level.INFO, "{0} revived {1} (lives: {2})",
                    new Object[]{sender.getName(), playerData.getUsername(), livesToRestore});
            sender.sendMessage(MessageUtil.get("revive-admin-success",
                    KEY_PLAYER, playerData.getUsername(),
                    KEY_LIVES, livesToRestore));
            restoreOnlineSpectator(playerData);

            // Remove any dropped player head items from all worlds
            plugin.removeDroppedHeads(playerData.getUuid());
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    "&cFailed to revive " + playerData.getUsername()));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /psadmin reset <player>"));
            return;
        }

        String targetName = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeReset(sender, targetName));
    }

    private void executeReset(CommandSender sender, String targetName) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        int defaultLives = plugin.getDefaultLives();
        PlayerData fresh = PlayerData.createNew(playerData.getUuid(), playerData.getUsername(),
                defaultLives, plugin.getGracePeriodMillis());
        databaseManager.savePlayer(fresh);

        plugin.getLogger().log(Level.INFO, "{0} reset {1} to defaults ({2} lives)",
                new Object[]{sender.getName(), playerData.getUsername(), defaultLives});
        sender.sendMessage(MessageUtil.get("admin-reset",
                KEY_PLAYER, playerData.getUsername(),
                KEY_LIVES, defaultLives));

        restoreOnlineSpectator(playerData);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /psadmin info <player>"));
            return;
        }

        String targetName = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                executeInfo(sender, targetName));
    }

    private void executeInfo(CommandSender sender, String targetName) {
        PlayerData playerData = databaseManager.getPlayerByName(targetName);
        if (playerData == null) {
            notFound(sender, targetName);
            return;
        }

        sendInfoHeader(sender, playerData);
        sendInfoDetails(sender, playerData);
        sender.sendMessage(MessageUtil.colorize("&6&l═══════════════"));
    }

    private void sendInfoHeader(CommandSender sender, PlayerData data) {
        sender.sendMessage(MessageUtil.colorize("&6&l══ Player Info ══"));
        sender.sendMessage(MessageUtil.colorize("&7Player: &f" + data.getUsername()));
        sender.sendMessage(MessageUtil.colorize("&7UUID: &f" + data.getUuid()));
        sender.sendMessage(MessageUtil.colorize("&7Lives: &e" + data.getLives()
                + " &7/ &e" + plugin.getMaxLives()));
        sender.sendMessage(MessageUtil.colorize("&7Status: "
                + (data.isDead() ? "&4Dead" : "&aAlive")));
    }

    private void sendInfoDetails(CommandSender sender, PlayerData data) {
        Player target = Bukkit.getPlayer(data.getUuid());
        String onlineStr;
        if (target != null && target.isOnline()) {
            onlineStr = "&a" + target.getGameMode().name();
        } else {
            onlineStr = "&cOffline";
        }

        sender.sendMessage(MessageUtil.colorize("&7Online: " + onlineStr));
        sender.sendMessage(MessageUtil.colorize("&7Grace: " + buildGraceStatus(data)));
        sender.sendMessage(MessageUtil.colorize("&7First Join: &f"
                + formatTimestamp(data.getFirstJoin())));
        sender.sendMessage(MessageUtil.colorize("&7Last Death: "
                + (data.getLastDeath() == 0 ? "&7Never"
                        : "&c" + formatTimestamp(data.getLastDeath()))));
    }

    private String buildGraceStatus(PlayerData data) {
        if (data.getGraceUntil() > 0 && data.getGraceUntil() > System.currentTimeMillis()) {
            return "&a" + data.getGraceTimeRemaining(plugin.getGracePeriodMillis()) + " remaining";
        }
        long graceMillis = plugin.getGracePeriodMillis();
        if (graceMillis <= 0 && data.getGraceUntil() <= 0) {
            return "&7Disabled";
        }
        if (data.isInGracePeriod(graceMillis)) {
            return "&a" + data.getGraceTimeRemaining(graceMillis) + " remaining";
        }
        return "&7Expired";
    }

    private void handleReload(CommandSender sender) {
        plugin.loadConfigValues();
        sender.sendMessage(MessageUtil.get("reload-success"));
        plugin.getLogger().log(Level.INFO, "{0} reloaded PolarSouls config.", sender.getName());
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("&6&l══ PolarSouls Admin ══"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin lives set <player> <n>  &7- Set lives"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin lives give <player> <n> &7- Add lives"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin lives take <player> <n> &7- Remove lives"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin grace set <player> <time> &7- Grant grace (e.g., 1h30m, 2h, 90m)"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin grace remove <player>   &7- Remove grace"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin confirm <overwrite|stack|cancel> &7- Confirm grace operation"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin kill <player>           &7- Force-kill"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin revive <player> [lives] &7- Revive"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin reset <player>          &7- Reset to defaults"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin info <player>           &7- Detailed info"));
        sender.sendMessage(MessageUtil.colorize("&e/psadmin reload                  &7- Reload config"));
        sender.sendMessage(MessageUtil.colorize("&6&l═══════════════════════"));
    }

    private static int parseIntOrError(CommandSender sender, String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize(ERR_NUMBER + text));
            return -1;
        }
    }

    private static void notFound(CommandSender sender, String name) {
        sender.sendMessage(MessageUtil.get("revive-player-not-found", KEY_PLAYER, name));
    }

    private void restoreOnlineSpectator(PlayerData data) {
        PlayerRevivalUtil.restoreOnlineSpectator(plugin, data);
    }

    private static String formatTimestamp(long millis) {
        if (millis <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(millis));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(SUB_COMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        return switch (args.length) {
            case 2 -> completeArgTwo(sub, args[1]);
            case 3 -> completeArgThree(sub, args);
            case 4 -> completeArgFour(sub, args);
            default -> Collections.emptyList();
        };
    }

    private static List<String> completeArgTwo(String sub, String partial) {
        return switch (sub) {
            case SUB_LIVES -> filterStartsWith(LIVES_ACTIONS, partial);
            case SUB_GRACE -> filterStartsWith(GRACE_ACTIONS, partial);
            case "confirm" -> filterStartsWith(CONFIRM_ACTIONS, partial);
            default -> playerNames(partial);
        };
    }

    private static List<String> completeArgThree(String sub, String[] args) {
        return switch (sub) {
            case SUB_LIVES, SUB_GRACE -> playerNames(args[2]);
            case SUB_REVIVE -> Arrays.asList("1", "2", "3");
            default -> Collections.emptyList();
        };
    }

    private static List<String> completeArgFour(String sub, String[] args) {
        return switch (sub) {
            case SUB_LIVES -> Arrays.asList("1", "2", "3", "5");
            case SUB_GRACE -> "set".equalsIgnoreCase(args[1])
                    ? Arrays.asList("1h", "6h", "12h", "24h", "1h30m", "2h30m")
                    : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private static List<String> filterStartsWith(List<String> options, String prefix) {
        return TabCompleteUtil.filterStartsWith(options, prefix);
    }

    private static List<String> playerNames(String prefix) {
        return TabCompleteUtil.getOnlinePlayerNames(prefix);
    }
}
