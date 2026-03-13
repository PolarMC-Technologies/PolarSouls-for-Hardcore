package org.polarnl.polarsouls.hrm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import org.polarnl.polarsouls.PolarSouls;
import org.polarnl.polarsouls.database.DatabaseManager;
import org.polarnl.polarsouls.model.PlayerData;

public class HeadDropListener implements Listener {

    private static final String PERM_BYPASS = "PolarSouls.bypass";
    private static final String SKIP_HEAD_DROP_MSG = "Skipping head drop for ";

    private final PolarSouls plugin;
    private final DatabaseManager db;
    // Tracks locations of skull blocks placed on death so cleanup can remove them
    // directly, even if their chunk is unloaded at revive time.
    private final Map<UUID, List<Location>> headBlockLocations = new ConcurrentHashMap<>();

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
                // Place / drop the head on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (plugin.isHrmHeadPlaceAsBlock()) {
                        // Place as a permanent block — never burns, never despawns
                        Block block = findSuitableBlock(world, deathLoc);
                        if (block != null) {
                            block.setType(Material.PLAYER_HEAD, false);
                            Skull skull = (Skull) block.getState();
                            skull.setOwningPlayer(player);
                            skull.update(true, false);
                            // Remember this location so cleanup can find it even
                            // if the chunk gets unloaded before the player is revived
                            headBlockLocations
                                    .computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                                    .add(block.getLocation());
                            if (plugin.isDebugMode()) {
                                plugin.debug("Placed " + player.getName() + "'s head block at "
                                        + block.getX() + ", " + block.getY() + ", " + block.getZ());
                            }
                        } else {
                            // Fallback so the head is never lost when no block can be placed.
                            dropHeadItem(world, deathLoc, player);
                            if (plugin.isDebugMode()) {
                                plugin.debug("No suitable block found to place " + player.getName()
                                        + "'s head; fell back to item drop.");
                            }
                        }
                    } else {
                        // Drop as item entity
                        dropHeadItem(world, deathLoc, player);
                    }
                });
            }, 10L); // 0.5s delay because why not it would break otherwise
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!plugin.isHrmDropHeads() || plugin.isHrmHeadPlaceAsBlock() || !plugin.isHrmHeadNoDespawn()) return;
        UUID ownerUuid = getHeadOwnerUuid(event.getEntity().getItemStack());
        if (ownerUuid == null) return;

        PlayerData data = db.getPlayer(ownerUuid);
        if (data != null && data.isDead()) {
            event.setCancelled(true);
        }
    }

    private UUID getHeadOwnerUuid(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return null;
        if (!(stack.getItemMeta() instanceof SkullMeta skullMeta)) return null;
        OfflinePlayer owner = skullMeta.getOwningPlayer();
        if (owner == null) return null;
        return owner.getUniqueId();
    }

    private void dropHeadItem(World world, Location deathLoc, Player player) {
        ItemStack head = createPlayerHead(player);
        Item item = world.dropItemNaturally(deathLoc, head);
        if (plugin.isHrmHeadFireproof()) {
            item.setInvulnerable(true);
        }
        if (plugin.isDebugMode()) {
            plugin.debug("Dropped " + player.getName() + "'s head at "
                    + deathLoc.getBlockX() + ", " + deathLoc.getBlockY()
                    + ", " + deathLoc.getBlockZ()
                    + (plugin.isHrmHeadFireproof() ? " (fireproof)" : "")
                    + (plugin.isHrmHeadNoDespawn() ? " (no-despawn)" : ""));
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

    // Removes every copy of a player's head from the world when they are revived.
    // Called once per revive, never on death.
    //
    // Two-pass design:
    //
    //  Pass 1 – Targeted removal (always O(n) with n = number of placed head blocks)
    //   headBlockLocations stores the Location of every skull block placed at death.
    //   On revive we look up those exact coords, force-load the chunk if necessary,
    //   verify the block still belongs to this player, set it to AIR, then release
    //   the chunk again if we had to load it.  This is instant and chunk-safe.
    //   Limitation: the map is in-memory only.  If the server restarts between death
    //   and revival the entries are lost and pass 2 acts as the safety net.
    //
    //  Pass 2 – Tick-spread fallback scan
    //   Catches anything pass 1 missed: item entities (item-entity mode), item frames,
    //   player/ender-chest inventories, shulker boxes, and stale skull blocks left by
    //   older plugin versions that lacked location tracking.
    //   Work is spread across multiple server ticks to avoid lag spikes:
    //     • Item entities  – 50 per tick
    //     • Item frames    – 50 per tick
    //     • Chunks (block scan, block-mode only) – 10 per tick
    //     • Online players – 5 per tick
    //   The chunk scan phase is skipped entirely when head-place-as-block is false
    //   because in item-entity mode there are no skull blocks to find in chunks.
    public void removeDroppedHeads(UUID ownerUuid) {
        // --- Targeted removal for block-mode heads ---
        // Remove known skull block locations first.  We force-load the chunk if
        // needed so this works even when the chunk is currently unloaded.
        List<Location> knownLocations = headBlockLocations.remove(ownerUuid);
        if (knownLocations != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Location loc : knownLocations) {
                    World w = loc.getWorld();
                    if (w == null) continue;
                    boolean wasLoaded = w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
                    // getBlockAt force-loads the chunk if not already loaded
                    Block b = w.getBlockAt(loc);
                    if (b.getType() == Material.PLAYER_HEAD || b.getType() == Material.PLAYER_WALL_HEAD) {
                        BlockState state = b.getState();
                        if (state instanceof Skull skull) {
                            OfflinePlayer owner = skull.getOwningPlayer();
                            if (owner != null && owner.getUniqueId().equals(ownerUuid)) {
                                b.setType(Material.AIR);
                                if (plugin.isDebugMode()) {
                                    plugin.debug("Removed tracked head block for " + ownerUuid
                                            + " at " + b.getX() + ", " + b.getY() + ", " + b.getZ());
                                }
                            }
                        }
                    }
                    // Unload the chunk again if we loaded it just for cleanup
                    if (!wasLoaded) {
                        w.unloadChunkRequest(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
                    }
                }
            });
        }

        // --- Pass 2: tick-spread fallback scan ---
        // In block-mode the chunk scan is necessary as a safety net for stale blocks
        // (server restart between death and revive loses the tracked location).
        // In item-entity mode there are never any skull blocks to find, so we skip
        // the whole chunk phase and save a significant amount of per-tick work.
        final boolean scanChunksForBlocks = plugin.isHrmHeadPlaceAsBlock();
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

            // Batch sizes to process per tick
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
                        processingItemFrames = false;
                        // Skip chunk scan entirely in item-entity mode — no skull blocks exist
                        if (scanChunksForBlocks) {
                            processingChunks = true;
                        } else {
                            processingPlayers = true;
                        }
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
                                if (state instanceof Skull skull) {
                                    OfflinePlayer skullOwner = skull.getOwningPlayer();
                                    if (skullOwner != null && skullOwner.getUniqueId().equals(ownerUuid)) {
                                        skull.getBlock().setType(Material.AIR);
                                        removedCount.incrementAndGet();
                                    }
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

    private static Block findSuitableBlock(World world, Location loc) {
        int x = loc.getBlockX();
        int startY = loc.getBlockY();
        int z = loc.getBlockZ();
        int maxY = Math.min(startY + 64, world.getMaxHeight() - 1);

        // Preferred: first air block sitting on top of a solid surface, scanning upward.
        // This naturally rises above lava/water pools to the nearest accessible floor/surface.
        for (int y = startY; y <= maxY; y++) {
            Block candidate = world.getBlockAt(x, y, z);
            if (!isAirBlock(candidate)) continue;
            Block below = world.getBlockAt(x, y - 1, z);
            if (below.getType().isSolid()) {
                return candidate;
            }
        }

        // Fallback: any air block going upward (e.g. open cave ceiling, or hovering above lava)
        for (int y = startY; y <= maxY; y++) {
            Block candidate = world.getBlockAt(x, y, z);
            if (isAirBlock(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isAirBlock(Block b) {
        Material t = b.getType();
        return t == Material.AIR || t == Material.CAVE_AIR || t == Material.VOID_AIR;
    }

    private static boolean isOwnedHead(ItemStack stack, UUID ownerUuid) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return false;
        if (!(stack.getItemMeta() instanceof SkullMeta skullMeta)) return false;
        OfflinePlayer skullOwner = skullMeta.getOwningPlayer();
        return skullOwner != null && skullOwner.getUniqueId().equals(ownerUuid);
    }
}
