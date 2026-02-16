package net.bloodpine.core.gui;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StatsGUI {
    
    private final BloodpineCore plugin;
    
    public StatsGUI(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, colorize("&c&lBloodpine Ascension"));
        
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Token info
        gui.setItem(4, createItem(Material.SUNFLOWER, 
            "&e&lYour Tokens",
            "&7Total: &e" + data.getTotalTokens(),
            "&7Allocated: &e" + data.getTotalAllocatedTokens(),
            "&7Available: &a" + data.getAvailableTokens(),
            "",
            "&7Earn tokens by killing players!",
            "&7Click stats below to allocate."));
        
        // Stats
        gui.setItem(20, createDamageStat(data));
        gui.setItem(21, createDefenseStat(data));
        gui.setItem(22, createTotemStat(data));
        gui.setItem(23, createVitalityStat(data));
        
        // Player info
        gui.setItem(48, createItem(Material.PLAYER_HEAD,
            "&a&lYour Stats",
            "&7Kills: &e" + data.getTotalKills(),
            "&7Deaths: &e" + data.getTotalDeaths(),
            "&7K/D Ratio: &e" + String.format("%.2f", data.getKDRatio()),
            "",
            data.isMarked() ? "&c&l⚠ YOU ARE MARKED ⚠" : "&aYou are safe"));
        
        // Leaderboard
        gui.setItem(49, createItem(Material.GOLDEN_HELMET,
            "&6&lLeaderboard",
            "&7Click to view top players!"));

        // Rebirth info/action
        gui.setItem(45, createItem(Material.NETHER_STAR,
            "&d&lRebirth",
            "&7Level: &f" + data.getRebirthLevel(),
            "&7Points: &f" + data.getRebirthPoints(),
            "&7Required tokens: &e" + plugin.getRebirthManager().getRequiredTokens(),
            "",
            "&aRun &f/rebirth &ato rebirth."));

        // Token shop
        gui.setItem(53, createItem(Material.EMERALD,
            "&6&lToken Shop",
            "&7Spend your tokens on perks and kits.",
            "&aClick to open"));
        
        // Reset stats
        gui.setItem(50, createItem(Material.BARRIER,
            "&c&lReset Stats",
            "&7Refund all allocated tokens",
            "&7Click to reset!"));
        
        player.openInventory(gui);
    }
    
    public void openAllocateMenu(Player player, StatType statType) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&c&lAllocate " + statType.getDisplayName()));
        
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int available = data.getAvailableTokens();
        int current = data.getAllocatedTokens(statType);
        int max = plugin.getStatManager().getMaxTokensForStat(statType);
        
        // Info
        gui.setItem(4, getStatItem(statType, data, current, max));
        
        // Allocate buttons
        if (available >= 1 && current < max) {
            gui.setItem(10, createItem(Material.LIME_DYE, "&a+1 Token", "&7Click to allocate 1 token"));
        }
        
        if (available >= 5 && current + 5 <= max) {
            gui.setItem(11, createItem(Material.LIME_DYE, "&a+5 Tokens", "&7Click to allocate 5 tokens"));
        }
        
        if (available >= 10 && current + 10 <= max) {
            gui.setItem(12, createItem(Material.LIME_DYE, "&a+10 Tokens", "&7Click to allocate 10 tokens"));
        }
        
        // Back button
        gui.setItem(22, createItem(Material.ARROW, "&7« Back to Stats"));
        
        player.openInventory(gui);
    }
    
    private ItemStack createDamageStat(PlayerData data) {
        int allocated = data.getAllocatedTokens(StatType.DAMAGE);
        int max = plugin.getStatManager().getMaxTokensForStat(StatType.DAMAGE);
        double bonus = allocated * plugin.getConfig().getDouble("stats.damage.per-token", 1.0);
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Allocated: &e" + allocated + "&7/&e" + max));
        lore.add(colorize("&7Bonus: &a+" + bonus + "%"));
        lore.add("");
        lore.add(colorize("&7Increases melee & ranged damage"));
        lore.add(colorize("&7+10% per token"));
        lore.add("");
        if (data.getAvailableTokens() > 0 && allocated < max) {
            lore.add(colorize("&aClick to allocate!"));
        } else if (allocated >= max) {
            lore.add(colorize("&c&lMAXED OUT!"));
        } else {
            lore.add(colorize("&cNo tokens available!"));
        }
        
        return createItem(Material.DIAMOND_SWORD, "&c&l⚔ Damage", lore);
    }
    
    private ItemStack createDefenseStat(PlayerData data) {
        int allocated = data.getAllocatedTokens(StatType.DEFENSE);
        int max = plugin.getStatManager().getMaxTokensForStat(StatType.DEFENSE);
        double bonus = allocated * plugin.getConfig().getDouble("stats.defense.per-token", 1.0);
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Allocated: &e" + allocated + "&7/&e" + max));
        lore.add(colorize("&7Bonus: &a-" + bonus + "%"));
        lore.add("");
        lore.add(colorize("&7Reduces damage taken"));
        lore.add(colorize("&7+5% reduction per token"));
        lore.add("");
        if (data.getAvailableTokens() > 0 && allocated < max) {
            lore.add(colorize("&aClick to allocate!"));
        } else if (allocated >= max) {
            lore.add(colorize("&c&lMAXED OUT!"));
        } else {
            lore.add(colorize("&cNo tokens available!"));
        }
        
        return createItem(Material.DIAMOND_CHESTPLATE, "&9&l🛡 Defense", lore);
    }
    
    private ItemStack createCrystalStat(PlayerData data) {
        int allocated = data.getAllocatedTokens(StatType.CRYSTAL);
        int max = plugin.getStatManager().getMaxTokensForStat(StatType.CRYSTAL);
        double bonus = allocated * plugin.getConfig().getDouble("stats.crystal.per-token", 1.0);
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Allocated: &e" + allocated + "&7/&e" + max));
        lore.add(colorize("&7Bonus: &a+" + bonus + "%"));
        lore.add("");
        lore.add(colorize("&7Increases explosion/crystal damage"));
        lore.add(colorize("&7+15% per token"));
        lore.add("");
        if (data.getAvailableTokens() > 0 && allocated < max) {
            lore.add(colorize("&aClick to allocate!"));
        } else if (allocated >= max) {
            lore.add(colorize("&c&lMAXED OUT!"));
        } else {
            lore.add(colorize("&cNo tokens available!"));
        }
        
        return createItem(Material.END_CRYSTAL, "&d&l💎 Crystal Mastery", lore);
    }
    
    private ItemStack createTotemStat(PlayerData data) {
        int allocated = data.getAllocatedTokens(StatType.TOTEM);
        int max = plugin.getStatManager().getMaxTokensForStat(StatType.TOTEM);
        int absorptionHearts = allocated * plugin.getConfig().getInt("stats.totem.per-token", 1);
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Allocated: &e" + allocated + "&7/&e" + max));
        lore.add(colorize("&7Bonus: &a+" + absorptionHearts + " absorption hearts"));
        lore.add("");
        lore.add(colorize("&7Bonus absorption hearts on totem pop"));
        lore.add(colorize("&7+1 absorption heart per token"));
        lore.add("");
        if (data.getAvailableTokens() > 0 && allocated < max) {
            lore.add(colorize("&aClick to allocate!"));
        } else if (allocated >= max) {
            lore.add(colorize("&c&lMAXED OUT!"));
        } else {
            lore.add(colorize("&cNo tokens available!"));
        }
        
        return createItem(Material.TOTEM_OF_UNDYING, "&6&l🗿 Totem Efficiency", lore);
    }
    
    private ItemStack createVitalityStat(PlayerData data) {
        int allocated = data.getAllocatedTokens(StatType.VITALITY);
        int max = plugin.getStatManager().getMaxTokensForStat(StatType.VITALITY);
        double hpBonus = allocated * plugin.getConfig().getDouble("stats.vitality.per-token", 2.0);
        double hearts = hpBonus / 2.0;
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Allocated: &e" + allocated + "&7/&e" + max));
        lore.add(colorize("&7Bonus: &a+" + (int) hearts + " hearts"));
        lore.add("");
        lore.add(colorize("&7Increases max health"));
        lore.add(colorize("&7+1 heart per token"));
        lore.add("");
        if (data.getAvailableTokens() > 0 && allocated < max) {
            lore.add(colorize("&aClick to allocate!"));
        } else if (allocated >= max) {
            lore.add(colorize("&c&lMAXED OUT!"));
        } else {
            lore.add(colorize("&cNo tokens available!"));
        }
        
        return createItem(Material.GOLDEN_APPLE, "&a&l❤ Vitality", lore);
    }
    
    private ItemStack getStatItem(StatType type, PlayerData data, int current, int max) {
        return switch (type) {
            case DAMAGE -> createDamageStat(data);
            case DEFENSE -> createDefenseStat(data);
            case CRYSTAL -> createCrystalStat(data);
            case TOTEM -> createTotemStat(data);
            case VITALITY -> createVitalityStat(data);
        };
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, List.of(lore));
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
