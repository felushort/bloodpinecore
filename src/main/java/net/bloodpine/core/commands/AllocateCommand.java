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
        
        boolean success = plugin.getStatManager().allocateTokens(player, statType, amount);
        
        if (!success) {
            int available = plugin.getDataManager().getPlayerData(player).getAvailableTokens();
            int current = plugin.getStatManager().getAllocatedTokens(player, statType);
            int max = plugin.getStatManager().getMaxTokensForStat(statType);
            
            if (available < amount) {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.insufficient-tokens")));
            } else if (current + amount > max) {
                String msg = plugin.getConfig().getString("messages.max-stat")
                    .replace("{stat}", statType.getDisplayName());
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + msg));
            }
            return true;
        }
        
        String message = plugin.getConfig().getString("messages.stat-allocated")
            .replace("{amount}", String.valueOf(amount))
            .replace("{stat}", statType.getDisplayName());
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + message));
        
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
