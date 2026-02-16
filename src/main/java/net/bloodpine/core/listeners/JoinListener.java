package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public JoinListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data
        PlayerData data = plugin.getDataManager().getPlayerData(event.getPlayer());
        
        // Apply stats (handles starting hearts, lifesteal hearts, and vitality)
        plugin.getStatManager().applyStats(event.getPlayer());
        
        // Update display
        plugin.getDisplayManager().updateDisplay(event.getPlayer());
        
        // Check if should be marked
        plugin.getMarkedManager().checkMarkedPlayers();

        // Update right-side sidebar
        plugin.getSidebarManager().updatePlayer(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear display
        plugin.getDisplayManager().clearDisplay(event.getPlayer());
        plugin.getSidebarManager().clearPlayer(event.getPlayer());
        
        // Save data
        plugin.getDataManager().saveData();
    }
}
