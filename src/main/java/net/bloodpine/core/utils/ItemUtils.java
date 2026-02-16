package net.bloodpine.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtils {

    /**
     * Create a physical Token item
     */
    public static ItemStack createTokenItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Bloodpine Token");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "A valuable token that can be");
            lore.add(ChatColor.GRAY + "redeemed for rewards.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-click to redeem!");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Bloodpine SMP");
            
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Create a physical Heart item
     */
    public static ItemStack createHeartItem(int amount) {
        ItemStack item = new ItemStack(Material.GHAST_TEAR, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "❤ Bloodpine Heart");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "A crystallized heart containing");
            lore.add(ChatColor.GRAY + "life essence.");
            lore.add("");
            lore.add(ChatColor.RED + "Right-click to absorb!");
            lore.add(ChatColor.GRAY + "Adds " + ChatColor.RED + "+1 Max Heart");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Bloodpine SMP");
            
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if an ItemStack is a Token item
     */
    public static boolean isTokenItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.equals("Bloodpine Token");
    }

    /**
     * Check if an ItemStack is a Heart item
     */
    public static boolean isHeartItem(ItemStack item) {
        if (item == null || item.getType() != Material.GHAST_TEAR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.contains("Bloodpine Heart");
    }
}
