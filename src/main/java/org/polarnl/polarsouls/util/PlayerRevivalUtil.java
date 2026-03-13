package org.polarnl.polarsouls.util;

import org.polarnl.polarsouls.PolarSouls;
import org.polarnl.polarsouls.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class PlayerRevivalUtil {

    private PlayerRevivalUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * restores an online spectator to survival and optionally transfers them from limbo.
     *
     * @param plugin the PolarSouls plugin instance
     * @param data the player data
     */
    public static void restoreOnlineSpectator(PolarSouls plugin, PlayerData data) {
        Player target = Bukkit.getPlayer(data.getUuid());
        if (target != null && target.isOnline()
                && target.getGameMode() != GameMode.SURVIVAL) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    plugin.getLimboDeadPlayers().remove(target.getUniqueId());
                    target.setGameMode(GameMode.SURVIVAL);
                    target.sendMessage(MessageUtil.get("revive-success"));

                    if (plugin.isLimboServer()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (target.isOnline()) {
                                ServerTransferUtil.sendToMain(target);
                            }
                        }, 40L);
                    }
                }
            });
        }
    }
}
