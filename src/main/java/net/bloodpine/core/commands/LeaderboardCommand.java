package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class LeaderboardCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public LeaderboardCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<PlayerData> topPlayers = plugin.getDataManager().getTopPlayers(10);
        
        sender.sendMessage(colorize("&c&m-----&r &c&lToken Leaderboard &c&m-----"));
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(colorize("&7No players found!"));
        } else {
            int position = 1;
            for (PlayerData data : topPlayers) {
                String prefix = getPrefix(position);
                String marked = data.isMarked() ? " &c⚠" : "";
                
                sender.sendMessage(colorize(prefix + " &f" + data.getName() + 
                    " &7- &e" + data.getTotalTokens() + " tokens" + marked));
                position++;
            }
        }
        
        sender.sendMessage(colorize("&c&m---------------------------"));
        
        return true;
    }
    
    private String getPrefix(int position) {
        return switch (position) {
            case 1 -> "&6&l#1";
            case 2 -> "&7&l#2";
            case 3 -> "&c&l#3";
            default -> "&7#" + position;
        };
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
