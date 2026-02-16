package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class DisplayManager {
    
    private final BloodpineCore plugin;
    private Scoreboard scoreboard;
    
    public DisplayManager(BloodpineCore plugin) {
        this.plugin = plugin;
        setupScoreboard();
    }
    
    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }
    
    public void updateAllDisplays() {
        if (!plugin.getConfig().getBoolean("display.show-stats", true)) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplay(player);
        }
    }
    
    public void updateDisplay(Player player) {
        if (!plugin.getConfig().getBoolean("display.show-stats", true)) {
            return;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Get stat values
        int damage = data.getAllocatedTokens(StatType.DAMAGE);
        int defense = data.getAllocatedTokens(StatType.DEFENSE);
        int vitality = data.getAllocatedTokens(StatType.VITALITY);
        int crystal = data.getAllocatedTokens(StatType.CRYSTAL);
        int totem = data.getAllocatedTokens(StatType.TOTEM);
        
        // Format display
        String format = plugin.getConfig().getString("display.format", 
            "&c{damage} &7| &9{defense} &7| &a{vitality}");
        
        String display = format
            .replace("{damage}", String.valueOf(damage))
            .replace("{defense}", String.valueOf(defense))
            .replace("{vitality}", String.valueOf(vitality))
            .replace("{crystal}", String.valueOf(crystal))
            .replace("{totem}", String.valueOf(totem))
            .replace("{tokens}", String.valueOf(data.getTotalTokens()));
        
        display = colorize(display);
        
        // Add marked indicator if player is marked
        if (data.isMarked()) {
            display = ChatColor.RED + "⚠ " + display + ChatColor.RED + " ⚠";
        }
        
        // Set player display name with stats
        player.setPlayerListName(colorize("&f" + player.getName() + " &7[" + display + "&7]"));
        
        // Update team prefix/suffix for nametag
        String teamName = "bp_" + player.getName().substring(0, Math.min(12, player.getName().length()));
        Team team = scoreboard.getTeam(teamName);
        
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        team.addEntry(player.getName());
        
        // Set prefix (limited to 64 chars in older versions, but we'll keep it short)
        String prefix = "";
        String suffix = colorize(" &7[" + display.substring(0, Math.min(display.length(), 32)) + "&7]");
        
        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }
    
    public void clearDisplay(Player player) {
        String teamName = "bp_" + player.getName().substring(0, Math.min(12, player.getName().length()));
        Team team = scoreboard.getTeam(teamName);
        
        if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
        }
        
        player.setPlayerListName(player.getName());
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
