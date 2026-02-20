package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HeartInsuranceCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public HeartInsuranceCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("heart-insurance.enabled", true)) {
            player.sendMessage(colorize("&cHeart Insurance is currently disabled."));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int cost = Math.max(1, plugin.getConfig().getInt("heart-insurance.token-cost", 10));
        int maxCharges = Math.max(1, plugin.getConfig().getInt("heart-insurance.max-charges", 3));

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            player.sendMessage(colorize("&b&lHeart Insurance"));
            player.sendMessage(colorize("&7Current insured hearts: &b" + data.getInsuredHearts() + "&7/&b" + maxCharges));
            player.sendMessage(colorize("&7Cost per insured heart: &e" + cost + " available tokens"));
            player.sendMessage(colorize("&8Use &f/heartinsurance buy [amount] &8to purchase."));
            return true;
        }

        if (!args[0].equalsIgnoreCase("buy")) {
            player.sendMessage(colorize("&cUsage: /heartinsurance [info|buy <amount>]"));
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(colorize("&cAmount must be a number."));
                return true;
            }
        }

        if (amount <= 0) {
            player.sendMessage(colorize("&cAmount must be at least 1."));
            return true;
        }

        int current = data.getInsuredHearts();
        int room = maxCharges - current;
        if (room <= 0) {
            player.sendMessage(colorize("&eYou already have the maximum insured hearts."));
            return true;
        }

        int toBuy = Math.min(room, amount);
        int totalCost = toBuy * cost;
        if (data.getAvailableTokens() < totalCost) {
            player.sendMessage(colorize("&cYou need &e" + totalCost + " &cavailable tokens to buy &e" + toBuy
                    + " &cinsurance charge(s)."));
            return true;
        }

        plugin.getTokenManager().removeTokens(player, totalCost);
        data.addInsuredHearts(toBuy);
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);

        player.sendMessage(colorize("&aPurchased &b" + toBuy + " heart insurance charge(s) &afor &e" + totalCost + " tokens&a."));
        player.sendMessage(colorize("&7Insured hearts now: &b" + data.getInsuredHearts() + "&7/&b" + maxCharges));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1.2f);
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
