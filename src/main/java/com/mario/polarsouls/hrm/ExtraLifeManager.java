package com.mario.polarsouls.hrm;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.mario.polarsouls.PolarSouls;
import com.mario.polarsouls.database.DatabaseManager;
import com.mario.polarsouls.model.PlayerData;
import com.mario.polarsouls.util.MessageUtil;

public class ExtraLifeManager implements Listener {

    private static final String PDC_KEY_VALUE = "extra_life";

    private final PolarSouls plugin;
    private final DatabaseManager db;
    private final NamespacedKey extraLifeKey;
    private final NamespacedKey recipeKey;

    public ExtraLifeManager(PolarSouls plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.extraLifeKey = new NamespacedKey(plugin, PDC_KEY_VALUE);
        this.recipeKey = new NamespacedKey(plugin, "extra_life_recipe");
    }

    public void registerRecipe() {
        ItemStack result = createExtraLifeItem();

        // check recipe shape and ingredients from config
        var cfg = plugin.getConfig();
        String row1 = cfg.getString("extra-life.recipe.row1", "GEG");
        String row2 = cfg.getString("extra-life.recipe.row2", "ENE");
        String row3 = cfg.getString("extra-life.recipe.row3", "GEG");

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(row1, row2, row3);

        var ingredientSection = cfg.getConfigurationSection("extra-life.recipe.ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                String materialName = ingredientSection.getString(key, "STONE");
                Material mat = (materialName != null) ? Material.matchMaterial(materialName) : null;
                if (mat != null && key.length() == 1) {
                    recipe.setIngredient(key.charAt(0), mat);
                } else {
                    plugin.getLogger().log(Level.WARNING,
                            "Invalid extra-life recipe ingredient: {0}={1}",
                            new Object[]{key, materialName});
                }
            }
        } else {
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('E', Material.EMERALD);
            recipe.setIngredient('N', Material.NETHER_STAR);
        }

        Bukkit.addRecipe(recipe);
        plugin.debug("Registered Extra Life crafting recipe.");
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }

    public ItemStack createExtraLifeItem() {
        String matName = plugin.getConfig().getString("extra-life.item-material", "NETHER_STAR");
        Material itemMaterial = (matName != null) ? Material.matchMaterial(matName) : null;
        if (itemMaterial == null) {
            itemMaterial = Material.NETHER_STAR;
        }

        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN.toString() + ChatColor.BOLD + "Extra Life");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Right-click to gain " + ChatColor.GREEN + "+1 Life",
                    ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + "Consumed on use"));
            meta.getPersistentDataContainer().set(extraLifeKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isExtraLifeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(extraLifeKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isExtraLifeItem(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                consumeExtraLife(player, item));
    }

    private void consumeExtraLife(Player player, ItemStack item) {
        PlayerData data = db.getPlayer(player.getUniqueId());
        if (data == null) {
            data = PlayerData.createNew(player.getUniqueId(), player.getName(),
                    plugin.getDefaultLives());
            db.savePlayer(data);
        }

        if (data.isDead()) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(MessageUtil.get("extra-life-dead")));
            return;
        }

        int maxLives = plugin.getMaxLives();
        if (maxLives > 0 && data.getLives() >= maxLives) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(MessageUtil.get("extra-life-max",
                            "max", maxLives)));
            return;
        }

        int newLives = data.getLives() + 1;
        db.setLives(data.getUuid(), newLives);
        plugin.getLogger().log(Level.INFO, "{0} used Extra Life item (now {1} lives)",
                new Object[]{player.getName(), newLives});

        final int finalLives = newLives;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            player.sendMessage(MessageUtil.get("extra-life-used",
                    "lives", finalLives));

            Location loc = player.getLocation();
            if (loc != null) {
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING, 60, 0, false, true));
        });
    }
}
