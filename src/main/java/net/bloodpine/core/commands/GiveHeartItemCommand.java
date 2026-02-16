package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class GiveHeartItemCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public GiveHeartItemCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bloodpine.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(colorize("&cUsage: /giveheartitem <player> [amount]"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(colorize("&cPlayer not found or not online!"));
            return true;
        }

        int amount = 1;
        if (args.length == 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize("&cAmount must be a number!"));
                return true;
            }
        }

        if (amount <= 0 || amount > 64) {
            sender.sendMessage(colorize("&cAmount must be between 1 and 64!"));
            return true;
        }

        ItemStack heartItem = ItemUtils.createHeartItem(amount);
        HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(heartItem);

        if (overflow.isEmpty()) {
            sender.sendMessage(colorize("&aGave &c" + amount + " &aHeart Item" + (amount > 1 ? "s" : "") + " to &f" + target.getName() + "&a!"));
            target.sendMessage(colorize("&aYou received &c" + amount + " &aHeart Item" + (amount > 1 ? "s" : "") + "&a!"));
        } else {
            sender.sendMessage(colorize("&e" + target.getName() + "'s inventory is full! Some items were dropped."));
            for (ItemStack item : overflow.values()) {
                target.getWorld().dropItem(target.getLocation(), item);
            }
        }

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
