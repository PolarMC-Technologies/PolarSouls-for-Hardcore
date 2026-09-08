package org.ssoggy.ssoggysouls.hrm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.ssoggy.ssoggysouls.SSoggySoulsMod;
import org.ssoggy.ssoggysouls.database.DatabaseManager;
import org.ssoggy.ssoggysouls.hrm.dlc.listener.GhostModeEvents;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcDeaths;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStat;
import org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcStats;
import org.ssoggy.ssoggysouls.hrm.dlc.util.GhostState;
import org.ssoggy.ssoggysouls.listener.ServerLifecycleListener;
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
    private static DatabaseManager db;

    private RevivalStructureListener() {
        // Utility class
    }

    public static void register(DatabaseManager database) {
        db = database;
    }

    public static GlobalPos consumePendingRevival(UUID uuid) {
        return PENDING_REVIVALS.remove(uuid);
    }

    @SubscribeEvent
    public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer serverPlayer = org.ssoggy.ssoggysouls.util.HrmUtil.getValidServerPlayer(event, db);
        if (serverPlayer == null) {
            return;
        }

        Level world = event.getLevel();
        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.PLAYER_HEAD)) {
            return;
        }

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) {
            return;
        }

        UUID ownerUuid = profile.id().get();
        BlockPos placedPos = event.getPos().relative(event.getFace());

        if (!isRitualStructure(world, placedPos)) {
            if (checkIncompleteStructure(world, placedPos)) {
                serverPlayer.sendSystemMessage(Component.literal(MessageUtil.getRawString("revival-structure-incomplete")).withStyle(net.minecraft.ChatFormatting.RED));
                world.playSound(null, placedPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 2f);
            }
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        final ItemStack finalStack = stack.copy();
        finalStack.setCount(1);
        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        triggerRevival(serverPlayer, world, placedPos, ownerUuid, db, finalStack);
    }

    private static void triggerRevival(ServerPlayer serverPlayer, Level world, BlockPos placedPos, UUID ownerUuid,
                                       DatabaseManager db, ItemStack refundedItem) {
        new DlcStats(serverPlayer.getUUID()).incrementStat(DlcStat.RITUAL_STARTED, 1);
        CompletableFuture.runAsync(() -> {
            boolean isDead = db.isPlayerDead(ownerUuid);
            String rawOwnerName = org.ssoggy.ssoggysouls.hrm.dlc.shared.DlcNames.getOrDefault(ownerUuid, "Player");
            String ownerName = rawOwnerName != null ? rawOwnerName : "Unknown";

            if (!isDead) {
                serverPlayer.server.execute(() -> {
                    sendError(serverPlayer, (ownerName != null ? ownerName : "Unknown") + " is not dead!");
                    world.playSound(null, placedPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 2f);
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

            new DlcStats(serverPlayer.getUUID()).incrementStat(DlcStat.RITUAL_COMPLETED, 1);
            new DlcStats(ownerUuid).incrementStat(DlcStat.REVIVES, 1);
            SSoggySoulsMod.LOGGER.info("{} revived {} via ritual structure!", serverPlayer.getScoreboardName(), ownerName);

            serverPlayer.server.execute(() -> performRevival(world, placedPos, serverPlayer, ownerUuid, ownerName));
        });
    }

    private static void refundHead(ServerPlayer serverPlayer, ItemStack head) {
        if (!serverPlayer.isCreative() && !serverPlayer.getInventory().add(head)) {
            serverPlayer.drop(head, false);
        }
    }

    private static void performRevival(Level world, BlockPos placedPos, ServerPlayer summoner, UUID revivedUuid, String revivedName) {
        breakStructure(world, placedPos);
        DlcDeaths.clearDeath(revivedUuid);
        GhostModeEvents.updateGhostStatus(revivedUuid, false);
        GhostState ghostState = GhostState.getServerState(world.getServer());
        ghostState.removeDeathLocation(revivedUuid);
        ghostState.removeDeathHolder(revivedUuid);
        ghostState.setDirty();

        HeadDropListener.removeDroppedHeads(revivedUuid, world.getServer());


        summoner.sendSystemMessage(MessageUtil.get("admin-revive-success", "player", revivedName, "lives", ConfigManager.getConfig().getOnReviveLives()));
        summoner.sendSystemMessage(MessageUtil.get("revive-from-limbo", "player", revivedName));

        world.players().forEach(p -> {
            if (!p.getUUID().equals(summoner.getUUID())) {
                p.sendSystemMessage(Component.literal("§e" + summoner.getScoreboardName() + " revived " + revivedName + "!"));
            }
        });

        if (ConfigManager.getConfig().isRitualLightningStrike()) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveTo(placedPos.getCenter());
                lightning.setVisualOnly(true);
                world.addFreshEntity(lightning);
            }
        }

        ServerPlayer revivedPlayer = world.getServer().getPlayerList().getPlayer(revivedUuid);
        if (revivedPlayer != null) {
            restoreAtStructure(revivedPlayer, (ServerLevel) world, placedPos);
        } else {
            PENDING_REVIVALS.put(revivedUuid, GlobalPos.of(world.dimension(), placedPos));
            SSoggySoulsMod.LOGGER.info("{} is offline; revival effects will be applied on next login.", revivedName);
        }
    }

    public static void restoreAtStructure(ServerPlayer revived, ServerLevel world, BlockPos spawnPos) {
        revived.teleportTo(world, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        revived.setGameMode(GameType.SURVIVAL);
        ServerLifecycleListener.setGhostModeAttributes(revived, false);
        revived.sendSystemMessage(MessageUtil.get("revive-success"));

        revived.removeAllEffects();
        int resistanceTicks = ConfigManager.getConfig().getReviveResistanceTicks();
        if (resistanceTicks > 0) {
            revived.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceTicks, 4, false, true));
        }
        int glowingTicks = ConfigManager.getConfig().getReviveGlowingTicks();
        if (glowingTicks > 0) {
            revived.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowingTicks, 0, false, true));
        }

        if (ConfigManager.getConfig().isRitualTotemEffect()) {
            revived.level().playSound(null, revived.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            world.broadcastEntityEvent(revived, EntityEvent.TALISMAN_ACTIVATE);
        }
    }

    private static void breakStructure(Level world, BlockPos headPos) {
        breakCorners(world, headPos);
        breakCenter(world, headPos);
        breakEdges(world, headPos);
        breakBase(world, headPos);
    }

    private static void breakCorners(Level world, BlockPos headPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    BlockPos soulSandPos = headPos.offset(x, -2, z);
                    BlockPos flowerPos = headPos.offset(x, -1, z);

                    if (isFlower(world, flowerPos.getX(), flowerPos.getY(), flowerPos.getZ())) {
                        world.destroyBlock(flowerPos, true);
                    }
                    if (isSoulSand(world, soulSandPos.getX(), soulSandPos.getY(), soulSandPos.getZ())) {
                        world.destroyBlock(soulSandPos, true);
                    }
                }
            }
        }
    }

    private static void breakCenter(Level world, BlockPos headPos) {
        BlockPos center = headPos.below();
        if (isFence(world, center.getX(), center.getY(), center.getZ())) {
            world.destroyBlock(center, true);
        }
    }

    private static void breakEdges(Level world, BlockPos headPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((Math.abs(x) == 1 && z == 0) || (x == 0 && Math.abs(z) == 1)) {
                    BlockPos stairPos = headPos.offset(x, -2, z);
                    if (isStair(world, stairPos.getX(), stairPos.getY(), stairPos.getZ())) {
                        world.destroyBlock(stairPos, true);
                    }
                }
            }
        }
    }

    private static void breakBase(Level world, BlockPos headPos) {
        if (!ConfigManager.getConfig().isLeaveStructureBase()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos basePos = headPos.offset(x, -2, z);
                    world.destroyBlock(basePos, true);
                }
            }
        }
    }

    private static void sendError(ServerPlayer player, String msg) {
        player.sendSystemMessage(Component.literal("§c" + msg));
    }

    private static boolean isRitualStructure(Level world, BlockPos headPos) {
        int hx = headPos.getX();
        int hy = headPos.getY();
        int hz = headPos.getZ();

        if (!isFence(world, hx, hy - 1, hz)) return false;
        if (!isOre(world, hx, hy - 2, hz)) return false;

        int by = hy - 2;
        if (!isSoulSand(world, hx - 1, by, hz - 1)) return false;
        if (!isSoulSand(world, hx + 1, by, hz - 1)) return false;
        if (!isSoulSand(world, hx - 1, by, hz + 1)) return false;
        if (!isSoulSand(world, hx + 1, by, hz + 1)) return false;

        if (!isStair(world, hx, by, hz - 1)) return false;
        if (!isStair(world, hx - 1, by, hz)) return false;
        if (!isStair(world, hx + 1, by, hz)) return false;
        if (!isStair(world, hx, by, hz + 1)) return false;

        int my = hy - 1;
        if (!isFlower(world, hx - 1, my, hz - 1)) return false;
        if (!isFlower(world, hx + 1, my, hz - 1)) return false;
        if (!isFlower(world, hx - 1, my, hz + 1)) return false;
        return isFlower(world, hx + 1, my, hz + 1);
    }

    private static boolean checkIncompleteStructure(Level world, BlockPos headPos) {
        int hx = headPos.getX();
        int hy = headPos.getY();
        int hz = headPos.getZ();

        int score = 0;
        if (isFence(world, hx, hy - 1, hz)) score += 3;
        if (isOre(world, hx, hy - 2, hz)) score += 2;

        for (int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                if (isOre(world, hx + i, hy - 2, hz + k) || isSoulSand(world, hx + i, hy - 2, hz + k)
                        || isStair(world, hx + i, hy - 2, hz + k))
                    score++;
            }
        }
        return score >= 6;
    }

    private static boolean isBlockInTagList(BlockState state, java.util.List<String> tags) {
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String name = id.getPath().toUpperCase(java.util.Locale.ROOT);
        return tags.contains(name);
    }

    private static boolean isOre(Level world, int x, int y, int z) {
        return isBlockInTagList(world.getBlockState(new BlockPos(x, y, z)), ConfigManager.getConfig().getOreBlocktag());
    }

    private static boolean isFlower(Level world, int x, int y, int z) {
        return isBlockInTagList(world.getBlockState(new BlockPos(x, y, z)), ConfigManager.getConfig().getFlowerBlocktag());
    }

    private static boolean isFence(Level world, int x, int y, int z) {
        return isBlockInTagList(world.getBlockState(new BlockPos(x, y, z)), ConfigManager.getConfig().getFenceBlocktag());
    }

    private static boolean isSoulSand(Level world, int x, int y, int z) {
        return isBlockInTagList(world.getBlockState(new BlockPos(x, y, z)), ConfigManager.getConfig().getSoulSandBlocktag());
    }

    private static boolean isStair(Level world, int x, int y, int z) {
        return isBlockInTagList(world.getBlockState(new BlockPos(x, y, z)), ConfigManager.getConfig().getStairBlocktag());
    }
}
