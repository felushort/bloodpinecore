package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatLogListener implements Listener {
    
    private final BloodpineCore plugin;
    private final Map<UUID, Long> combatTag;
    private final Map<UUID, Player> lastAttacker;
    
    public CombatLogListener(BloodpineCore plugin) {
        this.plugin = plugin;
        this.combatTag = new HashMap<>();
        this.lastAttacker = new HashMap<>();
    }
    
    public void tagPlayer(Player player, Player attacker) {
        UUID uuid = player.getUniqueId();
        combatTag.put(uuid, System.currentTimeMillis());
        lastAttacker.put(uuid, attacker);
        
        if (!isInCombat(player)) {
            player.sendMessage(colorize("&c&l⚔ You are now in combat! Don't log out!"));
        }
    }
    
    public boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTag.containsKey(uuid)) {
            return false;
        }
        
        long tagTime = combatTag.get(uuid);
        long currentTime = System.currentTimeMillis();
        
        // Combat tag lasts 15 seconds
        if (currentTime - tagTime > 15000) {
            removeCombatTag(player);
            return false;
        }
        
        return true;
    }
    
    public void removeCombatTag(Player player) {
        UUID uuid = player.getUniqueId();
        combatTag.remove(uuid);
        lastAttacker.remove(uuid);
        player.sendMessage(colorize("&a&l✓ You are no longer in combat."));
    }

    public void clearPlayerState(UUID uuid) {
        combatTag.remove(uuid);
        lastAttacker.remove(uuid);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (isInCombat(player)) {
            // Kill the player for combat logging
            Location deathLoc = player.getLocation();
            player.setHealth(0);
            
            // Give credit to the attacker
            Player attacker = lastAttacker.get(player.getUniqueId());
            if (attacker != null && attacker.isOnline()) {
                plugin.getTokenManager().handleKillReward(attacker, player);
                plugin.getKillstreakManager().addKill(attacker);
                attacker.sendMessage(colorize("&c" + player.getName() + " &7combat logged! You gained tokens!"));
            }
            
            Bukkit.broadcastMessage(colorize("&c&l" + player.getName() + " combat logged and died!"));
            
            // Remove combat tag
            combatTag.remove(player.getUniqueId());
            lastAttacker.remove(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        removeCombatTag(player);
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
