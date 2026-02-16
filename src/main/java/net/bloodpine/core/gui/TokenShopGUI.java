package net.bloodpine.core.gui;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TokenShopGUI {

    private final BloodpineCore plugin;

    public TokenShopGUI(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&6&lToken Shop"));
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        int heartShieldCost = plugin.getConfig().getInt("token-shop.heart-shield-cost", 8);
        int tokenBoostCost = plugin.getConfig().getInt("token-shop.token-boost-30m-cost", 10);
        int killEffectCost = plugin.getConfig().getInt("token-shop.kill-effect-30m-cost", 6);
        int extraHeartCost = plugin.getConfig().getInt("token-shop.extra-heart-cost", 15);
        int warriorKitCost = plugin.getConfig().getInt("token-shop.warrior-kit-cost", 20);
        int fullHealCost = plugin.getConfig().getInt("token-shop.full-heal-cost", 5);

        gui.setItem(4, createItem(Material.SUNFLOWER, "&e&lYour Tokens", "&7Available: &e" + data.getAvailableTokens()));

        gui.setItem(10, shopItem(Material.TOTEM_OF_UNDYING, "&aHeart Shield x1", heartShieldCost, "&7Protects one death from heart loss."));
        gui.setItem(11, shopItem(Material.BLAZE_POWDER, "&6Token Boost 2x (30m)", tokenBoostCost, "&7Double token gain for 30 minutes."));
        gui.setItem(12, shopItem(Material.LIGHTNING_ROD, "&eKill Effect (30m)", killEffectCost, "&7Lightning strike effect on kills."));
        gui.setItem(14, shopItem(Material.GOLDEN_APPLE, "&c+1 Extra Heart", extraHeartCost, "&7Permanent +1 heart."));
        gui.setItem(15, shopItem(Material.DIAMOND_SWORD, "&bWarrior Kit", warriorKitCost, "&7Instant warrior kit."));
        gui.setItem(16, shopItem(Material.COOKED_BEEF, "&aFull Heal", fullHealCost, "&7Refill health and food."));

        player.openInventory(gui);
    }

    public void handleClick(Player player, String plainName) {
        if (plainName.equalsIgnoreCase("Heart Shield x1")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.heart-shield-cost", 8))) {
                plugin.getBoostManager().giveHeartShield(player, 1);
            }
            return;
        }

        if (plainName.equalsIgnoreCase("Token Boost 2x (30m)")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.token-boost-30m-cost", 10))) {
                plugin.getBoostManager().giveTokenBoost(player, 30);
            }
            return;
        }

        if (plainName.equalsIgnoreCase("Kill Effect (30m)")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.kill-effect-30m-cost", 6))) {
                plugin.getBoostManager().giveKillEffect(player, 30);
            }
            return;
        }

        if (plainName.equalsIgnoreCase("+1 Extra Heart")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.extra-heart-cost", 15))) {
                plugin.getBoostManager().giveExtraHearts(player, 1);
            }
            return;
        }

        if (plainName.equalsIgnoreCase("Warrior Kit")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.warrior-kit-cost", 20))) {
                plugin.getBoostManager().giveKit(player, "warrior");
            }
            return;
        }

        if (plainName.equalsIgnoreCase("Full Heal")) {
            if (purchase(player, plugin.getConfig().getInt("token-shop.full-heal-cost", 5))) {
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                player.sendMessage(colorize("&aYou are fully healed."));
            }
        }
    }

    private boolean purchase(Player player, int cost) {
        int available = plugin.getTokenManager().getAvailableTokens(player);
        if (available < cost) {
            player.sendMessage(colorize("&cNot enough available tokens. Need &e" + cost + "&c, have &e" + available));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }

        plugin.getTokenManager().removeTokens(player, cost);
        player.sendMessage(colorize("&aPurchased for &e" + cost + " tokens&a."));
        return true;
    }

    private ItemStack shopItem(Material material, String name, int cost, String... description) {
        List<String> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(line);
        }
        lore.add("");
        lore.add("&7Cost: &e" + cost + " tokens");
        lore.add("&aClick to buy");
        return createItem(material, name, lore.toArray(new String[0]));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));

        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(colorize(line));
        }
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
