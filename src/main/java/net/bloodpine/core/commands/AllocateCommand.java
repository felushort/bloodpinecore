package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.StatType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllocateCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public AllocateCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        Player player = (Player) sender;
        StatType statType = StatType.fromString(args[0]);
        
        if (statType == null) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                "&cInvalid stat type! Valid types: damage, defense, totem, vitality"));
            return true;
        }

        if (statType == StatType.CRYSTAL) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") +
                "&cCrystal stat is disabled on this server."));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cAmount must be greater than 0!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cInvalid amount! Please enter a number."));
                return true;
            }
        }
        
        int totalCost = plugin.getStatManager().getAllocationCost(player, statType, amount);
        boolean success = plugin.getStatManager().allocateTokens(player, statType, amount);
        
        if (!success) {
            int available = plugin.getDataManager().getPlayerData(player).getAvailableTokens();
            int current = plugin.getStatManager().getAllocatedTokens(player, statType);
            int max = plugin.getStatManager().getMaxTokensForStat(statType);
            
            if (totalCost > 0 && available < totalCost) {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") +
                    "&cNot enough tokens! Need &e" + totalCost + " &ctokens, you have &e" + available));
            } else if (max > 0 && current + amount > max) {
                String msg = plugin.getConfig().getString("messages.max-stat")
                    .replace("{stat}", statType.getDisplayName());
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + msg));
            } else {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") +
                    "&cCannot allocate that amount right now."));
            }
            return true;
        }
        
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") +
            "&aAllocated &e" + amount + " point(s) &ato &f" + statType.getDisplayName() + " &7for &e" + totalCost + " tokens"));
        
        // Update display
        plugin.getDisplayManager().updateDisplay(player);
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&c&lStat Allocation"));
        sender.sendMessage(colorize("&7Usage: &e/allocate <stat> [amount]"));
        sender.sendMessage(colorize("&7Stats:"));
        sender.sendMessage(colorize("  &edamage &7- Increase sword damage"));
        sender.sendMessage(colorize("  &edefense &7- Reduce damage taken"));
        sender.sendMessage(colorize("  &etotem &7- Reduce totem cooldown"));
        sender.sendMessage(colorize("  &evitality &7- Increase max health"));
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
