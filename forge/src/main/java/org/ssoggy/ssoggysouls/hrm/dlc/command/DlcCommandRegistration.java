package org.ssoggy.ssoggysouls.hrm.dlc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.listener.GhostModeEvents;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcCommandResult;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeathRecord;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcNames;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStat;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStats;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcTrustAction;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcTrustService;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.listener.ServerLifecycleListener;
import org.ssoggy.ssoggysouls.model.PlayerData;
import org.ssoggy.ssoggysouls.util.ConfigManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class DlcCommandRegistration {
    private static final String ACTION = "action";
    private static final String PLAYER = "player";
    private static final String GROUP = "group";
    private static final String KEY = "key";
    private static final String EDIT = "edit";
    private static final String BLOCK = "block";

    private static final String C_STRUCTURE = "structure";
    private static final String C_GAMERULE = "gamerule";
    private static final String C_TIMER = "timer";
    private static final String C_RELOAD = "reload";
    private static final String C_SOUL_SAND = "soul-sand-blocktag";
    private static final String C_FLOWER = "flower-blocktag";
    private static final String C_ORE = "ore-blocktag";
    private static final String C_FENCE = "fence-blocktag";
    private static final String C_STAIR = "stair-blocktag";
    private static final String C_LOSE_INV = "lose-inventory";
    private static final String C_RESTRICT_MENU = "restrict-menu-access";
    private static final String C_CREATIVE_HEADS = "creative-players-drop-heads";
    private static final String C_KEEP_BASE = "keep-structure-base";
    private static final String C_HEAD_EFFECTS = "head-effects";
    private static final String C_HEAD_BURNS = "head-burns-in-lava";
    private static final String C_RITUAL_LIGHTNING = "ritual-lightning-strike";
    private static final String C_RITUAL_TOTEM = "ritual-totem-effect";
    private static final String C_GHOST_PARTICLES = "ghost-mode-particles";
    private static final String C_TRUSTED_OBIT = "trusted-obituary-after";
    private static final String C_FRIENDS_OBIT = "friends-obituary-after";
    private static final String C_PUBLIC_OBIT = "public-obituary-after";
    private static final String C_HEADRESTRICT = "spectator-headrestrict-radius";
    private static final String C_REVIVE_RESIST = "revive-resistance-ticks";
    private static final String C_REVIVE_GLOW = "revive-glowing-ticks";

    private static final List<String> TRUST_ACTIONS = DlcTrustAction.VALUES.stream()
            .map(action -> action.name().toLowerCase(Locale.ROOT))
            .toList();
    private static final List<String> CONFIG_GROUPS = List.of(C_STRUCTURE, C_GAMERULE, C_TIMER, C_RELOAD);
    private static final List<String> STRUCTURE_KEYS = List.of(
            C_SOUL_SAND,
            C_FLOWER,
            C_ORE,
            C_FENCE,
            C_STAIR
    );
    private static final List<String> GAMERULE_KEYS = List.of(
            C_LOSE_INV,
            C_RESTRICT_MENU,
            C_CREATIVE_HEADS,
            C_KEEP_BASE,
            C_HEAD_EFFECTS,
            C_HEAD_BURNS,
            C_RITUAL_LIGHTNING,
            C_RITUAL_TOTEM,
            C_GHOST_PARTICLES
    );
    private static final List<String> TIMER_KEYS = List.of(
            C_TRUSTED_OBIT,
            C_FRIENDS_OBIT,
            C_PUBLIC_OBIT,
            C_HEADRESTRICT,
            C_REVIVE_RESIST,
            C_REVIVE_GLOW
    );
    private static final List<String> EDIT_ACTIONS = List.of("add", "remove", "reset");
    private static final Map<String, List<String>> DEFAULT_STRUCTURE = Map.of(
            C_SOUL_SAND, List.of("CRYING_OBSIDIAN", "OBSIDIAN"),
            C_FLOWER, List.of("SOUL_TORCH", "REDSTONE_TORCH"),
            C_ORE, List.of("ENCHANTING_TABLE"),
            C_FENCE, List.of("OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE", "ACACIA_FENCE", "DARK_OAK_FENCE", "MANGROVE_FENCE", "CHERRY_FENCE", "BAMBOO_FENCE", "CRIMSON_FENCE", "WARPED_FENCE", "NETHER_BRICK_FENCE"),
            C_STAIR, List.of("MAGMA_BLOCK")
    );

    private DlcCommandRegistration() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, DatabaseManager db) {
        registerTrustCommand(dispatcher, db);
        registerDeathListCommand(dispatcher, "deathlist");
        registerDeathListCommand(dispatcher, "obituaries");
        registerGhostModeCommand(dispatcher, db);
        registerConfigCommand(dispatcher);
    }

    private static void registerTrustCommand(CommandDispatcher<CommandSourceStack> dispatcher, DatabaseManager db) {
        dispatcher.register(Commands.literal("trust")
                .executes(context -> {
                    sendResult(context.getSource(), DlcCommandResult.fail("Please use /trust <action> [player]"));
                    return 0;
                })
                .then(Commands.argument(ACTION, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TRUST_ACTIONS, builder))
                        .executes(context -> executeTrust(context.getSource(), db, StringArgumentType.getString(context, ACTION), null))
                        .then(Commands.argument(PLAYER, StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        context.getSource().getServer().getPlayerNames(),
                                        builder))
                                .executes(context -> executeTrust(context.getSource(), db,
                                        StringArgumentType.getString(context, ACTION),
                                        StringArgumentType.getString(context, PLAYER))))));
    }

    private static int executeTrust(CommandSourceStack source, DatabaseManager db, String rawAction, String targetName) {
        if (!source.isPlayer()) {
            sendResult(source, DlcCommandResult.fail("This command can only be run by a player."));
            return 0;
        }

        Optional<DlcTrustAction> action = DlcTrustAction.fromInput(rawAction);
        if (action.isEmpty()) {
            sendResult(source, DlcCommandResult.fail("Please use /trust <grant|revoke|block|info> [player]"));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (action.get() == DlcTrustAction.INFO && targetName == null) {
            sendTrustResult(source, DlcTrustService.execute(player.getUUID(), player.getScoreboardName(), null, null, DlcTrustAction.INFO));
            return 1;
        }

        if (targetName == null) {
            sendResult(source, DlcCommandResult.fail("Please use /trust <action> [player]"));
            return 0;
        }

        CompletableFuture.runAsync(() -> {
            ResolvedPlayer target = resolvePlayer(source, db, targetName);
            source.getServer().execute(() -> {
                if (target == null) {
                    sendResult(source, DlcCommandResult.fail("Player not found: " + targetName));
                    return;
                }
                DlcTrustService.TrustResult result = DlcTrustService.execute(
                        player.getUUID(),
                        player.getScoreboardName(),
                        target.uuid(),
                        target.name(),
                        action.get()
                );
                sendTrustResult(source, result);
                if (result.targetMessage() != null && target.onlinePlayer() != null) {
                    target.onlinePlayer().sendSystemMessage(format(DlcCommandResult.success(result.targetMessage())));
                }
            });
        });
        return 1;
    }

    private static void registerDeathListCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    List<DlcDeathRecord> deaths;
                    if (source.isPlayer()) {
                        deaths = DlcDeaths.visibleDeaths(
                                source.getPlayer().getUUID(),
                                ConfigManager.getConfig().getTrustedObituaryAfter(),
                                ConfigManager.getConfig().getFriendsObituaryAfter(),
                                ConfigManager.getConfig().getPublicObituaryAfter()
                        );
                    } else {
                        deaths = DlcDeaths.allDeaths();
                    }

                    if (deaths.isEmpty()) {
                        sendResult(source, DlcCommandResult.fail("There are no public deaths currently."));
                        return 0;
                    }

                    sendResult(source, DlcCommandResult.success("Here is a list of all the current public deaths"));
                    for (DlcDeathRecord death : deaths) {
                        source.sendSystemMessage(formatDeathComponent(death));
                    }
                    return 1;
                }));
    }

    private static void registerGhostModeCommand(CommandDispatcher<CommandSourceStack> dispatcher, DatabaseManager db) {
        dispatcher.register(Commands.literal("ghostmode")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!source.isPlayer()) {
                        sendResult(source, DlcCommandResult.fail("Please use /ghostmode <player> from console."));
                        return 0;
                    }
                    return setGhostMode(source, db, source.getPlayer());
                })
                .then(Commands.argument(PLAYER, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerNames(),
                                builder))
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, PLAYER);
                            ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                            if (target == null) {
                                sendResult(context.getSource(), DlcCommandResult.fail("Online player not found: " + targetName));
                                return 0;
                            }
                            return setGhostMode(context.getSource(), db, target);
                        })));
    }

    private static int setGhostMode(CommandSourceStack source, DatabaseManager db, ServerPlayer target) {
        CompletableFuture.runAsync(() -> {
            db.setLives(target.getUUID(), 0);
            DlcNames.cache(target.getUUID(), target.getScoreboardName());
            new DlcStats(target.getUUID()).incrementStat(DlcStat.DEATHS, 1);
            source.getServer().execute(() -> {
                target.setGameMode(GameType.ADVENTURE);
                ServerLifecycleListener.setGhostModeAttributes(target, true);
                GhostModeEvents.updateGhostStatus(target.getUUID(), true);
                GhostState ghostState = GhostState.getServerState(source.getServer());
                ghostState.setDeathLocation(target.getUUID(), target.blockPosition());
                ghostState.removeDeathHolder(target.getUUID());
                ghostState.setDirty();
                DlcDeaths.recordDeath(
                        target.getUUID(),
                        target.getScoreboardName(),
                        ((net.minecraft.server.level.ServerLevel) target.level()).dimension().toString(),
                        target.blockPosition().getX(),
                        target.blockPosition().getY(),
                        target.blockPosition().getZ()
                );
                sendResult(source, DlcCommandResult.success("Updated " + target.getScoreboardName() + " gamemode to GhostMode!"));
            });
        });
        return 1;
    }

    private static void registerConfigCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revivalconfig")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    sendResult(context.getSource(), DlcCommandResult.missingArgs("Please use "));
                    return 0;
                })
                .then(Commands.argument(GROUP, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(CONFIG_GROUPS, builder))
                        .executes(context -> executeConfig(context.getSource(), StringArgumentType.getString(context, GROUP), null, null, null))
                        .then(Commands.argument(KEY, StringArgumentType.word())
                                .suggests((context, builder) -> suggestConfigKeys(context.getArgument(GROUP, String.class), builder))
                                .executes(context -> executeConfig(context.getSource(),
                                        StringArgumentType.getString(context, GROUP),
                                        StringArgumentType.getString(context, KEY),
                                        null,
                                        null))
                                .then(Commands.argument(EDIT, StringArgumentType.word())
                                        .suggests((context, builder) -> suggestConfigValues(
                                                StringArgumentType.getString(context, GROUP),
                                                StringArgumentType.getString(context, KEY),
                                                builder))
                                        .executes(context -> executeConfig(context.getSource(),
                                                StringArgumentType.getString(context, GROUP),
                                                StringArgumentType.getString(context, KEY),
                                                StringArgumentType.getString(context, EDIT),
                                                null))
                                        .then(Commands.argument(BLOCK, StringArgumentType.greedyString())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(blockSuggestions(), builder))
                                                .executes(context -> executeConfig(context.getSource(),
                                                        StringArgumentType.getString(context, GROUP),
                                                        StringArgumentType.getString(context, KEY),
                                                        StringArgumentType.getString(context, EDIT),
                                                        StringArgumentType.getString(context, BLOCK))))))));
    }

    private static int executeConfig(CommandSourceStack source, String group, String key, String editOrValue, String block) {
        String normalizedGroup = normalizeGroup(group);
        if (C_RELOAD.equals(normalizedGroup)) {
            ConfigManager.load();
            sendResult(source, DlcCommandResult.info("Reloaded SSoggySouls config."));
            return 1;
        }
        if (C_GAMERULE.equals(normalizedGroup)) {
            return executeGameruleConfig(source, key, editOrValue);
        }
        if (C_TIMER.equals(normalizedGroup)) {
            return executeTimerConfig(source, key, editOrValue);
        }
        if (C_STRUCTURE.equals(normalizedGroup)) {
            return executeStructureConfig(source, key, editOrValue, block);
        }
        sendResult(source, DlcCommandResult.fail("Unknown config group: " + group));
        return 0;
    }

    private static int executeGameruleConfig(CommandSourceStack source, String key, String value) {
        if (!GAMERULE_KEYS.contains(key)) {
            sendResult(source, DlcCommandResult.fail("Unknown gamerule: " + key));
            return 0;
        }
        if (value == null) {
            sendResult(source, DlcCommandResult.info(key + ": " + getRule(key)));
            return 1;
        }
        setRule(key, Boolean.parseBoolean(value));
        ConfigManager.save();
        sendResult(source, DlcCommandResult.success("Set " + key + " to " + Boolean.parseBoolean(value)));
        return 1;
    }

    private static int executeTimerConfig(CommandSourceStack source, String key, String value) {
        if (!TIMER_KEYS.contains(key)) {
            sendResult(source, DlcCommandResult.fail("Unknown timer: " + key));
            return 0;
        }
        if (value == null) {
            sendResult(source, DlcCommandResult.info(key + ": " + getTimer(key)));
            return 1;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Component interactive = Component.literal("/revivalconfig timer " + key + " ")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/revivalconfig timer " + key + " "))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to auto-fill this command").withStyle(ChatFormatting.GRAY)))
                    );
            Component message = Component.literal("[RevivalPlus] Timer value must be a number. Click to fix: ")
                    .withStyle(ChatFormatting.RED)
                    .append(interactive);
            source.sendSystemMessage(message);
            return 0;
        }
        setTimer(key, parsed);
        ConfigManager.save();
        sendResult(source, DlcCommandResult.success("Set " + key + " to " + parsed));
        return 1;
    }

    private static int executeStructureConfig(CommandSourceStack source, String key, String edit, String block) {
        if (!STRUCTURE_KEYS.contains(key)) {
            sendResult(source, DlcCommandResult.fail("Unknown structure block tag: " + key));
            return 0;
        }
        if (edit == null) {
            sendResult(source, DlcCommandResult.info(key + ": " + getStructureList(key)));
            return 1;
        }

        List<String> values = new ArrayList<>(getStructureList(key));
        switch (edit.toLowerCase(Locale.ROOT)) {
            case "reset" -> values = new ArrayList<>(DEFAULT_STRUCTURE.getOrDefault(key, List.of()));
            case "add" -> {
                String normalized = normalizeBlock(block);
                if (normalized == null) {
                    sendResult(source, DlcCommandResult.fail("Invalid block id."));
                    return 0;
                }
                if (!values.contains(normalized)) {
                    values.add(normalized);
                }
            }
            case "remove" -> {
                if ("ALL".equalsIgnoreCase(block)) {
                    values.clear();
                    break;
                }
                String normalized = normalizeBlock(block);
                if (normalized == null) {
                    sendResult(source, DlcCommandResult.fail("Invalid block id."));
                    return 0;
                }
                values.remove(normalized);
            }
            default -> {
                sendResult(source, DlcCommandResult.fail("Use add, remove, or reset."));
                return 0;
            }
        }
        setStructureList(key, values);
        ConfigManager.save();
        sendResult(source, DlcCommandResult.success("Updated " + key + ": " + values));
        return 1;
    }

    private static ResolvedPlayer resolvePlayer(CommandSourceStack source, DatabaseManager db, String name) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            return new ResolvedPlayer(online.getUUID(), online.getScoreboardName(), online);
        }
        PlayerData data = db.getPlayerByName(name);
        if (data != null) {
            return new ResolvedPlayer(data.getUuid(), data.getUsername(), null);
        }
        return DlcNames.findUuidByName(name)
                .map(uuid -> new ResolvedPlayer(uuid, DlcNames.getOrDefault(uuid, name), null))
                .orElse(null);
    }

    private static void sendTrustResult(CommandSourceStack source, DlcTrustService.TrustResult result) {
        sendResult(source, result.result());
    }

    private static void sendResult(CommandSourceStack source, DlcCommandResult result) {
        for (String line : result.message().split("\\n")) {
            source.sendSystemMessage(format(new DlcCommandResult(result.status(), line, result.details())));
        }
    }

    private static Component format(DlcCommandResult result) {
        if (result.status() == DlcCommandResult.Status.MISSING_ARGS) {
            net.minecraft.network.chat.MutableComponent base = Component.literal("[RevivalPlus] " + result.message()).withStyle(ChatFormatting.RED);
            net.minecraft.network.chat.MutableComponent interactive = Component.literal("/revivalconfig <structure|gamerule|timer|reload>")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY)
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/revivalconfig "))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Click to auto-fill this command").withStyle(ChatFormatting.GRAY)))
                    );
            return base.append(interactive);
        }
        ChatFormatting color = switch (result.status()) {
            case TRUE -> ChatFormatting.GREEN;
            case FALSE -> ChatFormatting.RED;
            case INFO -> ChatFormatting.GRAY;
            case RAW -> ChatFormatting.GOLD;
            case MISSING_ARGS -> ChatFormatting.RED;
        };
        return Component.literal("[RevivalPlus] " + result.message()).withStyle(color);
    }

    private static Component formatDeathComponent(DlcDeathRecord death) {
        String rawUsername = DlcNames.getOrDefault(death.uuid(), death.username());
        String username = rawUsername != null ? rawUsername : "Unknown";
        final String finalUsername = (username != null) ? username : "Unknown";
        String coords = death.x() + " " + death.y() + " " + death.z();

        return Component.literal(finalUsername).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pstatus " + finalUsername))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to check player status").withStyle(s -> s.withColor(ChatFormatting.GRAY)))))
                .append(Component.literal(" has died at ").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
                .append(Component.literal("X" + death.x() + " Y" + death.y() + " Z" + death.z()).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy coordinates").withStyle(s -> s.withColor(ChatFormatting.GRAY))))))
                .append(Component.literal(" in the ").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
                .append(Component.literal(death.worldId()).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)))
                .append(Component.literal(" (" + formatAge(death.time()) + ")").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)));
    }

    private static String formatAge(Instant time) {
        long seconds = Math.max(0, Duration.between(time, Instant.now()).getSeconds());
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private static String normalizeGroup(String group) {
        return switch (group.toLowerCase(Locale.ROOT)) {
            case "s", "struc", "struct", C_STRUCTURE, "1" -> C_STRUCTURE;
            case "g", "gr", "gmr", C_GAMERULE, "gamerules", "2" -> C_GAMERULE;
            case "t", C_TIMER, "3" -> C_TIMER;
            case "r", C_RELOAD, "0" -> C_RELOAD;
            default -> group.toLowerCase(Locale.ROOT);
        };
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestConfigKeys(String group, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String normalized = normalizeGroup(group);
        if (C_STRUCTURE.equals(normalized)) {
            return SharedSuggestionProvider.suggest(STRUCTURE_KEYS, builder);
        }
        if (C_GAMERULE.equals(normalized)) {
            return SharedSuggestionProvider.suggest(GAMERULE_KEYS, builder);
        }
        if (C_TIMER.equals(normalized)) {
            return SharedSuggestionProvider.suggest(TIMER_KEYS, builder);
        }
        return SharedSuggestionProvider.suggest(List.of(), builder);
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestConfigValues(String group, String key, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String normalized = normalizeGroup(group);
        if (C_STRUCTURE.equals(normalized)) {
            return SharedSuggestionProvider.suggest(EDIT_ACTIONS, builder);
        }
        if (C_GAMERULE.equals(normalized)) {
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        }
        if (C_TIMER.equals(normalized)) {
            return SharedSuggestionProvider.suggest(List.of(String.valueOf(getTimer(key))), builder);
        }
        return SharedSuggestionProvider.suggest(List.of(), builder);
    }

    private static class BlockSuggestionsHolder {
        private static final List<String> INSTANCE = BuiltInRegistries.BLOCK.keySet().stream()
                .map(id -> id.getPath().toUpperCase(Locale.ROOT))
                .toList();
    }

    private static List<String> blockSuggestions() {
        return BlockSuggestionsHolder.INSTANCE;
    }

    private static String normalizeBlock(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if ("ALL".equalsIgnoreCase(trimmed)) {
            return "ALL";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        ResourceLocation id = ResourceLocation.tryParse(lower.contains(":") ? lower : "minecraft:" + lower);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        return id.getPath().toUpperCase(Locale.ROOT);
    }

    private static boolean getRule(String key) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        return switch (key) {
            case C_LOSE_INV -> config.isLoseInventory();
            case C_RESTRICT_MENU -> config.isRestrictMenuAccess();
            case C_CREATIVE_HEADS -> config.isCreativePlayersDropHeads();
            case C_KEEP_BASE -> config.isLeaveStructureBase();
            case C_HEAD_EFFECTS -> config.isHeadWearingEffects();
            case C_HEAD_BURNS -> config.isHeadBurnsInLava();
            case C_RITUAL_LIGHTNING -> config.isRitualLightningStrike();
            case C_RITUAL_TOTEM -> config.isRitualTotemEffect();
            case C_GHOST_PARTICLES -> config.isGhostModeParticles();
            default -> false;
        };
    }

    private static void setRule(String key, boolean value) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        switch (key) {
            case C_LOSE_INV -> config.setLoseInventory(value);
            case C_RESTRICT_MENU -> config.setRestrictMenuAccess(value);
            case C_CREATIVE_HEADS -> config.setCreativePlayersDropHeads(value);
            case C_KEEP_BASE -> config.setLeaveStructureBase(value);
            case C_HEAD_EFFECTS -> config.setHeadWearingEffects(value);
            case C_HEAD_BURNS -> {
                config.setHeadBurnsInLava(value);
                config.setHeadPlaceAsBlock(!value);
            }
            case C_RITUAL_LIGHTNING -> config.setRitualLightningStrike(value);
            case C_RITUAL_TOTEM -> config.setRitualTotemEffect(value);
            case C_GHOST_PARTICLES -> config.setGhostModeParticles(value);
            default -> { /* ignored */ }
        }
    }

    private static int getTimer(String key) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        return switch (key) {
            case C_TRUSTED_OBIT -> config.getTrustedObituaryAfter();
            case C_FRIENDS_OBIT -> config.getFriendsObituaryAfter();
            case C_PUBLIC_OBIT -> config.getPublicObituaryAfter();
            case C_HEADRESTRICT -> config.getSpectatorHeadRestrictRadius();
            case C_REVIVE_RESIST -> config.getReviveResistanceTicks();
            case C_REVIVE_GLOW -> config.getReviveGlowingTicks();
            default -> 0;
        };
    }

    private static void setTimer(String key, int value) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        switch (key) {
            case C_TRUSTED_OBIT -> config.setTrustedObituaryAfter(value);
            case C_FRIENDS_OBIT -> config.setFriendsObituaryAfter(value);
            case C_PUBLIC_OBIT -> config.setPublicObituaryAfter(value);
            case C_HEADRESTRICT -> config.setSpectatorHeadRestrictRadius(value);
            case C_REVIVE_RESIST -> config.setReviveResistanceTicks(value);
            case C_REVIVE_GLOW -> config.setReviveGlowingTicks(value);
            default -> { /* ignored */ }
        }
    }

    private static List<String> getStructureList(String key) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        return switch (key) {
            case C_SOUL_SAND -> config.getSoulSandBlockTag();
            case C_FLOWER -> config.getFlowerBlockTag();
            case C_ORE -> config.getOreBlockTag();
            case C_FENCE -> config.getFenceBlockTag();
            case C_STAIR -> config.getStairBlockTag();
            default -> List.of();
        };
    }

    private static void setStructureList(String key, Collection<String> values) {
        ConfigManager.ModConfig config = ConfigManager.getConfig();
        switch (key) {
            case C_SOUL_SAND -> config.setSoulSandBlocktag(values);
            case C_FLOWER -> config.setFlowerBlocktag(values);
            case C_ORE -> config.setOreBlocktag(values);
            case C_FENCE -> config.setFenceBlocktag(values);
            case C_STAIR -> config.setStairBlocktag(values);
            default -> { /* ignored */ }
        }
    }

    private record ResolvedPlayer(java.util.UUID uuid, String name, ServerPlayer onlinePlayer) {
    }
}
