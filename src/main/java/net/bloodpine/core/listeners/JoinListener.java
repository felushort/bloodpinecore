package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
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
        plugin.getExpansionDataManager().resetDailyIfNeeded(event.getPlayer());

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

        plugin.getGameplayExpansionManager().applySeasonStory(event.getPlayer());

        if (!event.getPlayer().hasPlayedBefore() && plugin.getConfig().getBoolean("onboarding.auto-open-learn-on-first-join", true)) {
            String welcome = plugin.getConfig().getString("onboarding.welcome-title", "Welcome to Bloodpine");
            event.getPlayer().sendMessage("§a§l" + welcome + "!");
            event.getPlayer().sendMessage("§7Type §f/hub §7for everything, or §f/learn §7for the beginner tutorial.");
            event.getPlayer().sendMessage("§7Fast start: §f/menu §7→ allocate stats, then join fights for tokens.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getExpansionGUIManager().openLearnCenter(event.getPlayer()), 40L);
        }

        if (!plugin.getAuthManager().isAuthRequired()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getSessionLocationManager().restoreLocation(event.getPlayer()), 2L);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSessionLocationManager().saveLocation(event.getPlayer());
        plugin.getSessionLocationManager().save();

        // Clear display
        plugin.getDisplayManager().clearDisplay(event.getPlayer());
        plugin.getSidebarManager().clearPlayer(event.getPlayer());
        
        // Save data
        plugin.getDataManager().saveData();
    }
}
