package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TokenManager {
    
    private final BloodpineCore plugin;
    
    public TokenManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    public void giveTokens(Player player, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int currentTokens = data.getTotalTokens();
        int maxTokens = plugin.getConfig().getInt("tokens.max-total", 50);
        
        int tokensToGive = Math.min(amount, maxTokens - currentTokens);
        
        if (tokensToGive <= 0) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cYou've reached the maximum token limit!"));
            return;
        }
        
        data.addTokens(tokensToGive);
        
        // Check if player should become marked
        if (!data.isMarked()) {
            int markedThreshold = plugin.getConfig().getInt("marked.threshold", 25);
            if (data.getTotalTokens() >= markedThreshold) {
                plugin.getMarkedManager().markPlayer(player);
            }
        }
    }
    
    public void removeTokens(Player player, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.removeTokens(amount);
        
        // Check if player should be unmarked
        if (data.isMarked()) {
            int markedThreshold = plugin.getConfig().getInt("marked.threshold", 25);
            if (data.getTotalTokens() < markedThreshold) {
                plugin.getMarkedManager().unmarkPlayer(player);
            }
        }
    }
    
    public void setTokens(Player player, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int maxTokens = plugin.getConfig().getInt("tokens.max-total", 50);
        data.setTotalTokens(Math.min(amount, maxTokens));
    }
    
    public int getTokens(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.getTotalTokens();
    }
    
    public int getAvailableTokens(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.getAvailableTokens();
    }
    
    public void handleKillReward(Player killer, Player victim) {
        int tokensPerKill = plugin.getConfig().getInt("tokens.per-kill", 1);
        
        // Apply boost multiplier if active
        int multiplier = plugin.getBoostManager().getTokenMultiplier(killer);
        int totalTokens = tokensPerKill * multiplier;
        
        // Add tokens to killer
        giveTokens(killer, totalTokens);
        
        // Send message to killer
        String boostMsg = multiplier > 1 ? " &6&l(2x BOOST!)" : "";
        String message = "&aYou earned &e" + totalTokens + " Token(s) &afrom killing &f" + victim.getName() + "&a!" + boostMsg;
        killer.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + message));
        
        // Increment kill counter
        PlayerData killerData = plugin.getDataManager().getPlayerData(killer);
        killerData.addKill();
        
        // Handle victim
        PlayerData victimData = plugin.getDataManager().getPlayerData(victim);
        victimData.addDeath();
        
        // If victim was marked, they lose tokens
        if (victimData.isMarked()) {
            int penalty = plugin.getConfig().getInt("tokens.marked-death-penalty", 5);
            removeTokensFromData(victimData, penalty);
            
            victim.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cYou lost &e" + penalty + " tokens &cfor dying while marked!"));
            
            // Give bonus to killer (also boosted)
            int bonusTokens = penalty * multiplier;
            killer.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&a&lBONUS! &aYou killed a marked player and earned &e" + bonusTokens + " extra tokens!" + boostMsg));
            giveTokens(killer, bonusTokens);
        }
    }
    
    private void removeTokensFromData(PlayerData data, int amount) {
        data.removeTokens(amount);
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
