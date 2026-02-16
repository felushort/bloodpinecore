package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayTokensCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public PayTokensCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(colorize("&cUsage: /paytokens <player> <amount>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(colorize("&cPlayer not found or not online!"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(colorize("&cYou cannot send tokens to yourself!"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&cAmount must be a number."));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(colorize("&cAmount must be greater than 0."));
            return true;
        }

        PlayerData senderData = plugin.getDataManager().getPlayerData(player);
        PlayerData targetData = plugin.getDataManager().getPlayerData(target);

        int senderAvailable = senderData.getAvailableTokens();
        if (senderAvailable < amount) {
            player.sendMessage(colorize("&cYou only have &e" + senderAvailable + " &cavailable tokens to send."));
            return true;
        }

        int maxTokens = plugin.getConfig().getInt("tokens.max-total", 50);
        int targetRoom = maxTokens - targetData.getTotalTokens();
        if (targetRoom < amount) {
            player.sendMessage(colorize("&c" + target.getName() + " can only receive &e" + Math.max(targetRoom, 0) + "&c more tokens."));
            return true;
        }

        senderData.removeTokens(amount);
        targetData.addTokens(amount);

        plugin.getDisplayManager().updateDisplay(player);
        plugin.getDisplayManager().updateDisplay(target);
        plugin.getSidebarManager().updatePlayer(player);
        plugin.getSidebarManager().updatePlayer(target);

        player.sendMessage(colorize("&aSent &e" + amount + " &atokens to &f" + target.getName() + "&a."));
        target.sendMessage(colorize("&aReceived &e" + amount + " &atokens from &f" + player.getName() + "&a."));
        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
