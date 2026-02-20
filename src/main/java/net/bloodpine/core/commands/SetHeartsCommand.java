package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHeartsCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public SetHeartsCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bloodpine.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(colorize("&cUsage: /sethearts <player> <hearts>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(colorize("&cPlayer not found or not online!"));
            return true;
        }

        int desiredHearts;
        try {
            desiredHearts = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(colorize("&cHearts must be a number!"));
            return true;
        }

        if (desiredHearts <= 0) {
            sender.sendMessage(colorize("&cHearts must be at least 1!"));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);

        int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
        double vitalityHp = data.getAllocatedTokens(StatType.VITALITY)
                * plugin.getConfig().getDouble("stats.vitality.per-token", 1.0);
        double rebirthHp = data.getRebirthLevel() * plugin.getRebirthManager().getVitalityHeartsPerLevel() * 2.0;

        double desiredHp = desiredHearts * 2.0;
        double requiredLifestealHp = desiredHp - (startingHearts * 2.0) - vitalityHp - rebirthHp;
        int lifestealHearts = (int) Math.round(requiredLifestealHp / 2.0);

        data.setLifestealHearts(lifestealHearts);
        plugin.getStatManager().applyStats(target);

        double finalHearts = target.getAttribute(Attribute.MAX_HEALTH).getBaseValue() / 2.0;

        sender.sendMessage(colorize("&aSet &f" + target.getName() + "&a to &c" + finalHearts + " &ahearts."));
        if (!sender.equals(target)) {
            target.sendMessage(colorize("&aYour max hearts were set to &c" + finalHearts + "&a."));
        }

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
