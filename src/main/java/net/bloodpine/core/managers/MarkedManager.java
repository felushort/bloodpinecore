package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MarkedManager {
    
    private final BloodpineCore plugin;
    
    public MarkedManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    public void checkMarkedPlayers() {
        int threshold = plugin.getConfig().getInt("marked.threshold", 25);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            
            if (data.getTotalTokens() >= threshold && !data.isMarked()) {
                markPlayer(player);
            } else if (data.getTotalTokens() < threshold && data.isMarked()) {
                unmarkPlayer(player);
            }
            
            // Apply glow effect if marked
            if (data.isMarked() && plugin.getConfig().getBoolean("marked.glow", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false));
            }
        }
    }
    
    public void markPlayer(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.setMarked(true);
        
        // Send message to player
        String message = plugin.getConfig().getString("messages.marked", 
            "&c&lYou are now MARKED! &7You have {tokens} tokens and a bounty on your head.");
        message = message.replace("{tokens}", String.valueOf(data.getTotalTokens()));
        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + message));
        
        // Broadcast if enabled
        if (plugin.getConfig().getBoolean("marked.broadcast", true)) {
            int bounty = getBounty(player);
            String broadcast = plugin.getConfig().getString("messages.marked-broadcast", 
                "&c&l{player} IS NOW MARKED! &7Kill them for &e{bounty} tokens!");
            broadcast = broadcast
                .replace("{player}", player.getName())
                .replace("{bounty}", String.valueOf(bounty));
            
            Bukkit.broadcastMessage(colorize(plugin.getConfig().getString("messages.prefix") + broadcast));
        }
        
        // Apply glow effect
        if (plugin.getConfig().getBoolean("marked.glow", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 0, false, false));
        }
        
        // Update display
        plugin.getDisplayManager().updateDisplay(player);
    }
    
    public void unmarkPlayer(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.setMarked(false);
        
        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
            "&aYou are no longer marked!"));
        
        // Remove glow effect
        player.removePotionEffect(PotionEffectType.GLOWING);
        
        // Update display
        plugin.getDisplayManager().updateDisplay(player);
    }
    
    public int getBounty(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (!data.isMarked()) {
            return 0;
        }
        
        double multiplier = plugin.getConfig().getDouble("marked.bounty-multiplier", 2.0);
        return (int) (data.getTotalTokens() * multiplier);
    }
    
    public boolean isMarked(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.isMarked();
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
