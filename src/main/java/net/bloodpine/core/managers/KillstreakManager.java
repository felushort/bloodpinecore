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
        plugin.getDataManager().getPlayerData(killer).recordKillstreak(streak);

        if (streak == 5) {
            int bonusTokens = plugin.getConfig().getInt("killstreak.bonus-at-5", 2);
            plugin.getTokenManager().giveTokens(killer, bonusTokens);
            killer.sendMessage(colorize("&6Killstreak &7» &a5 streak reached: &e+" + bonusTokens + " tokens"));
            killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
        } else if (streak == 10) {
            int glowSeconds = plugin.getConfig().getInt("killstreak.glow-seconds-at-10", 30);
            int regenSeconds = plugin.getConfig().getInt("killstreak.regen-seconds-at-10", 30);

            killer.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, glowSeconds * 20, 0, true, false, true));
            killer.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION, regenSeconds * 20, 0, true, true, true));

            killer.sendMessage(colorize("&dKillstreak &7» &a10 streak: temporary glow + regen activated"));
            killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else if (streak == 15) {
            Bukkit.broadcastMessage(colorize("&c&lKILLSTREAK &7» &f" + killer.getName() + " reached a &c15 kill streak&7!"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1f);
            }
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
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
