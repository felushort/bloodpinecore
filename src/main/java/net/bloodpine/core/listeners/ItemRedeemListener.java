package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemRedeemListener implements Listener {

    private final BloodpineCore plugin;

    public ItemRedeemListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check for right-click
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Check if it's a Token item
        if (ItemUtils.isTokenItem(item)) {
            event.setCancelled(true);
            redeemToken(player, item);
            return;
        }
        
        // Check if it's a Heart item
        if (ItemUtils.isHeartItem(item)) {
            event.setCancelled(true);
            redeemHeart(player, item);
            return;
        }
    }

    private void redeemToken(Player player, ItemStack item) {
        int amount = item.getAmount();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Check if player can receive tokens
        int currentTokens = data.getTotalTokens();
        int maxTokens = plugin.getConfig().getInt("tokens.max-total", 50);
        int availableSpace = maxTokens - currentTokens;
        
        if (availableSpace <= 0) {
            player.sendMessage(colorize("&cYou've reached the maximum token limit! &7(" + maxTokens + " tokens)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Calculate how many tokens can be redeemed
        int tokensToRedeem = Math.min(amount, availableSpace);
        
        // Remove items from inventory
        if (tokensToRedeem == amount) {
            item.setAmount(0); // Remove all
        } else {
            item.setAmount(amount - tokensToRedeem);
        }
        
        // Add tokens to player
        plugin.getTokenManager().giveTokens(player, tokensToRedeem);
        
        // Play sound and send message
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.sendMessage(colorize("&a&l✓ &aRedeemed &e" + tokensToRedeem + " Token" + (tokensToRedeem > 1 ? "s" : "") + "&a!"));
        
        if (tokensToRedeem < amount) {
            int remaining = amount - tokensToRedeem;
            player.sendMessage(colorize("&eYou could only redeem &6" + tokensToRedeem + "/" + amount + " &etokens due to the cap."));
        }
        
        // Update displays
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);
    }

    private void redeemHeart(Player player, ItemStack item) {
        int amount = item.getAmount();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Check if player can receive hearts
        double maxHearts = plugin.getConfig().getDouble("lifesteal.max-hearts", 20.0);
        double currentHearts = data.getLifestealHearts();
        double availableSpace = maxHearts - currentHearts;
        
        if (availableSpace <= 0) {
            player.sendMessage(colorize("&cYou've reached the maximum heart limit! &7(" + (int)maxHearts + " hearts)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Calculate how many hearts can be redeemed
        int heartsToRedeem = Math.min(amount, (int)availableSpace);
        
        // Remove items from inventory
        if (heartsToRedeem == amount) {
            item.setAmount(0); // Remove all
        } else {
            item.setAmount(amount - heartsToRedeem);
        }
        
        // Add hearts to player
        for (int i = 0; i < heartsToRedeem; i++) {
            data.addLifestealHeart();
        }
        
        // Apply stats
        plugin.getStatManager().applyStats(player);
        
        // Play sound and send message
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        player.sendMessage(colorize("&c&l❤ &aAbsorbed &c" + heartsToRedeem + " Heart" + (heartsToRedeem > 1 ? "s" : "") + "&a!"));
        
        if (heartsToRedeem < amount) {
            int remaining = amount - heartsToRedeem;
            player.sendMessage(colorize("&eYou could only absorb &6" + heartsToRedeem + "/" + amount + " &ehearts due to the cap."));
        }
        
        // Update displays
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
