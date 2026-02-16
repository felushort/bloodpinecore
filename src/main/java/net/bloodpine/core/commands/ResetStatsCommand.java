package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetStatsCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public ResetStatsCommand(BloodpineCore plugin) {
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
        
        int refundedTokens = data.getTotalAllocatedTokens();
        
        if (refundedTokens == 0) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cYou have no allocated tokens to reset!"));
            return true;
        }
        
        // Reset stats
        plugin.getStatManager().resetStats(player);
        
        // Send message
        String message = plugin.getConfig().getString("messages.reset-stats")
            .replace("{tokens}", String.valueOf(refundedTokens));
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + message));
        
        // Update display
        plugin.getDisplayManager().updateDisplay(player);
        
        return true;
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
