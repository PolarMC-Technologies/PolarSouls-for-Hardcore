package com.mario.polarsouls.hrm;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.mario.polarsouls.PolarSouls;

// does that thingy with effects when you wear player head
public class HeadEffectsTask extends BukkitRunnable {

    private static final int INFINITE_DURATION = Integer.MAX_VALUE;

    private final PolarSouls plugin;
    private final Set<UUID> wearingHead = new HashSet<>();

    public HeadEffectsTask(PolarSouls plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean wearing = isWearingPlayerHead(player);

            if (wearing && !wearingHead.contains(uuid)) {
                applyEffects(player);
                wearingHead.add(uuid);
                plugin.debug(player.getName() + " equipped a player head, applying effects.");
            } else if (!wearing && wearingHead.remove(uuid)) {
                removeEffects(player);
                plugin.debug(player.getName() + " removed player head, removing effects.");
            }
        }

        // get rid of disconnected people since they stink
        wearingHead.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private static boolean isWearingPlayerHead(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.getType() == Material.PLAYER_HEAD;
    }

    private static void applyEffects(Player player) {
        // naseau thingy 200 ticks
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA, 200, 0, false, false));

        // effects stay until head gone
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HEALTH_BOOST, INFINITE_DURATION, 4, false, false));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, INFINITE_DURATION, 0, false, false));
    }

    private static void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }
}
