package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class BoostManager {

    private final BloodpineCore plugin;

    // UUID -> expiry timestamp for 2x token boost
    private final Map<UUID, Long> tokenBoosts = new HashMap<>();

    // UUID -> number of heart shields remaining (no heart loss on death)
    private final Map<UUID, Integer> heartShields = new HashMap<>();

    // UUID -> expiry timestamp for kill effect (lightning on kill)
    private final Map<UUID, Long> killEffects = new HashMap<>();

    public BoostManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    // ========== TOKEN BOOST (2x tokens for X minutes) ==========

    public void giveTokenBoost(Player player, int minutes) {
        long expiry = System.currentTimeMillis() + (minutes * 60 * 1000L);
        // Extend if already boosted
        Long current = tokenBoosts.get(player.getUniqueId());
        if (current != null && current > System.currentTimeMillis()) {
            expiry = current + (minutes * 60 * 1000L);
        }
        tokenBoosts.put(player.getUniqueId(), expiry);

        player.sendMessage(colorize("&6&l⭐ STORE &7» &aYou now have &e2x Token Boost &afor &e" + minutes + " minutes&a!"));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void clearPlayerEffects(UUID uuid) {
        tokenBoosts.remove(uuid);
        heartShields.remove(uuid);
        killEffects.remove(uuid);
    }

    public boolean hasTokenBoost(Player player) {
        Long expiry = tokenBoosts.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            tokenBoosts.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public int getTokenMultiplier(Player player) {
        return hasTokenBoost(player) ? 2 : 1;
    }

    // ========== HEART SHIELD (prevent heart loss on next X deaths) ==========

    public void giveHeartShield(Player player, int uses) {
        int current = heartShields.getOrDefault(player.getUniqueId(), 0);
        heartShields.put(player.getUniqueId(), current + uses);

        player.sendMessage(colorize("&6&l⭐ STORE &7» &aYou received &e" + uses + " Heart Shield(s)&a! " +
                "You won't lose hearts on your next &e" + (current + uses) + " &adeaths."));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.2f);
    }

    public boolean hasHeartShield(Player player) {
        return heartShields.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    public boolean consumeHeartShield(Player player) {
        int shields = heartShields.getOrDefault(player.getUniqueId(), 0);
        if (shields <= 0) return false;
        heartShields.put(player.getUniqueId(), shields - 1);
        player.sendMessage(colorize("&6&l⭐ &e&lHeart Shield activated! &7You didn't lose a heart. &8(" + (shields - 1) + " remaining)"));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.5f);
        return true;
    }

    // ========== KILL EFFECTS (lightning strike on kill for X minutes) ==========

    public void giveKillEffect(Player player, int minutes) {
        long expiry = System.currentTimeMillis() + (minutes * 60 * 1000L);
        Long current = killEffects.get(player.getUniqueId());
        if (current != null && current > System.currentTimeMillis()) {
            expiry = current + (minutes * 60 * 1000L);
        }
        killEffects.put(player.getUniqueId(), expiry);

        player.sendMessage(colorize("&6&l⭐ STORE &7» &aYou now have &e⚡ Lightning Kill Effect &afor &e" + minutes + " minutes&a!"));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
    }

    public boolean hasKillEffect(Player player) {
        Long expiry = killEffects.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            killEffects.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    // ========== EXTRA HEARTS (permanent lifesteal hearts) ==========

    public void giveExtraHearts(Player player, int hearts) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        for (int i = 0; i < hearts; i++) {
            data.addLifestealHeart();
        }
        plugin.getStatManager().applyStats(player);

        double newHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
        player.sendMessage(colorize("&6&l⭐ STORE &7» &aYou received &c❤ " + hearts + " extra heart(s)&a! " +
                "&7(" + (newHealth / 2.0) + " hearts total)"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
    }

    // ========== REVIVE (unban eliminated player + reset hearts) ==========

    public void revivePlayer(String playerName) {
        // Remove ban
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);

        // If they're online somehow, reset their hearts
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
            // Reset lifesteal hearts to give them starting hearts
            data.setLifestealHearts(0);
            plugin.getStatManager().applyStats(player);
            player.sendMessage(colorize("&6&l⭐ STORE &7» &a&lYou have been REVIVED! &7Your hearts have been restored."));
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        }

        Bukkit.broadcastMessage(colorize("&6&l⭐ &e" + playerName + " &ahas been &lREVIVED &afrom elimination! &7Welcome back!"));
    }

    // ========== STARTER KIT ==========

    public void giveKit(Player player, String kitName) {
        org.bukkit.inventory.ItemStack[] items;
        String kitDisplay;

        switch (kitName.toLowerCase()) {
            case "warrior":
                kitDisplay = "&c&lWarrior Kit";
                items = createWarriorKit();
                break;
            case "tank":
                kitDisplay = "&9&lTank Kit";
                items = createTankKit();
                break;
            case "god":
                kitDisplay = "&d&lGod Kit";
                items = createGodKit();
                break;
            default:
                return;
        }

        for (org.bukkit.inventory.ItemStack item : items) {
            if (item != null) {
                HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(item);
                // Drop overflow items at player's feet
                for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        player.sendMessage(colorize("&6&l⭐ STORE &7» &aYou received the " + kitDisplay + "&a!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        Bukkit.broadcastMessage(colorize("&6&l⭐ &f" + player.getName() + " &7just purchased the " + kitDisplay + "&7!"));
    }

    private org.bukkit.inventory.ItemStack[] createWarriorKit() {
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD);
        org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(colorize("&c&lWarrior's Blade"));
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 3, true);
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2, true);
        sword.setItemMeta(swordMeta);

        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET);
        org.bukkit.inventory.meta.ItemMeta hMeta = helmet.getItemMeta();
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 2, true);
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2, true);
        helmet.setItemMeta(hMeta);

        org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE);
        org.bukkit.inventory.meta.ItemMeta cMeta = chest.getItemMeta();
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 2, true);
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2, true);
        chest.setItemMeta(cMeta);

        org.bukkit.inventory.ItemStack legs = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS);
        org.bukkit.inventory.meta.ItemMeta lMeta = legs.getItemMeta();
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 2, true);
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2, true);
        legs.setItemMeta(lMeta);

        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS);
        org.bukkit.inventory.meta.ItemMeta bMeta = boots.getItemMeta();
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 2, true);
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2, true);
        boots.setItemMeta(bMeta);

        org.bukkit.inventory.ItemStack gapples = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 16);
        org.bukkit.inventory.ItemStack pearls = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL, 8);

        return new org.bukkit.inventory.ItemStack[]{sword, helmet, chest, legs, boots, gapples, pearls};
    }

    private org.bukkit.inventory.ItemStack[] createTankKit() {
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD);
        org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(colorize("&9&lTank's Edge"));
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 2, true);
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        sword.setItemMeta(swordMeta);

        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_HELMET);
        org.bukkit.inventory.meta.ItemMeta hMeta = helmet.getItemMeta();
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 3, true);
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        helmet.setItemMeta(hMeta);

        org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE);
        org.bukkit.inventory.meta.ItemMeta cMeta = chest.getItemMeta();
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 3, true);
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        chest.setItemMeta(cMeta);

        org.bukkit.inventory.ItemStack legs = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS);
        org.bukkit.inventory.meta.ItemMeta lMeta = legs.getItemMeta();
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 3, true);
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        legs.setItemMeta(lMeta);

        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_BOOTS);
        org.bukkit.inventory.meta.ItemMeta bMeta = boots.getItemMeta();
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 3, true);
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        boots.setItemMeta(bMeta);

        org.bukkit.inventory.ItemStack gapples = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 32);
        org.bukkit.inventory.ItemStack totems = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING, 2);

        return new org.bukkit.inventory.ItemStack[]{sword, helmet, chest, legs, boots, gapples, totems};
    }

    private org.bukkit.inventory.ItemStack[] createGodKit() {
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_SWORD);
        org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(colorize("&d&l✦ Bloodpine Godslayer ✦"));
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2, true);
        swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3, true);
        sword.setItemMeta(swordMeta);

        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_HELMET);
        org.bukkit.inventory.meta.ItemMeta hMeta = helmet.getItemMeta();
        hMeta.setDisplayName(colorize("&d&lGod Helmet"));
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 4, true);
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        hMeta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        helmet.setItemMeta(hMeta);

        org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE);
        org.bukkit.inventory.meta.ItemMeta cMeta = chest.getItemMeta();
        cMeta.setDisplayName(colorize("&d&lGod Chestplate"));
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 4, true);
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        cMeta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        chest.setItemMeta(cMeta);

        org.bukkit.inventory.ItemStack legs = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS);
        org.bukkit.inventory.meta.ItemMeta lMeta = legs.getItemMeta();
        lMeta.setDisplayName(colorize("&d&lGod Leggings"));
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 4, true);
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        lMeta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        legs.setItemMeta(lMeta);

        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_BOOTS);
        org.bukkit.inventory.meta.ItemMeta bMeta = boots.getItemMeta();
        bMeta.setDisplayName(colorize("&d&lGod Boots"));
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 4, true);
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        bMeta.addEnchant(org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 3, true);
        boots.setItemMeta(bMeta);

        org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
        org.bukkit.inventory.meta.ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setDisplayName(colorize("&d&lGod Bow"));
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 5, true);
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.PUNCH, 2, true);
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.INFINITY, 1, true);
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        bow.setItemMeta(bowMeta);

        org.bukkit.inventory.ItemStack gapples = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENCHANTED_GOLDEN_APPLE, 4);
        org.bukkit.inventory.ItemStack gaps = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 64);
        org.bukkit.inventory.ItemStack totems = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING, 4);
        org.bukkit.inventory.ItemStack pearls = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL, 16);
        org.bukkit.inventory.ItemStack arrows = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 1);

        return new org.bukkit.inventory.ItemStack[]{sword, helmet, chest, legs, boots, bow, gapples, gaps, totems, pearls, arrows};
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
