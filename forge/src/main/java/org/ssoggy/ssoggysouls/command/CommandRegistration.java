package org.ssoggy.ssoggysouls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ssoggy.ssoggysouls.SSoggySoulsMod;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.command.DlcCommandRegistration;
import org.ssoggy.ssoggysouls.hrm.dlc.listener.GhostModeEvents;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.listener.ServerLifecycleListener;
import org.ssoggy.ssoggysouls.model.PlayerData;
import org.ssoggy.ssoggysouls.util.AdminLogger;
import org.ssoggy.ssoggysouls.util.MessageUtil;
import org.ssoggy.ssoggysouls.util.PermissionUtil;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = SSoggySoulsMod.MODID)
public class CommandRegistration {
    
    private CommandRegistration() {
        // Utility class
    }

    private static final String PLAYER = "player";
    private static final String LIVES = "lives";
    private static DatabaseManager db;

    public static void setDatabase(DatabaseManager database) {
        db = database;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (db == null) {
            SSoggySoulsMod.LOGGER.error("Cannot register commands: DatabaseManager is null");
            return;
        }
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        registerStatusCommand(dispatcher);
        registerReviveCommand(dispatcher);
        registerSetLivesCommand(dispatcher);
        registerAdminLogCommand(dispatcher);
        DlcCommandRegistration.register(dispatcher, db);
    }

