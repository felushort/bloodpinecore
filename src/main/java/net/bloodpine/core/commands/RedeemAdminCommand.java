package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.RedeemCodeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class RedeemAdminCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public RedeemAdminCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bloodpine.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        RedeemCodeManager manager = plugin.getRedeemCodeManager();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /redeemadmin create <code> [tokens] [hearts] [totems] [gapples] [xp]"));
                    return true;
                }
                String code = args[1].trim().toUpperCase();
                if (code.isBlank()) {
                    sender.sendMessage(colorize("&cCode cannot be blank."));
                    return true;
                }

                int tokens = parseInt(args, 2, 20);
                int hearts = parseInt(args, 3, 1);
                int totems = parseInt(args, 4, 1);
                int gapples = parseInt(args, 5, 8);
                int xp = parseInt(args, 6, 16);

                if (tokens < 0 || hearts < 0 || totems < 0 || gapples < 0 || xp < 0) {
                    sender.sendMessage(colorize("&cReward values must be 0 or higher."));
                    return true;
                }

                manager.setCode(code, new RedeemCodeManager.RedeemReward(tokens, hearts, totems, gapples, xp));
                manager.save();

                sender.sendMessage(colorize("&aCreated code &f" + code + "&a."));
                sender.sendMessage(colorize("&7Rewards: &e" + tokens + " tokens&7, &c" + hearts + " hearts&7, &f" + totems + " totems&7, &6" + gapples + " gapples&7, &a" + xp + " xp bottles"));
            }
            case "delete" -> {
                if (args.length != 2) {
                    sender.sendMessage(colorize("&cUsage: /redeemadmin delete <code>"));
                    return true;
                }
                String code = args[1].trim().toUpperCase();
                boolean removed = manager.removeCode(code);
                if (!removed) {
                    sender.sendMessage(colorize("&cCode not found: &f" + code));
                    return true;
                }
                manager.save();
                sender.sendMessage(colorize("&aDeleted code &f" + code + "&a."));
            }
            case "list" -> {
                if (manager.getCodes().isEmpty()) {
                    sender.sendMessage(colorize("&eNo redeem codes configured."));
                    return true;
                }
                sender.sendMessage(colorize("&6Redeem Codes:"));
                for (Map.Entry<String, RedeemCodeManager.RedeemReward> entry : manager.getAllRewards().entrySet()) {
                    RedeemCodeManager.RedeemReward reward = entry.getValue();
                    sender.sendMessage(colorize("&f- " + entry.getKey() + " &7(tokens=" + reward.tokens() + ", hearts=" + reward.hearts() + ", totems=" + reward.totems() + ", gapples=" + reward.goldenApples() + ", xp=" + reward.experienceBottles() + ")"));
                }
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private int parseInt(String[] args, int index, int fallback) {
        if (index >= args.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(colorize("&6/redeemadmin create <code> [tokens] [hearts] [totems] [gapples] [xp]"));
        sender.sendMessage(colorize("&6/redeemadmin delete <code>"));
        sender.sendMessage(colorize("&6/redeemadmin list"));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
