package net.bloodpine.core.gui;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardGUI {
    
    private final BloodpineCore plugin;
    
    public LeaderboardGUI(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    public void openLeaderboard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, colorize("&6&lToken Leaderboard"));
        
        List<PlayerData> topPlayers = plugin.getDataManager().getTopPlayers(10);
        
        // Title
        gui.setItem(4, createItem(Material.DIAMOND,
            "&6&l⭐ TOP PLAYERS ⭐",
            "&7Most tokens in the realm"));
        
        int[] positions = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            PlayerData data = topPlayers.get(i);
            gui.setItem(positions[i], createPlayerHead(data, i + 1));
        }
        
        // Back button
        gui.setItem(49, createItem(Material.ARROW, "&7« Back"));
        
        player.openInventory(gui);
    }
    
    private ItemStack createPlayerHead(PlayerData data, int position) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        String prefix = switch (position) {
            case 1 -> "&6&l#1 ";
            case 2 -> "&7&l#2 ";
            case 3 -> "&c&l#3 ";
            default -> "&7#" + position + " ";
        };
        
        meta.setDisplayName(colorize(prefix + data.getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Tokens: &e" + data.getTotalTokens()));
        lore.add(colorize("&7Kills: &a" + data.getTotalKills()));
        lore.add(colorize("&7Deaths: &c" + data.getTotalDeaths()));
        lore.add(colorize("&7K/D: &e" + String.format("%.2f", data.getKDRatio())));
        
        if (data.isMarked()) {
            lore.add("");
            lore.add(colorize("&c&l⚠ MARKED ⚠"));
        }
        
        meta.setLore(lore);
        skull.setItemMeta(meta);
        
        return skull;
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
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
