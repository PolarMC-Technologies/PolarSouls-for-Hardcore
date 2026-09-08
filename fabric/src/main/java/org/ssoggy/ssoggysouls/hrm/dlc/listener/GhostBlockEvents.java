package org.ssoggy.ssoggysouls.hrm.dlc.listener;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcNames;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.model.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GhostBlockEvents {

    private GhostBlockEvents() {
        // Utility class
    }

    public static void register(DatabaseManager db) {
        registerHeadBreak(db);
        registerHeadPlace(db);
        registerInventoryHeadTracker();
    }

    private static void registerHeadBreak(DatabaseManager db) {
        // Detect when a player breaks a player's head block
        PlayerBlockBreakEvents.AFTER.register((world, player, ignoredPos, state, blockEntity) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return;

            if ((state.isOf(Blocks.PLAYER_HEAD) || state.isOf(Blocks.PLAYER_WALL_HEAD))
                    && blockEntity instanceof SkullBlockEntity skull) {
                handleHeadBreak(world, serverPlayer, skull, db);
            }
        });
    }

    private static void handleHeadBreak(net.minecraft.world.World world, ServerPlayerEntity serverPlayer,
            SkullBlockEntity skull, DatabaseManager db) {
        ProfileComponent profile = skull.getOwner();
        if (profile != null) {
            profile.id().ifPresent(ownerUuid -> CompletableFuture.runAsync(() -> {
                PlayerData data = db.getPlayer(ownerUuid);
                if (data != null && data.isDead()) {
                    world.getServer().execute(() -> {
                        GhostState ghostState = GhostState.getServerState(world.getServer());

                        // The owner is a ghost! The breaker becomes the "Death Holder"
                        ghostState.deathLocations.remove(ownerUuid);
                        ghostState.deathHolders.put(ownerUuid, serverPlayer.getUuid());
                        ghostState.markDirty();
                        DlcDeaths.setHolder(ownerUuid, serverPlayer.getUuid());
                        DlcNames.cache(serverPlayer.getUuid(), serverPlayer.getName().getString());

                        ServerPlayerEntity ghost = world.getServer().getPlayerManager().getPlayer(ownerUuid);
                        if (ghost != null) {
                            // Put ghost into spectator mode to follow the holder
                            ghost.changeGameMode(GameMode.SPECTATOR);
                            ghost.setCameraEntity(serverPlayer);
                            ghost.sendMessage(
                                    Text.literal("Started spectating " + serverPlayer.getName().getString())
                                            .styled(s -> s.withColor(Formatting.GRAY)),
                                    false);
                            ghost.sendMessage(Text
                                    .literal(serverPlayer.getName().getString()
                                            + " is currently carrying your playerhead...")
                                    .styled(s -> s.withColor(Formatting.YELLOW)), true);
                        }
                    });
                }
            }));
        }
    }

    private static void registerHeadPlace(DatabaseManager db) {
        // Detect when a player places a player's head block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity))
                return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.PLAYER_HEAD))
                return ActionResult.PASS;

            ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
            if (profile == null || profile.id().isEmpty())
                return ActionResult.PASS;

            UUID ownerUuid = profile.id().get();
            BlockPos targetPos = hitResult.getBlockPos().offset(hitResult.getSide());

            // Schedule a check next tick to see if it was successfully placed
            world.getServer().execute(() -> handleHeadPlace(world, ownerUuid, targetPos, db));

            return ActionResult.PASS;
        });
    }

    private static void registerInventoryHeadTracker() {
        final int[] tickCounter = {0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter[0] = (tickCounter[0] + 1) % 20;
            if (tickCounter[0] != 0) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayerInventoryForHeads(player);
            }
        });
    }

    private static void checkPlayerInventoryForHeads(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.PLAYER_HEAD)) {
                ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
                if (profile != null && profile.id().isPresent()) {
                    java.util.UUID ownerUuid = profile.id().get();
                    java.util.UUID playerUuid = player.getUuid();
                    String playerName = player.getName().getString();
                    CompletableFuture.runAsync(() -> {
                        DlcDeaths.setHolder(ownerUuid, playerUuid);
                        DlcNames.cache(playerUuid, playerName);
                    });
                }
            }
        }
    }

    private static void handleHeadPlace(net.minecraft.world.World world, UUID ownerUuid, BlockPos targetPos,
            DatabaseManager db) {
        BlockState state = world.getBlockState(targetPos);
        if (state.isOf(Blocks.PLAYER_HEAD) || state.isOf(Blocks.PLAYER_WALL_HEAD)) {
            BlockEntity be = world.getBlockEntity(targetPos);
            if (be instanceof SkullBlockEntity skull) {
                ProfileComponent profile = skull.getOwner();
                if (profile != null) {
                    profile.id().ifPresent(id -> {
                        if (id.equals(ownerUuid)) {
                            updateGhostStateOnPlace(world, ownerUuid, targetPos, db);
                        }
                    });
                }
            }
        }
    }

    private static void updateGhostStateOnPlace(net.minecraft.world.World world, UUID ownerUuid, BlockPos targetPos,
            DatabaseManager db) {
        GhostState ghostState = GhostState.getServerState(world.getServer());

        // Block was placed! Update death location and remove holder
        ghostState.deathHolders.remove(ownerUuid);
        ghostState.deathLocations.put(ownerUuid, targetPos);
        ghostState.markDirty();
        DlcDeaths.setHolder(ownerUuid, null);
        DlcDeaths.recordDeath(
                ownerUuid,
                DlcNames.getOrDefault(ownerUuid, ownerUuid.toString()),
                world.getRegistryKey().getValue().toString(),
                targetPos.getX(),
                targetPos.getY(),
                targetPos.getZ()
        );

        CompletableFuture.runAsync(() -> {
            PlayerData data = db.getPlayer(ownerUuid);
            if (data != null && data.isDead()) {
                world.getServer().execute(() -> {
                    ServerPlayerEntity ghost = world.getServer().getPlayerManager().getPlayer(ownerUuid);
                    if (ghost != null && ghost.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                        // Remove from spectator and put back into Ghost Mode restrictions
                        ghost.changeGameMode(GameMode.ADVENTURE);
                        org.ssoggy.ssoggysouls.listener.MainServerListener.setGhostModeAttributes(ghost, true);

                        // Teleport to the newly placed head
                        ghost.teleport(ghost.getServerWorld(), targetPos.getX() + 0.5, targetPos.getY(),
                                targetPos.getZ() + 0.5, ghost.getYaw(), ghost.getPitch());
                        ghost.sendMessage(Text.literal("Your head has been placed down.")
                                .styled(s -> s.withColor(Formatting.GRAY)), true);
                    }
                });
            }
        });
    }
}
