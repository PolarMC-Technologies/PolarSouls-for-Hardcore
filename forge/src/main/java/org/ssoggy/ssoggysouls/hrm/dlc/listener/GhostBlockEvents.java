package org.ssoggy.ssoggysouls.hrm.dlc.listener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.ssoggy.ssoggysouls.SSoggySoulsMod;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcNames;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.listener.ServerLifecycleListener;
import org.ssoggy.ssoggysouls.model.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
public class GhostBlockEvents {

    private static DatabaseManager db;

    private GhostBlockEvents() {}

    public static void register(DatabaseManager database) {
        db = database;
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!org.ssoggy.ssoggysouls.util.ConfigManager.getConfig().isHrmEnabled()) return;

        if (db == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem().getItem();
        if (!stack.is(Items.PLAYER_HEAD)) {
            return;
        }

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) return;
        UUID ownerUuid = profile.id().get();

        UUID holderUuid = player.getUUID();
        String holderName = player.getScoreboardName();
        CompletableFuture.runAsync(() -> {
            DlcDeaths.setHolder(ownerUuid, holderUuid);
            DlcNames.cache(holderUuid, holderName);
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!org.ssoggy.ssoggysouls.util.ConfigManager.getConfig().isHrmEnabled()) return;

        if (db == null || event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Level world = (Level) event.getLevel();
        BlockState state = event.getState();
        
        if ((state.is(Blocks.PLAYER_HEAD) || state.is(Blocks.PLAYER_WALL_HEAD))) {
            BlockEntity be = world.getBlockEntity(event.getPos());
            if (be instanceof SkullBlockEntity skull) {
                handleHeadBreak(world, player, skull);
            }
        }
    }

    private static void handleHeadBreak(Level world, ServerPlayer player, SkullBlockEntity skull) {
        ResolvableProfile profile = skull.getOwnerProfile();
        if (profile != null && profile.id().isPresent()) {
            UUID ownerUuid = profile.id().get();
            
            CompletableFuture.runAsync(() -> {
                PlayerData data = db.getPlayer(ownerUuid);
                if (data != null && data.isDead()) {
                    world.getServer().execute(() -> {
                        GhostState ghostState = GhostState.getServerState(world.getServer());

                        ghostState.removeDeathLocation(ownerUuid);
                        ghostState.setDeathHolder(ownerUuid, player.getUUID());
                        ghostState.setDirty();
                        DlcDeaths.setHolder(ownerUuid, player.getUUID());
                        DlcNames.cache(player.getUUID(), player.getScoreboardName());

                        ServerPlayer ghost = world.getServer().getPlayerList().getPlayer(ownerUuid);
                        if (ghost != null) {
                            ghost.setGameMode(GameType.SPECTATOR);
                            ghost.setCamera(player);
                            ghost.sendSystemMessage(Component.literal("Started spectating " + player.getScoreboardName()).withStyle(net.minecraft.ChatFormatting.GRAY));
                            ghost.sendSystemMessage(Component.literal(player.getScoreboardName() + " is currently carrying your playerhead...").withStyle(net.minecraft.ChatFormatting.YELLOW));
                        }
                    });
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = org.ssoggy.ssoggysouls.util.HrmUtil.getValidServerPlayer(event, db);
        if (player == null) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.PLAYER_HEAD)) return;

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) return;
        UUID ownerUuid = profile.id().get();
        BlockPos targetPos = event.getPos().relative(event.getFace());

        event.getLevel().getServer().execute(() -> handleHeadPlace(event.getLevel(), ownerUuid, targetPos));
    }

    private static void handleHeadPlace(Level world, UUID ownerUuid, BlockPos targetPos) {
        BlockState state = world.getBlockState(targetPos);
        if (state.is(Blocks.PLAYER_HEAD) || state.is(Blocks.PLAYER_WALL_HEAD)) {
            BlockEntity be = world.getBlockEntity(targetPos);
            if (be instanceof SkullBlockEntity skull) {
                ResolvableProfile profile = skull.getOwnerProfile();
                if (profile != null && profile.id().isPresent() && profile.id().get().equals(ownerUuid)) {
                    updateGhostStateOnPlace(world, ownerUuid, targetPos);
                }
            }
        }
    }

    private static void updateGhostStateOnPlace(Level world, UUID ownerUuid, BlockPos targetPos) {
        GhostState ghostState = GhostState.getServerState(world.getServer());

        ghostState.removeDeathHolder(ownerUuid);
        ghostState.setDeathLocation(ownerUuid, targetPos);
        ghostState.setDirty();
        DlcDeaths.setHolder(ownerUuid, null);
        DlcDeaths.recordDeath(
                ownerUuid,
                DlcNames.getOrDefault(ownerUuid, ownerUuid.toString()),
                world.dimension().location().toString(),
                targetPos.getX(),
                targetPos.getY(),
                targetPos.getZ()
        );

        CompletableFuture.runAsync(() -> {
            PlayerData data = db.getPlayer(ownerUuid);
            if (data != null && data.isDead()) {
                world.getServer().execute(() -> {
                    ServerPlayer ghost = world.getServer().getPlayerList().getPlayer(ownerUuid);
                    if (ghost != null && ghost.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                        ghost.setGameMode(GameType.ADVENTURE);
                        ServerLifecycleListener.setGhostModeAttributes(ghost, true);

                        ghost.teleportTo(ghost.serverLevel(), targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, ghost.getYRot(), ghost.getXRot());
                        ghost.sendSystemMessage(Component.literal("Your head has been placed down.").withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                });
            }
        });
    }
}
