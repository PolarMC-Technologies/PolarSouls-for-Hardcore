package com.mario.polarsouls.util;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Utility class for player revival operations.
 */
public final class PlayerRevivalUtil {

    private PlayerRevivalUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Restore an online spectator player to survival mode and optionally transfer to main server.
     * This method handles the restoration of a dead player back to survival mode.
     *
     * @param plugin The PolarSouls plugin instance
     * @param data   The player data containing UUID and other information
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
