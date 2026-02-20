package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public GUIListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if it's one of our GUIs
        if (!title.contains("Bloodpine") && !title.contains("Leaderboard") && !title.contains("Allocate") && !title.contains("Token Shop")) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        // Main menu handling
        if (title.contains("Bloodpine Ascension")) {
            handleMainMenu(player, itemName, clicked);
        }
        // Allocate menu handling
        else if (title.contains("Allocate")) {
            handleAllocateMenu(player, title, itemName);
        }
        // Leaderboard handling
        else if (title.contains("Leaderboard")) {
            handleLeaderboard(player, itemName);
        }
        else if (title.contains("Token Shop")) {
            handleTokenShop(player, itemName);
        }
    }
    
    private void handleMainMenu(Player player, String itemName, ItemStack clicked) {
        if (itemName.contains("Damage")) {
            plugin.getStatsGUI().openAllocateMenu(player, StatType.DAMAGE);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Defense")) {
            plugin.getStatsGUI().openAllocateMenu(player, StatType.DEFENSE);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Totem")) {
            plugin.getStatsGUI().openAllocateMenu(player, StatType.TOTEM);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Vitality")) {
            plugin.getStatsGUI().openAllocateMenu(player, StatType.VITALITY);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Leaderboard")) {
            plugin.getLeaderboardGUI().openLeaderboard(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Token Shop")) {
            plugin.getTokenShopGUI().open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Rebirth")) {
            player.closeInventory();
            player.performCommand("rebirth info");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        else if (itemName.contains("Reset")) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            int refunded = data.getAllocatedTokenCost();
            
            if (refunded == 0) {
                player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cYou have no allocated tokens!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            
            plugin.getStatManager().resetStats(player);
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aReset stats! Refunded &e" + refunded + " tokens"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            plugin.getStatsGUI().openMainMenu(player);
        }
    }
    
    private void handleAllocateMenu(Player player, String title, String itemName) {
        // Get stat type from title
        StatType statType = null;
        for (StatType type : StatType.values()) {
            if (title.contains(type.getDisplayName())) {
                statType = type;
                break;
            }
        }
        
        if (statType == null) return;
        
        if (itemName.contains("Back")) {
            plugin.getStatsGUI().openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }
        
        int amount = 1;
        if (itemName.contains("+5")) {
            amount = 5;
        } else if (itemName.contains("+10")) {
            amount = 10;
        }
        
        if (itemName.contains("+")) {
            int cost = plugin.getStatManager().getAllocationCost(player, statType, amount);
            boolean success = plugin.getStatManager().allocateTokens(player, statType, amount);
            
            if (success) {
                player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&aAllocated &e" + amount + " point(s) &ato &f" + statType.getDisplayName() + " &7for &e" + cost + " tokens"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                plugin.getDisplayManager().updateDisplay(player);
                plugin.getStatsGUI().openAllocateMenu(player, statType);
            } else {
                PlayerData data = plugin.getDataManager().getPlayerData(player);
                int available = data.getAvailableTokens();
                int current = plugin.getStatManager().getAllocatedTokens(player, statType);
                int max = plugin.getStatManager().getMaxTokensForStat(statType);
                
                if (cost > 0 && available < cost) {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cNot enough tokens! Need &e" + cost + " &ctokens, you only have &e" + available));
                } else if (max > 0 && current + amount > max) {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cCannot allocate! Max is &e" + max + " &cand you have &e" + current));
                } else {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") +
                        "&cCannot allocate that many points right now."));
                }
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
    
    private void handleLeaderboard(Player player, String itemName) {
        if (itemName.contains("Back")) {
            plugin.getStatsGUI().openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    private void handleTokenShop(Player player, String itemName) {
        plugin.getTokenShopGUI().handleClick(player, itemName);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getTokenShopGUI().open(player));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
