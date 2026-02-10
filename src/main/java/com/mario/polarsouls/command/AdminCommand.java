package com.mario.polarsouls.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.MessageUtil;
import com.mario.polarsouls.util.ServerTransferUtil;
import com.mario.polarsouls.util.TimeUtil;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String KEY_PLAYER = "player";
    private static final String KEY_LIVES = "lives";
    private static final String KEY_ACTION = "action";
    private static final String KEY_HOURS = "hours";
    private static final String KEY_TIME = "time";
    private static final String SUB_LIVES = "lives";
    private static final String SUB_GRACE = "grace";
    private static final String SUB_REVIVE = "revive";
    private static final String ERR_NUMBER = "&cInvalid number: ";

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            SUB_LIVES, SUB_GRACE, "kill", SUB_REVIVE, "reset", "info", "reload");
    private static final List<String> LIVES_ACTIONS = Arrays.asList("set", "give", "take");
    private static final List<String> GRACE_ACTIONS = Arrays.asList("set", "remove");

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public AdminCommand(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        int newLives = computeNewLives(data.getLives(), action, amount);
        int maxLives = plugin.getMaxLives();
        if (maxLives > 0 && newLives > maxLives) {
            newLives = maxLives;
        }

        db.setLives(data.getUuid(), newLives);

        plugin.getLogger().log(Level.INFO, "{0} {1} lives for {2}: now {3}",
                new Object[]{sender.getName(), action, data.getUsername(), newLives});

        sender.sendMessage(MessageUtil.get("admin-lives-updated",
                KEY_PLAYER, data.getUsername(),
                KEY_ACTION, action,
                KEY_LIVES, newLives));

        if (newLives <= 0) {
            sender.sendMessage(MessageUtil.colorize(
                    "&c" + data.getUsername() + " is now dead (0 lives)."));
        } else if (data.isDead()) {
            restoreOnlineSpectator(data);
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        if ("remove".equals(action)) {
            db.setFirstJoin(data.getUuid(), 0L);
            plugin.getLogger().log(Level.INFO, "{0} removed grace period for {1}",
                    new Object[]{sender.getName(), data.getUsername()});
            sender.sendMessage(MessageUtil.get("admin-grace-removed",
                    KEY_PLAYER, data.getUsername()));
        } else {
            applyGraceSet(sender, args, data);
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

        db.setFirstJoin(data.getUuid(), System.currentTimeMillis());
        updateLastSeenForGrace(data);

        String formattedTime = TimeUtil.formatTime(millis);
        plugin.getLogger().log(Level.INFO, "{0} set grace period for {1} ({2})",
                new Object[]{sender.getName(), data.getUsername(), formattedTime});
        sender.sendMessage(MessageUtil.colorize(
                "&aGrace period set for &e" + data.getUsername() + "&a (" + formattedTime + " from now)."));
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        db.setLives(data.getUuid(), 0);

        plugin.getLogger().log(Level.INFO, "{0} force-killed {1}",
                new Object[]{sender.getName(), data.getUsername()});
        sender.sendMessage(MessageUtil.get("admin-killed",
                KEY_PLAYER, data.getUsername()));

        Player target = Bukkit.getPlayer(data.getUuid());
        if (target != null && target.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    applySpectatorIfOnline(target));
        }
    }

    private static void applySpectatorIfOnline(Player target) {
        if (target.isOnline()) {
            target.setGameMode(GameMode.SPECTATOR);
            target.sendMessage(MessageUtil.get("death-now-spectator"));
        }
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        if (!data.isDead()) {
            sender.sendMessage(MessageUtil.get("revive-not-dead",
                    KEY_PLAYER, data.getUsername()));
            return;
        }

        boolean success = db.revivePlayer(data.getUuid(), livesToRestore);
        if (success) {
            plugin.getLogger().log(Level.INFO, "{0} revived {1} (lives: {2})",
                    new Object[]{sender.getName(), data.getUsername(), livesToRestore});
            sender.sendMessage(MessageUtil.get("revive-admin-success",
                    KEY_PLAYER, data.getUsername(),
                    KEY_LIVES, livesToRestore));
            restoreOnlineSpectator(data);
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    "&cFailed to revive " + data.getUsername()));
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        int defaultLives = plugin.getDefaultLives();
        PlayerData fresh = PlayerData.createNew(data.getUuid(), data.getUsername(), defaultLives);
        db.savePlayer(fresh);
        db.setFirstJoin(data.getUuid(), fresh.getFirstJoin());
        updateLastSeenForGrace(fresh);

        plugin.getLogger().log(Level.INFO, "{0} reset {1} to defaults ({2} lives)",
                new Object[]{sender.getName(), data.getUsername(), defaultLives});
        sender.sendMessage(MessageUtil.get("admin-reset",
                KEY_PLAYER, data.getUsername(),
                KEY_LIVES, defaultLives));

        restoreOnlineSpectator(data);
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
        PlayerData data = db.getPlayerByName(targetName);
        if (data == null) {
            notFound(sender, targetName);
            return;
        }

        sendInfoHeader(sender, data);
        sendInfoDetails(sender, data);
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
        long graceMillis = plugin.getGracePeriodMillis();
        if (graceMillis <= 0) {
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
        Player target = Bukkit.getPlayer(data.getUuid());
        if (target != null && target.isOnline()
                && target.getGameMode() != GameMode.SURVIVAL) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    plugin.getLimboDeadPlayers().remove(target.getUniqueId());
                    target.setGameMode(GameMode.SURVIVAL);
                    target.sendMessage(MessageUtil.get("revive-success"));

                    if (plugin.isLimboServer()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (target.isOnline()) {
                                ServerTransferUtil.sendToMain(target);
                            }
                        }, 40L);
                    }
                }
            });
        }
    }

    private void updateLastSeenForGrace(PlayerData data) {
        Player target = Bukkit.getPlayer(data.getUuid());
        if (target != null && target.isOnline()) {
            db.setLastSeen(data.getUuid(), 0L);
        } else {
            db.setLastSeen(data.getUuid(), System.currentTimeMillis());
        }
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
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private static List<String> playerNames(String prefix) {
        List<String> names = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lower)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
