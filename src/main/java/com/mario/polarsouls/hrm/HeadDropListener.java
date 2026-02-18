package com.mario.polarsouls.hrm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;

public class HeadDropListener implements Listener {

    private static final String PERM_BYPASS = "PolarSouls.bypass";
    private static final String SKIP_HEAD_DROP_MSG = "Skipping head drop for ";

    private final PolarSouls plugin;
    private final DatabaseManager db;

    public HeadDropListener(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission(PERM_BYPASS)) return;

        World world = player.getWorld();
        Location deathLoc = player.getLocation();
        if (deathLoc == null) return;

        if (plugin.isHrmDeathLocationMsg()) {
            player.sendMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC
                    + "You died at " + deathLoc.getBlockX() + ", "
                    + deathLoc.getBlockY() + ", " + deathLoc.getBlockZ()
                    + " in " + world.getName());
        }

        // only drop head if really dead (work pls)
        if (plugin.isHrmDropHeads()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                PlayerData data = db.getPlayer(player.getUniqueId());
                if (data == null) {
                    if (plugin.isDebugMode()) {
                        plugin.debug(SKIP_HEAD_DROP_MSG + player.getName() + " (no data).");
                    }
                    return;
                }
                if (!data.isDead()) {
                    if (plugin.isDebugMode()) {
                        plugin.debug(SKIP_HEAD_DROP_MSG + player.getName() + " (not dead).");
                    }
                    return;
                }
                if (data.isInGracePeriod(plugin.getGracePeriodMillis())) {
                    if (plugin.isDebugMode()) {
                        plugin.debug(SKIP_HEAD_DROP_MSG + player.getName() + " (grace period).");
                    }
                    return;
                }
                // Drop head on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack head = createPlayerHead(player);
                    world.dropItemNaturally(deathLoc, head);
                    if (plugin.isDebugMode()) {
                        plugin.debug("Dropped " + player.getName() + "'s head at "
                                + deathLoc.getBlockX() + ", " + deathLoc.getBlockY()
                                + ", " + deathLoc.getBlockZ());
                    }
                });
            }, 10L); // 0.5s delay because idfk it feels good
        }
    }

    public static ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.YELLOW + player.getName() + "'s Head");
            meta.setLore(List.of(
                    ChatColor.DARK_RED.toString() + ChatColor.ITALIC + "A fallen player's head",
                    ChatColor.GRAY + "Place on a revival structure to revive"));
            head.setItemMeta(meta);
        }
        return head;
    }

    // Removes all of a player's heads from existence across all worlds
    // This operation is spread across multiple ticks to prevent server lag on large servers
    // It's only called when a player is revived, not on every death
    //
    // IMPORTANT: This method schedules work across multiple ticks to avoid lag spikes
    // Multi-tick approach prevents server freezing on large servers with many chunks/entities/players
    //
    // Performance considerations:
    // - Processes a limited number of items per tick (configurable via batch sizes)
    // - Spreads work across multiple server ticks to maintain server responsiveness
    // - Total cleanup time: ~1-2 seconds on large servers vs single-tick lag spike
    // - Alternative approaches considered:
    //   1. Track head locations on drop (memory overhead, complex state management)
    //   2. Limit cleanup to specific radius (heads could remain far from spawn)
    //   3. Single-tick scan (causes lag spikes, rejected based on PR feedback)
    // - Current approach prioritizes server performance and responsiveness
    public void removeDroppedHeads(UUID ownerUuid) {
        new BukkitRunnable() {
            private final List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
            private final List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            private final AtomicInteger removedCount = new AtomicInteger(0);
            private int worldIndex = 0;
            private int playerIndex = 0;
            private List<Item> currentItemEntities = null;
            private List<ItemFrame> currentItemFrames = null;
            private List<Chunk> currentChunks = null;
            private int entityIndex = 0;
            private int chunkIndex = 0;
            private boolean processingItemEntities = true;
            private boolean processingItemFrames = false;
            private boolean processingChunks = false;
            private boolean processingPlayers = false;

            // Batch sizes to process per tick (tunable for performance)
            private final int ENTITIES_PER_TICK = 50;
            private final int CHUNKS_PER_TICK = 10;
            private final int PLAYERS_PER_TICK = 5;

            @Override
            public void run() {
                // Process item entities from worlds
                if (processingItemEntities) {
                    if (worldIndex >= worlds.size()) {
                        // Done with item entities, move to item frames
                        processingItemEntities = false;
                        processingItemFrames = true;
                        worldIndex = 0;
                        return;
                    }

                    World world = worlds.get(worldIndex);
                    if (currentItemEntities == null) {
                        currentItemEntities = new ArrayList<>(world.getEntitiesByClass(Item.class));
                        entityIndex = 0;
                    }

                    int processed = 0;
                    while (entityIndex < currentItemEntities.size() && processed < ENTITIES_PER_TICK) {
                        Item itemEntity = currentItemEntities.get(entityIndex);
                        if (itemEntity.isValid() && isOwnedHead(itemEntity.getItemStack(), ownerUuid)) {
                            itemEntity.remove();
                            removedCount.incrementAndGet();
                        }
                        entityIndex++;
                        processed++;
                    }

                    if (entityIndex >= currentItemEntities.size()) {
                        // Done with this world, move to next
                        currentItemEntities = null;
                        worldIndex++;
                    }
                    return;
                }

                // Process item frames from worlds
                if (processingItemFrames) {
                    if (worldIndex >= worlds.size()) {
                        // Done with item frames, move to chunks
                        processingItemFrames = false;
                        processingChunks = true;
                        worldIndex = 0;
                        return;
                    }

                    World world = worlds.get(worldIndex);
                    if (currentItemFrames == null) {
                        currentItemFrames = new ArrayList<>(world.getEntitiesByClass(ItemFrame.class));
                        entityIndex = 0;
                    }

                    int processed = 0;
                    while (entityIndex < currentItemFrames.size() && processed < ENTITIES_PER_TICK) {
                        ItemFrame frame = currentItemFrames.get(entityIndex);
                        if (frame.isValid() && isOwnedHead(frame.getItem(), ownerUuid)) {
                            frame.setItem(null);
                            removedCount.incrementAndGet();
                        }
                        entityIndex++;
                        processed++;
                    }

                    if (entityIndex >= currentItemFrames.size()) {
                        // Done with this world, move to next
                        currentItemFrames = null;
                        worldIndex++;
                    }
                    return;
                }

                // Process chunks in worlds
                if (processingChunks) {
                    if (worldIndex >= worlds.size()) {
                        // Done with chunks, move to players
                        processingChunks = false;
                        processingPlayers = true;
                        return;
                    }

                    World world = worlds.get(worldIndex);
                    if (currentChunks == null) {
                        currentChunks = new ArrayList<>(List.of(world.getLoadedChunks()));
                        chunkIndex = 0;
                    }

                    int processed = 0;
                    while (chunkIndex < currentChunks.size() && processed < CHUNKS_PER_TICK) {
                        Chunk chunk = currentChunks.get(chunkIndex);
                        if (chunk.isLoaded()) {
                            for (BlockState state : chunk.getTileEntities()) {
                                if (state instanceof InventoryHolder holder) {
                                    removedCount.addAndGet(removeFromInventory(holder.getInventory(), ownerUuid));
                                }
                            }
                        }
                        chunkIndex++;
                        processed++;
                    }

                    if (chunkIndex >= currentChunks.size()) {
                        // Done with this world, move to next
                        currentChunks = null;
                        worldIndex++;
                    }
                    return;
                }

                // Process online players
                if (processingPlayers) {
                    int processed = 0;
                    while (playerIndex < players.size() && processed < PLAYERS_PER_TICK) {
                        Player player = players.get(playerIndex);
                        if (player.isOnline()) {
                            PlayerInventory inv = player.getInventory();
                            for (int i = 0; i < inv.getSize(); i++) {
                                if (isOwnedHead(inv.getItem(i), ownerUuid)) {
                                    inv.setItem(i, null);
                                    removedCount.incrementAndGet();
                                }
                            }
                            removedCount.addAndGet(removeFromInventory(player.getEnderChest(), ownerUuid));
                        }
                        playerIndex++;
                        processed++;
                    }

                    if (playerIndex >= players.size()) {
                        // Done with all processing
                        int total = removedCount.get();
                        if (total > 0) {
                            Bukkit.getLogger().info("Removed " + total + " player head(s) for UUID " + ownerUuid);
                        }
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
    }

    private static int removeFromInventory(Inventory inv, UUID ownerUuid) {
        int removedCount = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            if (isOwnedHead(item, ownerUuid)) {
                inv.setItem(i, null);
                removedCount++;
            } else if (isShulkerBox(item.getType())) {
                removedCount += removeFromShulkerItem(inv, i, item, ownerUuid);
            }
        }
        return removedCount;
    }

    private static int removeFromShulkerItem(Inventory inv, int slot, ItemStack item, UUID ownerUuid) {
        if (!item.hasItemMeta()) return 0;
        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) return 0;
        BlockState blockState = bsm.getBlockState();
        if (!(blockState instanceof InventoryHolder shulkerHolder)) return 0;

        Inventory shulkerInv = shulkerHolder.getInventory();
        int removedCount = 0;
        boolean changed = false;
        for (int j = 0; j < shulkerInv.getSize(); j++) {
            if (isOwnedHead(shulkerInv.getItem(j), ownerUuid)) {
                shulkerInv.setItem(j, null);
                changed = true;
                removedCount++;
            }
        }
        if (changed) {
            bsm.setBlockState(blockState);
            item.setItemMeta(bsm);
            inv.setItem(slot, item);
        }
        return removedCount;
    }

    private static boolean isShulkerBox(Material type) {
        return Tag.SHULKER_BOXES.isTagged(type);
    }

    private static boolean isOwnedHead(ItemStack stack, UUID ownerUuid) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return false;
        if (!(stack.getItemMeta() instanceof SkullMeta skullMeta)) return false;
        OfflinePlayer skullOwner = skullMeta.getOwningPlayer();
        return skullOwner != null && skullOwner.getUniqueId().equals(ownerUuid);
    }
}
