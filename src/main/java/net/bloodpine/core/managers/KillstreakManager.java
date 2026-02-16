package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillstreakManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, Integer> killstreaks;
    private final Map<UUID, Long> lastKillTime;
    
    public KillstreakManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.killstreaks = new HashMap<>();
        this.lastKillTime = new HashMap<>();
    }
    
    public void addKill(Player killer) {
        UUID uuid = killer.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastKill = lastKillTime.getOrDefault(uuid, 0L);
        
        // Reset streak if more than 2 minutes since last kill
        if (currentTime - lastKill > 120000) {
            killstreaks.put(uuid, 1);
        } else {
            killstreaks.put(uuid, killstreaks.getOrDefault(uuid, 0) + 1);
        }
        
        lastKillTime.put(uuid, currentTime);
        int streak = killstreaks.get(uuid);
        
        // Broadcast killstreaks at milestones
        if (streak == 3) {
            broadcastKillstreak(killer, "TRIPLE KILL", Sound.ENTITY_ENDER_DRAGON_GROWL);
        } else if (streak == 5) {
            broadcastKillstreak(killer, "KILLING SPREE", Sound.ENTITY_WITHER_SPAWN);
            giveStreakBonus(killer, 1);
            plugin.getAchievementManager().unlockAchievement(killer, net.bloodpine.core.data.Achievement.KILLING_SPREE);
        } else if (streak == 10) {
            broadcastKillstreak(killer, "RAMPAGE", Sound.ENTITY_ENDER_DRAGON_DEATH);
            giveStreakBonus(killer, 2);
            plugin.getAchievementManager().unlockAchievement(killer, net.bloodpine.core.data.Achievement.UNSTOPPABLE);
        } else if (streak == 15) {
            broadcastKillstreak(killer, "UNSTOPPABLE", Sound.ENTITY_WITHER_DEATH);
            giveStreakBonus(killer, 3);
            plugin.getAchievementManager().unlockAchievement(killer, net.bloodpine.core.data.Achievement.GODLIKE);
        } else if (streak == 20) {
            broadcastKillstreak(killer, "GODLIKE", Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
            giveStreakBonus(killer, 5);
        }
    }
    
    public void endKillstreak(Player player) {
        UUID uuid = player.getUniqueId();
        int streak = killstreaks.getOrDefault(uuid, 0);
        
        if (streak >= 5) {
            Bukkit.broadcastMessage(colorize("&c&l" + player.getName() + "'s " + streak + " killstreak was ended!"));
            
            // Play sound to all players
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1f);
            }
        }
        
        killstreaks.remove(uuid);
        lastKillTime.remove(uuid);
    }
    
    public int getKillstreak(Player player) {
        return killstreaks.getOrDefault(player.getUniqueId(), 0);
    }

    public void clearPlayerState(UUID uuid) {
        killstreaks.remove(uuid);
        lastKillTime.remove(uuid);
    }
    
    private void broadcastKillstreak(Player player, String streakName, Sound sound) {
        String message = colorize("&c&l" + player.getName() + " &e&lis on a &c&l" + streakName + "&e&l!");
        Bukkit.broadcastMessage(message);
        
        // Play sound to all players
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), sound, 1f, 1f);
        }
    }
    
    private void giveStreakBonus(Player player, int bonusTokens) {
        plugin.getTokenManager().giveTokens(player, bonusTokens);
        player.sendMessage(colorize("&a&lSTREAK BONUS! &e+" + bonusTokens + " tokens!"));
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