    private static void registerStatusCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pstatus")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                if (!source.isPlayer()) {
                    source.sendFailure(MessageUtil.get("admin-players-only"));
                    return 0;
                }
                ServerPlayer player = source.getPlayer();

                CompletableFuture.runAsync(() -> {
                    PlayerData data = db.getPlayer(player.getUUID());
                    if (data != null) {
                        source.getServer().execute(() ->
                            source.sendSuccess(() -> MessageUtil.get("status-self", LIVES, data.getLives()), false));
                    }
                });
                return 1;
            })
            .then(Commands.argument(PLAYER, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerNames(), builder))
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, PLAYER);
                    CommandSourceStack source = context.getSource();

                    CompletableFuture.runAsync(() -> {
                        PlayerData data = db.getPlayerByName(targetName);
                        if (data == null) {
                            source.getServer().execute(() ->
                                source.sendFailure(MessageUtil.get("status-not-found", PLAYER, targetName)));
                            return;
                        }

                        if (data.isDead()) {
                            source.getServer().execute(() ->
                                source.sendSuccess(() -> MessageUtil.get("status-other-dead", PLAYER, data.getUsername()), false));
                        } else {
                            source.getServer().execute(() ->
                                source.sendSuccess(() -> MessageUtil.get("status-other-alive",
                                        PLAYER, data.getUsername(),
                                        LIVES, data.getLives()), false));
                        }
                    });
                    return 1;
                })
            )
        );
    }

    private static void registerReviveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revive")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                context.getSource().sendFailure(MessageUtil.get("usage-revive")
                    .copy().withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/revive "))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, MessageUtil.get("click-to-autofill").copy().withStyle(net.minecraft.ChatFormatting.GRAY)))));
                return 0;
            })
            .then(Commands.argument(PLAYER, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerNames(), builder))
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, PLAYER);
                    CommandSourceStack source = context.getSource();

                    if (PermissionUtil.isBlockedByLimboOpSecurity(source)) {
                        PermissionUtil.sendSecurityBlockMessage(source);
                        return 0;
                    }

                    CompletableFuture.runAsync(() -> executeRevive(targetName, source));
                    return 1;
                })
            )
        );
    }

    private static void executeRevive(String targetName, CommandSourceStack source) {
        PlayerData targetData = db.getPlayerByName(targetName);
        if (targetData == null) {
            source.getServer().execute(() ->
                source.sendFailure(MessageUtil.get("revive-not-found", PLAYER, targetName)));
            return;
        }

        if (!targetData.isDead()) {
            source.getServer().execute(() ->
                source.sendFailure(MessageUtil.get("revive-already-alive", PLAYER, targetData.getUsername())));
            return;
        }

        int defaultLives = org.ssoggy.ssoggysouls.util.ConfigManager.getConfig().getDefaultLives();
        boolean success = db.revivePlayer(targetData.getUuid(), defaultLives);
        if (success) {
            handleReviveSuccess(targetData, source);
        }
    }

    private static void handleReviveSuccess(PlayerData targetData, CommandSourceStack source) {
        source.getServer().execute(() -> {
            DlcDeaths.clearDeath(targetData.getUuid());
            GhostModeEvents.updateGhostStatus(targetData.getUuid(), false);
            GhostState ghostState = GhostState.getServerState(source.getServer());
            ghostState.removeDeathLocation(targetData.getUuid());
            ghostState.removeDeathHolder(targetData.getUuid());
            ghostState.setDirty();
            org.ssoggy.ssoggysouls.hrm.HeadDropListener.removeDroppedHeads(targetData.getUuid(), source.getServer());
            source.sendSuccess(() -> MessageUtil.get("admin-revive-success", PLAYER, targetData.getUsername()), true);
            AdminLogger.log(source.getTextName(), "Revived " + targetData.getUsername());

            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(targetData.getUuid());
            if (targetPlayer != null) {
                targetPlayer.setGameMode(GameType.SURVIVAL);
                ServerLifecycleListener.setGhostModeAttributes(targetPlayer, false);
                targetPlayer.sendSystemMessage(MessageUtil.get("revive-success"));
            }
        });
    }

    private static void registerSetLivesCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("psetlives")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                context.getSource().sendFailure(MessageUtil.get("usage-psetlives")
                    .copy().withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/psetlives "))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, MessageUtil.get("click-to-autofill").copy().withStyle(net.minecraft.ChatFormatting.GRAY)))));
                return 0;
            })
            .then(Commands.argument(PLAYER, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerNames(), builder))
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, PLAYER);
                    context.getSource().sendFailure(MessageUtil.get("usage-psetlives-player", PLAYER, targetName)
                        .copy().withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED)
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/psetlives " + targetName + " "))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, MessageUtil.get("click-to-autofill").copy().withStyle(net.minecraft.ChatFormatting.GRAY)))));
                    return 0;
                })
                .then(Commands.argument(LIVES, IntegerArgumentType.integer(0))
                    .executes(context -> {
                        String targetName = StringArgumentType.getString(context, PLAYER);
                        int lives = IntegerArgumentType.getInteger(context, LIVES);
                        CommandSourceStack source = context.getSource();

                        if (PermissionUtil.isBlockedByLimboOpSecurity(source)) {
                            PermissionUtil.sendSecurityBlockMessage(source);
                            return 0;
                        }

                        CompletableFuture.runAsync(() -> {
                            PlayerData data = db.getPlayerByName(targetName);
                            if (data == null) {
                                source.getServer().execute(() ->
                                    source.sendFailure(MessageUtil.get("status-not-found", PLAYER, targetName)));
                                return;
                            }
                            db.setLives(data.getUuid(), lives);
                            source.getServer().execute(() -> {
                                ServerPlayer online = source.getServer().getPlayerList().getPlayer(data.getUuid());
                                if (lives > 0) {
                                    DlcDeaths.clearDeath(data.getUuid());
                                    GhostModeEvents.updateGhostStatus(data.getUuid(), false);
                                    GhostState ghostState = GhostState.getServerState(source.getServer());
                                    ghostState.removeDeathLocation(data.getUuid());
                                    ghostState.removeDeathHolder(data.getUuid());
                                    ghostState.setDirty();
                                    org.ssoggy.ssoggysouls.hrm.HeadDropListener.removeDroppedHeads(data.getUuid(), source.getServer());
                                    if (online != null) {
                                        ServerLifecycleListener.setGhostModeAttributes(online, false);
                                        online.setGameMode(GameType.SURVIVAL);
                                    }
                                }
                                source.sendSuccess(() -> MessageUtil.get("admin-setlives-success",
                                        PLAYER, data.getUsername(), LIVES, lives), true);
                                AdminLogger.log(source.getTextName(), "Set lives for " + data.getUsername() + " to " + lives);
                            });
                        });
                        return 1;
                    })
                )
            )
        );
    }



    private static void registerAdminLogCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adminlog")
            .requires(source -> source.hasPermission(3))
            .executes(context -> {
                CommandSourceStack source = context.getSource();

                CompletableFuture.runAsync(() -> {
                    File logFile = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(SSoggySoulsMod.MODID).resolve(AdminLogger.LOG_FILE_NAME).toFile();
                    org.ssoggy.ssoggysouls.command.action.AdminLogAction.AdminLogResult result =
                        org.ssoggy.ssoggysouls.command.action.AdminLogAction.execute(logFile, 15);

                    source.getServer().execute(() -> {
                        switch (result.type) {
                            case FILE_NOT_FOUND -> source.sendFailure(Component.literal("No admin logs found."));
                            case READ_ERROR -> {
                                SSoggySoulsMod.LOGGER.error("Error reading admin log");
                                source.sendFailure(MessageUtil.get("admin-log-read-error"));
                            }
                            case SUCCESS -> {
                                source.sendSystemMessage(Component.literal("--- Recent Admin Logs ---").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
                                if (source.isPlayer()) {
                                    for (String line : result.lines) {
                                        source.sendSystemMessage(Component.literal(line).withStyle(s ->
                                            s.withColor(net.minecraft.ChatFormatting.GRAY)
                                             .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, line))
                                             .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy log entry").withStyle(net.minecraft.ChatFormatting.GRAY)))
                                        ));
                                    }
                                } else {
                                    for (String line : result.lines) {
                                        source.sendSystemMessage(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                                    }
                                }
                            }
                        }
                    });
                });

                return 1;
            })
        );
    }

}
