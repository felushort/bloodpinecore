package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public BountyCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show player's own bounty
            int bounty = plugin.getBountyManager().getBounty(player);
            if (bounty > 0) {
                player.sendMessage(colorize("&c&lYour Bounty: &e" + bounty + " tokens"));
            } else {
                player.sendMessage(colorize("&7You have no bounty on your head."));
            }
            return true;
        }
        
        if (args.length == 1) {
            // Check another player's bounty
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(colorize("&cPlayer not found!"));
                return true;
            }
            
            int bounty = plugin.getBountyManager().getBounty(target);
            if (bounty > 0) {
                player.sendMessage(colorize("&c" + target.getName() + "'s Bounty: &e" + bounty + " tokens"));
            } else {
                player.sendMessage(colorize("&7" + target.getName() + " has no bounty."));
            }
            return true;
        }
        
        if (args.length >= 2) {
            // Place a bounty
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(colorize("&cPlayer not found!"));
                return true;
            }
            
            if (target.equals(player)) {
                player.sendMessage(colorize("&cYou cannot place a bounty on yourself!"));
                return true;
            }
            
            try {
                int amount = Integer.parseInt(args[1]);
                plugin.getBountyManager().placeBounty(target, player, amount);
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&cInvalid amount! Use: /bounty <player> <amount>"));
            }
            
            return true;
        }
        
        return false;
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
