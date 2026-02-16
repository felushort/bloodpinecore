package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks detailed combat statistics for players
 */
public class CombatStatsManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, CombatStats> playerStats;
    
    public CombatStatsManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
    }
    
    /**
     * Record damage dealt by a player
     */
    public void recordDamageDealt(Player player, double damage) {
        if (player == null) return;
        
        CombatStats stats = getStats(player.getUniqueId());
        stats.damageDealt += damage;
        stats.hitsLanded++;
    }
    
    /**
     * Record damage taken by a player
     */
    public void recordDamageTaken(Player player, double damage) {
        if (player == null) return;
        
        CombatStats stats = getStats(player.getUniqueId());
        stats.damageTaken += damage;
        stats.hitsTaken++;
    }
    
    /**
     * Record a critical hit
     */
    public void recordCriticalHit(Player player) {
        if (player == null) return;
        
        CombatStats stats = getStats(player.getUniqueId());
        stats.criticalHits++;
    }
    
    /**
     * Get combat stats for a player
     */
    public CombatStats getStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new CombatStats());
    }
    
    /**
     * Get combat stats for a player
     */
    public CombatStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }
    
    /**
     * Reset stats for a player
     */
    public void resetStats(UUID uuid) {
        playerStats.remove(uuid);
    }
    
    /**
     * Class to hold combat statistics
     */
    public static class CombatStats {
        private double damageDealt = 0;
        private double damageTaken = 0;
        private int hitsLanded = 0;
        private int hitsTaken = 0;
        private int criticalHits = 0;
        
        public double getDamageDealt() {
            return damageDealt;
        }
        
        public double getDamageTaken() {
            return damageTaken;
        }
        
        public int getHitsLanded() {
            return hitsLanded;
        }
        
        public int getHitsTaken() {
            return hitsTaken;
        }
        
        public int getCriticalHits() {
            return criticalHits;
        }
        
        public double getAverageDamagePerHit() {
            return hitsLanded > 0 ? damageDealt / hitsLanded : 0;
        }
        
        public double getCriticalHitRate() {
            return hitsLanded > 0 ? (double) criticalHits / hitsLanded : 0;
        }
        
        public double getDamageRatio() {
            return damageTaken > 0 ? damageDealt / damageTaken : damageDealt;
        }
    }
}
