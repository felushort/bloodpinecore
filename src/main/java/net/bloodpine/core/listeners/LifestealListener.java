package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Date;

public class LifestealListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public LifestealListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Only process PvP deaths for lifesteal
        if (killer == null || killer.equals(victim)) {
            return;
        }
        
        // Update lifesteal hearts in PlayerData
        net.bloodpine.core.data.PlayerData victimData = plugin.getDataManager().getPlayerData(victim);
        net.bloodpine.core.data.PlayerData killerData = plugin.getDataManager().getPlayerData(killer);
        
        // Check for heart shield on victim
        boolean victimShielded = plugin.getBoostManager().consumeHeartShield(victim);
        boolean victimInsured = false;

        if (!victimShielded) {
            victimInsured = victimData.consumeInsuredHeart();
            if (!victimInsured) {
                victimData.removeLifestealHeart();

                int extraLoss = plugin.getGameplayExpansionManager().getExtraHeartLossInBloodZone(victim.getLocation());
                for (int i = 0; i < extraLoss; i++) {
                    victimData.removeLifestealHeart();
                }
            }
        }
        killerData.addLifestealHeart();
        plugin.getExpansionDataManager().getProfile(killer).addHeartsGainedSeason(1);
        
        // Apply stats (this recalculates max health properly)
        plugin.getStatManager().applyStats(victim);
        plugin.getStatManager().applyStats(killer);
        
        // Get the new max health values for messages
        double newVictimHealth = victim.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newKillerHealth = killer.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        
        // Messages
        if (victimShielded) {
            victim.sendMessage(colorize("&6&l🛡 Heart Shield saved your heart this death."));
        } else if (victimInsured) {
            victim.sendMessage(colorize("&b&l☂ Heart Insurance triggered. Your insured heart was consumed."));
        } else {
            victim.sendMessage(colorize("&c&l💔 You lost a heart! &7(" + (newVictimHealth / 2.0) + " hearts remaining)"));
        }
        killer.sendMessage(colorize("&a&l❤ You gained a heart! &7(" + (newKillerHealth / 2.0) + " hearts total)"));
        
        // Check if victim is eliminated (1 heart = 2.0 health)
        double minHealth = plugin.getConfig().getDouble("lifesteal.min-hearts", 1) * 2.0;
        if (newVictimHealth <= minHealth) {
            eliminatePlayer(victim);
        }
        
        // Broadcast
        Bukkit.broadcastMessage(colorize("&c" + victim.getName() + " &7lost a heart to &a" + killer.getName()));
    }
    
    private void eliminatePlayer(Player player) {
        // Ban until next season
        Bukkit.broadcastMessage(colorize("&c&l⚠ " + player.getName() + " HAS BEEN ELIMINATED! &7They are banned until next season."));
        
        // Get season end date (30 days from now as configured)
        int seasonLength = plugin.getConfig().getInt("season.length", 30);
        long banDuration = seasonLength * 24 * 60 * 60 * 1000L; // Convert days to milliseconds
        Date banExpiry = new Date(System.currentTimeMillis() + banDuration);
        
        // Ban the player
        Bukkit.getBanList(BanList.Type.NAME).addBan(
            player.getName(),
            ChatColor.RED + "You have been eliminated!\n" +
            ChatColor.GRAY + "You ran out of hearts.\n" +
            ChatColor.YELLOW + "See you next season!",
            banExpiry,
            "Bloodpine Lifesteal System"
        );
        
        // Kick player
        player.kickPlayer(colorize(
            "&c&l💀 ELIMINATED 💀\n\n" +
            "&7You have run out of hearts!\n" +
            "&7You are banned until the next season.\n\n" +
            "&eNext Season: &f" + new java.text.SimpleDateFormat("MMM dd, yyyy").format(banExpiry)
        ));
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
