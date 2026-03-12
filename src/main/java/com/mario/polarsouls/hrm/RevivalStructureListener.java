package com.mario.polarsouls.hrm;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.MessageUtil;

// detects when player head is placed for HRM strcuture (so cool to short it to HRM i know)
public class RevivalStructureListener implements Listener {

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public RevivalStructureListener(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isHrmStructureRevive()) return;

        Block placed = event.getBlockPlaced();
        Material type = placed.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) return;

        // get owner of placed head
        ItemStack item = event.getItemInHand();
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) return;

        OfflinePlayer skullOwner = skullMeta.getOwningPlayer();
        if (skullOwner == null) return;

        UUID ownerUuid = skullOwner.getUniqueId();
        Player placer = event.getPlayer();

        // check if structure is correct because idk it feels pretty essential
        if (!isRitualStructure(placed)) {
            checkIncompleteStructure(placed, placer);
            return;
        }

        // db checkkkkk
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.getPlayer(ownerUuid);

            if (data == null) {
                sendError(placer, "Unknown player.");
                return;
            }

            if (!data.isDead()) {
                sendError(placer, data.getUsername() + " is not dead!");
                playErrorEffect(placed);
                return;
            }

            boolean success = db.revivePlayer(ownerUuid, plugin.getLivesOnRevive());
            if (!success) {
                sendError(placer, "Failed to revive. Check console.");
                return;
            }

            plugin.getLogger().log(Level.INFO,
                    "{0} revived {1} via ritual structure!",
                    new Object[]{placer.getName(), data.getUsername()});

            // visual effects on main thread
            Bukkit.getScheduler().runTask(plugin, () ->
                    performRevival(placed, placer, ownerUuid, data.getUsername()));
        });
    }

    private void performRevival(Block headBlock, Player summoner, UUID revivedUuid, String revivedName) {
        Location spawnLoc = headBlock.getLocation().add(0.5, 0.05, 0.5);
        World world = headBlock.getWorld();

        breakStructure(headBlock);

        world.strikeLightningEffect(spawnLoc);

        summoner.sendMessage(MessageUtil.get("revive-admin-success",
                "player", revivedName, "lives", plugin.getLivesOnRevive()));

        Player revived = Bukkit.getPlayer(revivedUuid);
        if (revived != null && revived.isOnline()) {
            restoreAtStructure(revived, spawnLoc);
        } else {
            summoner.sendMessage(MessageUtil.get("revive-from-limbo",
                    "player", revivedName));
        }
    }

    private static void restoreAtStructure(Player revived, Location spawnLoc) {
        revived.teleport(spawnLoc);
        revived.setGameMode(GameMode.SURVIVAL);
        revived.sendMessage(MessageUtil.get("revive-success"));

        // effects after revive since HRM does it too and i have it built in so
        revived.getActivePotionEffects().forEach(e ->
                revived.removePotionEffect(e.getType()));

        int duration = 100; // 5 seconds (for people who dont uderstand ticks ig)
        revived.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, duration, 4, false, true));
        revived.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING, duration, 0, false, true));

        // attempt to do totem of undying animation (will prob oinly work if you are revived within the spectator time window but hey its worth a shot and its cool for that window anyway)
        revived.playEffect(EntityEffect.TOTEM_RESURRECT);
    }

    private boolean isRitualStructure(Block headBlock) {
        int hx = headBlock.getX();
        int hy = headBlock.getY();
        int hz = headBlock.getZ();
        World world = headBlock.getWorld();

        // fence below da head
        if (!Tag.FENCES.isTagged(world.getBlockAt(hx, hy - 1, hz).getType())) return false;

        // ore block below the fence
        if (!Tag.BEACON_BASE_BLOCKS.isTagged(world.getBlockAt(hx, hy - 2, hz).getType())) return false;

        int by = hy - 2;

        // soul sand corners
        if (!isSoulSand(world, hx - 1, by, hz - 1)) return false;
        if (!isSoulSand(world, hx + 1, by, hz - 1)) return false;
        if (!isSoulSand(world, hx - 1, by, hz + 1)) return false;
        if (!isSoulSand(world, hx + 1, by, hz + 1)) return false;

        // stair edges
        if (!isStair(world, hx, by, hz - 1)) return false;
        if (!isStair(world, hx - 1, by, hz)) return false;
        if (!isStair(world, hx + 1, by, hz)) return false;
        if (!isStair(world, hx, by, hz + 1)) return false;

        // wither roses on the corners on the soul sand
        int my = hy - 1;
        if (!isWitherRose(world, hx - 1, my, hz - 1)) return false;
        if (!isWitherRose(world, hx + 1, my, hz - 1)) return false;
        if (!isWitherRose(world, hx - 1, my, hz + 1)) return false;
        return isWitherRose(world, hx + 1, my, hz + 1);
    }

    private static boolean isSoulSand(World world, int x, int y, int z) {
        Material type = world.getBlockAt(x, y, z).getType();
        return type == Material.SOUL_SAND || type == Material.SOUL_SOIL;
    }

    private static boolean isStair(World world, int x, int y, int z) {
        return Tag.STAIRS.isTagged(world.getBlockAt(x, y, z).getType());
    }

    private static boolean isWitherRose(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType() == Material.WITHER_ROSE;
    }

    private void breakStructure(Block headBlock) {
        int hx = headBlock.getX();
        int hy = headBlock.getY();
        int hz = headBlock.getZ();
        World world = headBlock.getWorld();
        boolean leaveBase = plugin.isHrmLeaveStructureBase();

        // head
        setAir(world, hx, hy, hz);

        // fence + 4 roses
        setAir(world, hx, hy - 1, hz);
        setAir(world, hx - 1, hy - 1, hz - 1);
        setAir(world, hx + 1, hy - 1, hz - 1);
        setAir(world, hx - 1, hy - 1, hz + 1);
        setAir(world, hx + 1, hy - 1, hz + 1);

        if (!leaveBase) {
            int by = hy - 2;
            setAir(world, hx, by, hz);
            setAir(world, hx - 1, by, hz - 1);
            setAir(world, hx + 1, by, hz - 1);
            setAir(world, hx - 1, by, hz + 1);
            setAir(world, hx + 1, by, hz + 1);
            setAir(world, hx, by, hz - 1);
            setAir(world, hx - 1, by, hz);
            setAir(world, hx + 1, by, hz);
            setAir(world, hx, by, hz + 1);
        }
    }

    private static void setAir(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.AIR);
    }

    private void checkIncompleteStructure(Block headBlock, Player placer) {
        World world = headBlock.getWorld();
        int hx = headBlock.getX();
        int hy = headBlock.getY();
        int hz = headBlock.getZ();

        Block fence = world.getBlockAt(hx, hy - 1, hz);
        Block ore = world.getBlockAt(hx, hy - 2, hz);

        // partial match - fence+ore present but rest is wrong (for retarded people)
        if (Tag.FENCES.isTagged(fence.getType())
                && Tag.BEACON_BASE_BLOCKS.isTagged(ore.getType())) {
            placer.sendMessage(ChatColor.RED + "The revival structure is incomplete!");
            playErrorEffect(headBlock);
        }
    }

    private static void sendError(Player player, String message) {
        Bukkit.getScheduler().runTask(PolarSouls.getInstance(), () ->
                player.sendMessage(ChatColor.RED + message));
    }

    private static void playErrorEffect(Block block) {
        block.getWorld().playSound(block.getLocation(),
                Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 20f);
    }
}
