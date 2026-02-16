package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages command cooldowns to prevent spam
 */
public class CooldownManager {
    
    private final BloodpineCore plugin;
    private final Map<String, Map<UUID, Long>> cooldowns;
    
    public CooldownManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if player is on cooldown for a command
     * @param player Player to check
     * @param commandName Command name
     * @param cooldownSeconds Cooldown duration in seconds
     * @return true if on cooldown, false if can use
     */
    public boolean isOnCooldown(Player player, String commandName, int cooldownSeconds) {
        if (player == null || commandName == null) {
            return false;
        }
        
        // Bypass for ops/admins
        if (player.hasPermission("bloodpine.bypass.cooldown")) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        Map<UUID, Long> commandCooldowns = cooldowns.computeIfAbsent(commandName, k -> new ConcurrentHashMap<>());
        
        Long lastUsed = commandCooldowns.get(uuid);
        if (lastUsed == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        
        return (currentTime - lastUsed) < cooldownMillis;
    }
    
    /**
     * Set cooldown for player
     * @param player Player
     * @param commandName Command name
     */
    public void setCooldown(Player player, String commandName) {
        if (player == null || commandName == null) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Map<UUID, Long> commandCooldowns = cooldowns.computeIfAbsent(commandName, k -> new ConcurrentHashMap<>());
        commandCooldowns.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * Get remaining cooldown time in seconds
     * @param player Player
     * @param commandName Command name
     * @param cooldownSeconds Total cooldown duration
     * @return Remaining seconds, or 0 if no cooldown
     */
    public int getRemainingCooldown(Player player, String commandName, int cooldownSeconds) {
        if (player == null || commandName == null) {
            return 0;
        }
        
        UUID uuid = player.getUniqueId();
        Map<UUID, Long> commandCooldowns = cooldowns.get(commandName);
        
        if (commandCooldowns == null) {
            return 0;
        }
        
        Long lastUsed = commandCooldowns.get(uuid);
        if (lastUsed == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        long elapsed = currentTime - lastUsed;
        
        if (elapsed >= cooldownMillis) {
            return 0;
        }
        
        return (int) ((cooldownMillis - elapsed) / 1000);
    }
    
    /**
     * Clear cooldown for player
     * @param player Player
     * @param commandName Command name
     */
    public void clearCooldown(Player player, String commandName) {
        if (player == null || commandName == null) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Map<UUID, Long> commandCooldowns = cooldowns.get(commandName);
        
        if (commandCooldowns != null) {
            commandCooldowns.remove(uuid);
        }
    }
    
    /**
     * Clear all cooldowns for a player
     * @param player Player
     */
    public void clearAllCooldowns(Player player) {
        if (player == null) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.remove(uuid);
        }
    }
    
    /**
     * Clean up expired cooldowns periodically
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > 3600000 // Remove after 1 hour
            );
        }
    }
}
