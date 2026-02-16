package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveTokenCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public GiveTokenCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bloodpine.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /give <player> <amount>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(colorize("&cPlayer not found!"));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                sender.sendMessage(colorize("&cAmount must be positive!"));
                return true;
            }

            plugin.getTokenManager().giveTokens(target, amount);
            sender.sendMessage(colorize("&aGave &e" + amount + " tokens &ato &f" + target.getName()));
            target.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "&c&lBloodpine &7» ") +
                    "&aYou received &e" + amount + " tokens&a!"));
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid amount! Usage: /give <player> <amount>"));
        }

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
