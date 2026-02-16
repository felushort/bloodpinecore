package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public KillListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Only process PvP kills
        if (killer == null || killer.equals(victim)) {
            return;
        }
        
        try {
            // Handle killstreaks
            plugin.getKillstreakManager().addKill(killer);
            plugin.getKillstreakManager().endKillstreak(victim);
            
            // Handle bounties
            plugin.getBountyManager().claimBounty(killer, victim);
            
            // Handle token rewards
            plugin.getTokenManager().handleKillReward(killer, victim);
            
            // Handle kill effects (lightning strike on victim's death location)
            if (plugin.getBoostManager().hasKillEffect(killer)) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
            }
            
            // Check and award achievements
            plugin.getAchievementManager().checkAchievements(killer);
            
            // Reset combat stats for the victim
            plugin.getCombatStatsManager().resetStats(victim.getUniqueId());
            
            // Update displays
            plugin.getDisplayManager().updateDisplay(killer);
            plugin.getDisplayManager().updateDisplay(victim);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player kill: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
