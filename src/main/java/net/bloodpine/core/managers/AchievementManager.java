package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.Achievement;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player achievements and rewards
 */
public class AchievementManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, Set<Achievement>> playerAchievements;
    
    public AchievementManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.playerAchievements = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if player has unlocked an achievement
     */
    public boolean hasAchievement(UUID uuid, Achievement achievement) {
        return playerAchievements.getOrDefault(uuid, new HashSet<>()).contains(achievement);
    }
    
    /**
     * Unlock an achievement for a player
     */
    public boolean unlockAchievement(Player player, Achievement achievement) {
        if (player == null || achievement == null) {
            return false;
        }
        
        Set<Achievement> achievements = playerAchievements.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        
        if (achievements.contains(achievement)) {
            return false; // Already unlocked
        }
        
        achievements.add(achievement);
        
        // Award tokens
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addTokens(achievement.getTokenReward());
        
        // Notify player
        broadcastAchievement(player, achievement);
        
        // Play sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        plugin.getLogger().info(player.getName() + " unlocked achievement: " + achievement.getName());
        
        return true;
    }
    
    /**
     * Broadcast achievement unlock to server
     */
    private void broadcastAchievement(Player player, Achievement achievement) {
        String message = ChatColor.translateAlternateColorCodes('&',
            "&6&l✦ &e" + player.getName() + " &7has unlocked &6&l" + achievement.getName() + 
            "&7! (+&e" + achievement.getTokenReward() + " tokens&7)");
        
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Check and award achievements based on player stats
     */
    public void checkAchievements(Player player) {
        if (player == null) {
            return;
        }
        
        try {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            
            // Kill-based achievements
            if (data.getTotalKills() >= 1) {
                unlockAchievement(player, Achievement.FIRST_BLOOD);
            }
            if (data.getTotalKills() >= 10) {
                unlockAchievement(player, Achievement.WARRIOR);
            }
            if (data.getTotalKills() >= 50) {
                unlockAchievement(player, Achievement.CHAMPION);
            }
            if (data.getTotalKills() >= 100) {
                unlockAchievement(player, Achievement.LEGEND);
            }
            
            // Token-based achievements
            if (data.getTotalTokens() >= 25) {
                unlockAchievement(player, Achievement.TOKEN_COLLECTOR);
            }
            if (data.getTotalTokens() >= 50) {
                unlockAchievement(player, Achievement.TOKEN_MASTER);
            }
            
            // Heart-based achievements
            if (data.getLifestealHearts() >= 20) {
                unlockAchievement(player, Achievement.SURVIVOR);
            }
            
            // Rebirth achievements
            if (data.getRebirthLevel() >= 1) {
                unlockAchievement(player, Achievement.REBIRTH_INITIATE);
            }
            
            // Stat allocation achievements
            checkStatAchievements(player, data);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking achievements for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Check stat-related achievements
     */
    private void checkStatAchievements(Player player, PlayerData data) {
        boolean hasAllStats = true;
        boolean hasMaxStat = false;
        
        for (net.bloodpine.core.data.StatType stat : net.bloodpine.core.data.StatType.values()) {
            int allocated = data.getAllocatedTokens(stat);
            int max = plugin.getStatManager().getMaxTokensForStat(stat);
            
            if (max > 0) {
                if (allocated == 0) {
                    hasAllStats = false;
                }
                if (allocated >= max) {
                    hasMaxStat = true;
                }
            }
        }
        
        if (hasMaxStat) {
            unlockAchievement(player, Achievement.STAT_SPECIALIST);
        }
        
        if (hasAllStats) {
            unlockAchievement(player, Achievement.BALANCED_BUILD);
        }
    }
    
    /**
     * Get all achievements for a player
     */
    public Set<Achievement> getAchievements(UUID uuid) {
        return new HashSet<>(playerAchievements.getOrDefault(uuid, new HashSet<>()));
    }
    
    /**
     * Get achievement progress for a player
     */
    public int getAchievementCount(UUID uuid) {
        return playerAchievements.getOrDefault(uuid, new HashSet<>()).size();
    }
    
    /**
     * Load achievements from data manager
     */
    public void loadAchievements(UUID uuid, Set<String> achievementKeys) {
        if (achievementKeys == null || achievementKeys.isEmpty()) {
            return;
        }
        
        Set<Achievement> achievements = new HashSet<>();
        for (String key : achievementKeys) {
            try {
                Achievement achievement = Achievement.valueOf(key.toUpperCase());
                achievements.add(achievement);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown achievement: " + key);
            }
        }
        
        playerAchievements.put(uuid, achievements);
    }
    
    /**
     * Get achievement keys for saving
     */
    public Set<String> getAchievementKeys(UUID uuid) {
        Set<Achievement> achievements = playerAchievements.getOrDefault(uuid, new HashSet<>());
        Set<String> keys = new HashSet<>();
        
        for (Achievement achievement : achievements) {
            keys.add(achievement.name());
        }
        
        return keys;
    }
}
