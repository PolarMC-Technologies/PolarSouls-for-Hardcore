/*
RevivePlus by Cera and Jakeccz
Copyright (C) 2026 Commune

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RevivePlus.  If not, see <https://www.gnu.org/licenses/>
 */

package org.ssoggy.ssoggysouls.hrm.dlc.listener;

import org.ssoggy.ssoggysouls.hrm.dlc.enums.GAMEMODESENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.enums.COMMANDOUTPUTENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPCommandOutput;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPStatic;
import net.kyori.adventure.text.Component;
import org.ssoggy.ssoggysouls.hrm.dlc.util.Pair;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.Instant;
import java.util.UUID;

public class BlockEvents implements Listener {
    private static final String KEY_DEATHPOS = "deathpos";
    private static final String KEY_DEATHTIME = "deathtime";
    private static final String KEY_DEATHHOLDER = "deathholder";

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        BlockState state = event.getBlock().getState();
        if (state instanceof Skull skull) {
            OfflinePlayer skullOwner = skull.getOwningPlayer();
            if (skullOwner == null) return;

            Player destroyer = event.getPlayer();
            World world = skull.getWorld();
            UUID uuid = skullOwner.getUniqueId();

            if (skullOwner.getPlayer() instanceof Player destroyed && GAMEMODESENUM.getPlayerGameMode(destroyed) == GAMEMODESENUM.GHOSTMODE) { // Once a bug now a feature
                RPStatic.DEAD_LOCATIONS.remove(uuid); // Instead I'll track them by DEAD_HOLDER
                RPStatic.DEAD_HOLDERS.put(uuid, destroyer.getUniqueId());
                RPStatic.DEAD_STORAGE.removeValue(uuid.toString(), KEY_DEATHPOS);
                if (RPStatic.DEAD_STORAGE.setValueIfChanged(uuid.toString(), KEY_DEATHHOLDER, destroyer.getUniqueId().toString())) {
                    RPStatic.DEAD_STORAGE.saveConfig();
                }

                world.spawnParticle(Particle.SOUL, event.getBlock().getLocation().add(0.5, 1 , 0.5), 1, 0, 0, 0, 0.01);

                destroyed.setGameMode(GameMode.SPECTATOR);
                destroyed.setSpectatorTarget(destroyer);
                RPCommandOutput message = new RPCommandOutput();
                message.success = COMMANDOUTPUTENUM.INFO;
                message.message = "Started spectating " + (destroyer.getName() != null ? MiniMessage.miniMessage().escapeTags(destroyer.getName()) : "Unknown");
                destroyed.sendRichMessage(message.toString());
                destroyed.sendActionBar(Component.text((destroyer.getName() != null ? destroyer.getName() : "Unknown") + " is currently carrying your playerhead..."));
            }
        }
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.PLAYER_HEAD) return;

        ItemMeta handMeta = event.getItemInHand().getItemMeta();
        if (handMeta instanceof SkullMeta meta) {
            OfflinePlayer skullOwner = meta.getOwningPlayer();
            if (skullOwner == null) return;

            Location location = block.getLocation();
            World world = location.getWorld();
            UUID uuid = skullOwner.getUniqueId();
            Instant now = Instant.now();
            Player ownerPlayer = skullOwner.getPlayer();

            world.spawnParticle(Particle.SOUL, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0.005);

            if (ownerPlayer == null || GAMEMODESENUM.getPlayerGameMode(ownerPlayer) != GAMEMODESENUM.GHOSTMODE) {
                return;
            }

            // Run after MONITOR listeners so successful ritual revives can update gamemode first.
            Bukkit.getScheduler().runTaskLater(RPStatic.CLIENT, () -> {
                String uuidString = uuid.toString();

                if (!ownerPlayer.isOnline() || GAMEMODESENUM.getPlayerGameMode(ownerPlayer) != GAMEMODESENUM.GHOSTMODE) {
                    RPStatic.DEAD_LOCATIONS.remove(uuid);
                    RPStatic.DEAD_HOLDERS.remove(uuid);
                    RPStatic.DEAD_STORAGE.removeValue(uuidString, KEY_DEATHHOLDER);
                    RPStatic.DEAD_STORAGE.removeValue(uuidString, KEY_DEATHPOS);
                    RPStatic.DEAD_STORAGE.removeValue(uuidString, KEY_DEATHTIME);
                    RPStatic.DEAD_STORAGE.saveConfig();
                    return;
                }

                RPStatic.DEAD_STORAGE.removeValue(uuidString, KEY_DEATHHOLDER);
                RPStatic.DEAD_HOLDERS.remove(uuid);
                RPStatic.DEAD_LOCATIONS.put(uuid, Pair.of(location, now));
                RPStatic.DEAD_STORAGE.setValue(uuidString, KEY_DEATHPOS,
                        location.getBlockX() + "$" + location.getBlockY() + "$" + location.getBlockZ() + "$" + world.getName());
                RPStatic.DEAD_STORAGE.setValue(uuidString, KEY_DEATHTIME, now.toString());
                RPStatic.DEAD_STORAGE.saveConfig();
            }, 1L);

        }
    }
}
