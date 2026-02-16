package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TokensCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public TokensCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return true;
        }
        
        Player player = (Player) sender;
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        sender.sendMessage(colorize("&c&m-----&r &c&lBloodpine Tokens &c&m-----"));
        sender.sendMessage(colorize("&7Total Tokens: &e" + data.getTotalTokens()));
        sender.sendMessage(colorize("&7Allocated: &e" + data.getTotalAllocatedTokens()));
        sender.sendMessage(colorize("&7Available: &a" + data.getAvailableTokens()));
        sender.sendMessage(colorize("&7Kills: &e" + data.getTotalKills()));
        sender.sendMessage(colorize("&7Deaths: &e" + data.getTotalDeaths()));
        sender.sendMessage(colorize("&7K/D Ratio: &e" + String.format("%.2f", data.getKDRatio())));
        
        if (data.isMarked()) {
            sender.sendMessage(colorize("&c&lSTATUS: MARKED"));
        }
        
        sender.sendMessage(colorize("&c&m---------------------------"));
        sender.sendMessage(colorize("&7Use &e/allocate <stat> <amount> &7to allocate tokens"));
        
        return true;
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
