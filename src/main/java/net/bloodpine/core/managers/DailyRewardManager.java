package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages daily login rewards for players
 */
public class DailyRewardManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, LocalDate> lastClaim;
    private final Map<UUID, Integer> streakDays;
    
    public DailyRewardManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.lastClaim = new ConcurrentHashMap<>();
        this.streakDays = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if player can claim daily reward
     */
    public boolean canClaim(UUID uuid) {
        LocalDate last = lastClaim.get(uuid);
        if (last == null) {
            return true;
        }
        
        LocalDate today = LocalDate.now();
        return !last.equals(today);
    }
    
    /**
     * Claim daily reward
     */
    public boolean claimReward(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        
        if (!canClaim(uuid)) {
            long hoursUntil = getHoursUntilNextClaim(uuid);
            player.sendMessage(colorize("&cYou've already claimed your daily reward! Next reward in &e" + hoursUntil + " hours&c."));
            return false;
        }
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate last = lastClaim.get(uuid);
            
            // Update streak
            int streak = streakDays.getOrDefault(uuid, 0);
            if (last != null && ChronoUnit.DAYS.between(last, today) == 1) {
                // Consecutive day
                streak++;
            } else if (last != null && ChronoUnit.DAYS.between(last, today) > 1) {
                // Streak broken
                streak = 1;
            } else {
                // First claim
                streak = 1;
            }
            
            streakDays.put(uuid, streak);
            lastClaim.put(uuid, today);
            
            // Calculate rewards
            int baseTokens = plugin.getConfig().getInt("daily-rewards.base-tokens", 3);
            int streakBonus = Math.min(streak - 1, 6); // Max +6 bonus
            int totalTokens = baseTokens + streakBonus;
            
            // Apply rewards
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            data.addTokens(totalTokens);
            
            // Notify player
            player.sendMessage(colorize("&a&l✦ DAILY REWARD CLAIMED! ✦"));
            player.sendMessage(colorize("&7Tokens: &e+" + totalTokens + " &7(Base: &e" + baseTokens + "&7, Streak Bonus: &e+" + streakBonus + "&7)"));
            player.sendMessage(colorize("&7Login Streak: &a" + streak + " day" + (streak != 1 ? "s" : "")));
            
            if (streak >= 7) {
                player.sendMessage(colorize("&6&l✦ Week Streak Milestone! &7Keep it up!"));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            
            plugin.getLogger().info(player.getName() + " claimed daily reward: " + totalTokens + " tokens (streak: " + streak + ")");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error claiming daily reward for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(colorize("&cAn error occurred while claiming your daily reward."));
            return false;
        }
    }
    
    /**
     * Get hours until next claim
     */
    public long getHoursUntilNextClaim(UUID uuid) {
        LocalDate last = lastClaim.get(uuid);
        if (last == null) {
            return 0;
        }
        
        LocalDate today = LocalDate.now();
        if (last.isBefore(today)) {
            return 0;
        }
        
        // Calculate hours until midnight
        LocalDate tomorrow = today.plusDays(1);
        long daysUntil = ChronoUnit.DAYS.between(today, tomorrow);
        return daysUntil * 24;
    }
    
    /**
     * Get current streak for player
     */
    public int getStreak(UUID uuid) {
        return streakDays.getOrDefault(uuid, 0);
    }
    
    /**
     * Load daily reward data
     */
    public void loadData(UUID uuid, String lastClaimDate, int streak) {
        try {
            if (lastClaimDate != null && !lastClaimDate.isEmpty()) {
                lastClaim.put(uuid, LocalDate.parse(lastClaimDate));
            }
            if (streak > 0) {
                streakDays.put(uuid, streak);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading daily reward data for " + uuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Get last claim date for saving
     */
    public String getLastClaimDate(UUID uuid) {
        LocalDate date = lastClaim.get(uuid);
        return date != null ? date.toString() : "";
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
