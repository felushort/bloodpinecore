package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class GambleCommand implements CommandExecutor {

    private final BloodpineCore plugin;
    private final Random random = new Random();

    public GambleCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) {
            plugin.getExpansionGUIManager().openEconomy(player);
            player.sendMessage(colorize("&cUsage: /gamble <amount>"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&cAmount must be numeric."));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(colorize("&cAmount must be > 0."));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (data.getAvailableTokens() < amount) {
            player.sendMessage(colorize("&cNot enough available tokens."));
            return true;
        }

        plugin.getTokenManager().removeTokens(player, amount);

        double winChance = plugin.getConfig().getDouble("token-sinks.gamble-win-chance", 0.45);
        if (random.nextDouble() <= winChance) {
            int reward = amount * 2;
            plugin.getTokenManager().giveTokens(player, reward);
            player.sendMessage(colorize("&aYou won! &e+" + reward + " tokens"));
        } else {
            player.sendMessage(colorize("&cYou lost &e" + amount + " tokens&c."));
        }

        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
