package org.ssoggy.ssoggysouls.hrm;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.ssoggy.ssoggysouls.SSoggySoulsMod;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.listener.GhostModeEvents;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStat;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStats;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.listener.MainServerListener;
import org.ssoggy.ssoggysouls.model.PlayerData;
import org.ssoggy.ssoggysouls.util.ConfigManager;
import org.ssoggy.ssoggysouls.util.MessageUtil;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RevivalStructureListener {

    private static final Map<UUID, GlobalPos> PENDING_REVIVALS = new ConcurrentHashMap<>();

    private RevivalStructureListener() {
        // Utility class
    }

    /**
     * Removes and returns the pending revival location (dimension + position) for
     * the given player, or {@code null} if no pending revival exists. Called on player join.
     */
    public static GlobalPos consumePendingRevival(UUID uuid) {
        return PENDING_REVIVALS.remove(uuid);
    }

    public static void register(DatabaseManager db) {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            return handleStructureInteraction(serverPlayer, world, hand, hitResult, db);
        });
    }

    private static ActionResult handleStructureInteraction(ServerPlayerEntity serverPlayer, World world, Hand hand,
            net.minecraft.util.hit.BlockHitResult hitResult, DatabaseManager db) {
        if (serverPlayer.isSpectator()) {
            return ActionResult.PASS;
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        if (!stack.isOf(Items.PLAYER_HEAD)) {
            return ActionResult.PASS;
        }

        if (db.isPlayerDead(serverPlayer.getUuid())) {
            return ActionResult.PASS;
        }

        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) {
            return ActionResult.PASS;
        }
        Optional<UUID> ownerId = profile.id();
        if (ownerId.isEmpty()) {
            return ActionResult.PASS;
        }

        UUID ownerUuid = ownerId.get();
        BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());

        // Check if ritual structure is valid underneath where the head is about to be
        // placed
        if (!isRitualStructure(world, placedPos)) {
            if (checkIncompleteStructure(world, placedPos)) {
                serverPlayer.sendMessage(MessageUtil.get("revival-structure-incomplete"), false);
                world.playSound(null, placedPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 2f);
            }
            return ActionResult.PASS; // Let them place the head normally if not doing ritual
        }

        // Copy the stack, consume it immediately to prevent race conditions.
        // It will be given back if the revival fails.
        final ItemStack finalStack = stack.copy();
        finalStack.setCount(1);
        if (!serverPlayer.isCreative()) {
            stack.decrement(1);
        }

        // Trigger async DB check
        triggerRevival(serverPlayer, world, placedPos, ownerUuid, db, finalStack);

        // Return SUCCESS to indicate we handled the interaction (preventing standard
        // block placement)
        return ActionResult.SUCCESS;
    }

    private static void triggerRevival(ServerPlayerEntity serverPlayer, World world, BlockPos placedPos, UUID ownerUuid,
            DatabaseManager db, ItemStack refundedItem) {
        new DlcStats(serverPlayer.getUuid()).incrementStat(DlcStat.RITUAL_STARTED, 1);
        CompletableFuture.runAsync(() -> {
            PlayerData data = db.getPlayer(ownerUuid);
            if (data == null) {
                serverPlayer.server.execute(() -> {
                    sendError(serverPlayer, "Unknown player.");
                    refundHead(serverPlayer, refundedItem);
                });
                return;
            }

            if (!data.isDead()) {
                serverPlayer.server.execute(() -> {
                    sendError(serverPlayer, (data.getUsername() != null ? data.getUsername() : "Unknown") + " is not dead!");
                    world.playSound(null, placedPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 2f);
                    refundHead(serverPlayer, refundedItem);
                });
                return;
            }

            boolean success = db.revivePlayer(ownerUuid, ConfigManager.getConfig().getOnReviveLives());
            if (!success) {
                serverPlayer.server.execute(() -> {
                    sendError(serverPlayer, "Failed to revive. Check console.");
                    refundHead(serverPlayer, refundedItem);
                });
                return;
            }

            new DlcStats(serverPlayer.getUuid()).incrementStat(DlcStat.RITUAL_COMPLETED, 1);
            new DlcStats(ownerUuid).incrementStat(DlcStat.REVIVES, 1);
            SSoggySoulsMod.LOGGER.info("{} revived {} via ritual structure!", serverPlayer.getName().getString(),
                    data.getUsername());

            serverPlayer.server.execute(() -> performRevival(world, placedPos, serverPlayer, ownerUuid, data.getUsername()));
        });
    }

    private static void refundHead(ServerPlayerEntity serverPlayer, ItemStack head) {
        if (!serverPlayer.isCreative() && !serverPlayer.getInventory().insertStack(head)) {
            serverPlayer.dropItem(head, false);
        }
    }

    private static void performRevival(World world, BlockPos placedPos, ServerPlayerEntity summoner, UUID revivedUuid,
            String revivedName) {
        breakStructure(world, placedPos);
        DlcDeaths.clearDeath(revivedUuid);
        GhostModeEvents.updateGhostStatus(revivedUuid, false);
        GhostState ghostState = GhostState.getServerState(world.getServer());
        ghostState.deathLocations.remove(revivedUuid);
        ghostState.deathHolders.remove(revivedUuid);
        ghostState.markDirty();

        HeadDropListener.removeDroppedHeads(revivedUuid, world.getServer());

        // Notify summoner
        summoner.sendMessage(MessageUtil.get("admin-revive-success", "player", revivedName, "lives",
                ConfigManager.getConfig().getOnReviveLives()), false);
        summoner.sendMessage(MessageUtil.get("revive-from-limbo", "player", revivedName), false);

        // Notify server
        world.getPlayers().forEach(p -> {
            if (!p.getUuid().equals(summoner.getUuid())) {
                p.sendMessage(
                        MessageUtil.colorizeText("§e" + summoner.getName().getString() + " revived " + revivedName + "!"),
                        false);
            }
        });

        if (ConfigManager.getConfig().isRitualLightningStrike()) {
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(placedPos.toCenterPos());
                lightning.setCosmetic(true);
                world.spawnEntity(lightning);
            }
        }

        // Apply effects directly to the revived player if they are online,
        // otherwise queue the revival for when they next log in
        ServerPlayerEntity revivedPlayer = world.getServer().getPlayerManager().getPlayer(revivedUuid);
        if (revivedPlayer != null) {
            restoreAtStructure(revivedPlayer, (ServerWorld) world, placedPos);
        } else {
            PENDING_REVIVALS.put(revivedUuid, GlobalPos.create(world.getRegistryKey(), placedPos));
            SSoggySoulsMod.LOGGER.info("{} is offline; revival effects will be applied on next login.", revivedName);
        }
    }

    public static void restoreAtStructure(ServerPlayerEntity revived, ServerWorld world, BlockPos spawnPos) {
        revived.teleport(world, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        revived.changeGameMode(GameMode.SURVIVAL);
        MainServerListener.setGhostModeAttributes(revived, false);
        revived.sendMessage(MessageUtil.get("revive-success"), false);

        revived.clearStatusEffects();
        int resistanceTicks = ConfigManager.getConfig().getReviveResistanceTicks();
        if (resistanceTicks > 0) {
            revived.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistanceTicks, 4, false, true));
        }
        int glowingTicks = ConfigManager.getConfig().getReviveGlowingTicks();
        if (glowingTicks > 0) {
            revived.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowingTicks, 0, false, true));
        }

        // Totem effect
        if (ConfigManager.getConfig().isRitualTotemEffect()) {
            revived.getWorld().playSound(null, revived.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS,
                    1.0f, 1.0f);
            revived.getWorld().sendEntityStatus(revived, EntityStatuses.USE_TOTEM_OF_UNDYING);
        }
    }

    private static void breakStructure(World world, BlockPos headPos) {
        breakCorners(world, headPos);
        breakCenter(world, headPos);
        breakEdges(world, headPos);
        breakBase(world, headPos);
    }

    private static void breakCorners(World world, BlockPos headPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    BlockPos soulSandPos = headPos.add(x, -2, z);
                    BlockPos flowerPos = headPos.add(x, -1, z);

                    if (isFlower(world, flowerPos.getX(), flowerPos.getY(), flowerPos.getZ())) {
                        world.breakBlock(flowerPos, true);
                    }
                    if (isSoulSand(world, soulSandPos.getX(), soulSandPos.getY(), soulSandPos.getZ())) {
                        world.breakBlock(soulSandPos, true);
                    }
                }
            }
        }
    }

    private static void breakCenter(World world, BlockPos headPos) {
        BlockPos center = headPos.down();
        if (isFence(world, center.getX(), center.getY(), center.getZ())) {
            world.breakBlock(center, true);
        }
    }

    private static void breakEdges(World world, BlockPos headPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((Math.abs(x) == 1 && z == 0) || (x == 0 && Math.abs(z) == 1)) {
                    BlockPos stairPos = headPos.add(x, -2, z);
                    if (isStair(world, stairPos.getX(), stairPos.getY(), stairPos.getZ())) {
                        world.breakBlock(stairPos, true);
                    }
                }
            }
        }
    }

    private static void breakBase(World world, BlockPos headPos) {
        if (!ConfigManager.getConfig().isLeaveStructureBase()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos basePos = headPos.add(x, -2, z);
                    world.breakBlock(basePos, true);
                }
            }
        }
    }

    private static void sendError(ServerPlayerEntity player, String msg) {
        player.sendMessage(MessageUtil.colorizeText("§c" + msg), false);
    }

    private static boolean isRitualStructure(World world, BlockPos headPos) {
        int hx = headPos.getX();
        int hy = headPos.getY();
        int hz = headPos.getZ();

        // fence below da head
        if (!isFence(world, hx, hy - 1, hz))
            return false;

        // ore block below the fence
        if (!isOre(world, hx, hy - 2, hz))
            return false;

        int by = hy - 2;
        // soul sand corners
        if (!isSoulSand(world, hx - 1, by, hz - 1))
            return false;
        if (!isSoulSand(world, hx + 1, by, hz - 1))
            return false;
        if (!isSoulSand(world, hx - 1, by, hz + 1))
            return false;
        if (!isSoulSand(world, hx + 1, by, hz + 1))
            return false;

        // stair edges
        if (!isStair(world, hx, by, hz - 1))
            return false;
        if (!isStair(world, hx - 1, by, hz))
            return false;
        if (!isStair(world, hx + 1, by, hz))
            return false;
        if (!isStair(world, hx, by, hz + 1))
            return false;

        // wither roses on the corners on the soul sand
        int my = hy - 1;
        if (!isFlower(world, hx - 1, my, hz - 1))
            return false;
        if (!isFlower(world, hx + 1, my, hz - 1))
            return false;
        if (!isFlower(world, hx - 1, my, hz + 1))
            return false;
        return isFlower(world, hx + 1, my, hz + 1);
    }

    private static boolean checkIncompleteStructure(World world, BlockPos headPos) {
        int hx = headPos.getX();
        int hy = headPos.getY();
        int hz = headPos.getZ();

        // Just check if the base has at least some matching blocks
        int score = 0;
        if (isFence(world, hx, hy - 1, hz))
            score += 3;
        if (isOre(world, hx, hy - 2, hz))
            score += 2;

        for (int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                if (isOre(world, hx + i, hy - 2, hz + k) || isSoulSand(world, hx + i, hy - 2, hz + k)
                        || isStair(world, hx + i, hy - 2, hz + k))
                    score++;
            }
        }
        return score >= 6; // somewhat arbitrary threshold to decide if they tried to build the ritual
    }

    private static boolean isBlockInTagList(BlockState state, java.util.List<String> tags) {
        net.minecraft.util.Identifier id = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock());
        String name = id.getPath().toUpperCase(java.util.Locale.ROOT);
        return tags.contains(name);
    }

    private static boolean isOre(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return isBlockInTagList(state, ConfigManager.getConfig().getOreBlocktag());
    }

    private static boolean isFlower(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return isBlockInTagList(state, ConfigManager.getConfig().getFlowerBlocktag());
    }

    private static boolean isFence(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState fence = world.getBlockState(pos);
        return isBlockInTagList(fence, ConfigManager.getConfig().getFenceBlocktag());
    }

    private static boolean isSoulSand(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return isBlockInTagList(state, ConfigManager.getConfig().getSoulSandBlocktag());
    }

    private static boolean isStair(World world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return isBlockInTagList(state, ConfigManager.getConfig().getStairBlocktag());
    }
}
