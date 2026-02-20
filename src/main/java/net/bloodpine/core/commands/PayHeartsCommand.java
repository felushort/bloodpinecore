package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayHeartsCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public PayHeartsCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(colorize("&cUsage: /payhearts <player> <amount>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(colorize("&cPlayer not found or not online!"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(colorize("&cYou cannot send hearts to yourself!"));
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

        double senderProjected = projectedHealth(senderData, -amount);
        double senderMinimum = plugin.getConfig().getDouble("lifesteal.min-hearts", 1.0) * 2.0;
        if (senderProjected < senderMinimum) {
            int maxSendable = maxHeartsSenderCanGive(senderData);
            player.sendMessage(colorize("&cYou can only send up to &e" + Math.max(maxSendable, 0) + " &chearts right now."));
            return true;
        }

        double targetProjected = projectedHealth(targetData, amount);
        double targetMaximum = plugin.getConfig().getDouble("lifesteal.max-hearts", 20.0) * 2.0;
        if (targetMaximum > 0 && targetProjected > targetMaximum) {
            int maxReceivable = maxHeartsTargetCanReceive(targetData);
            player.sendMessage(colorize("&c" + target.getName() + " can only receive &e" + Math.max(maxReceivable, 0) + " &cmore hearts."));
            return true;
        }

        for (int i = 0; i < amount; i++) {
            senderData.removeLifestealHeart();
            targetData.addLifestealHeart();
        }

        plugin.getStatManager().applyStats(player);
        plugin.getStatManager().applyStats(target);

        player.sendMessage(colorize("&aSent &e" + amount + " &aheart(s) to &f" + target.getName() + "&a."));
        target.sendMessage(colorize("&aReceived &e" + amount + " &aheart(s) from &f" + player.getName() + "&a."));
        return true;
    }

    private int maxHeartsSenderCanGive(PlayerData senderData) {
        double minimum = plugin.getConfig().getDouble("lifesteal.min-hearts", 1.0) * 2.0;
        double currentBase = projectedHealth(senderData, 0);
        return (int) Math.floor((currentBase - minimum) / 2.0);
    }

    private int maxHeartsTargetCanReceive(PlayerData targetData) {
        double maximum = plugin.getConfig().getDouble("lifesteal.max-hearts", 20.0) * 2.0;
        if (maximum <= 0) {
            return Integer.MAX_VALUE;
        }
        double currentBase = projectedHealth(targetData, 0);
        return (int) Math.floor((maximum - currentBase) / 2.0);
    }

    private double projectedHealth(PlayerData data, int lifestealHeartDelta) {
        int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
        double baseHealth = startingHearts * 2.0;
        double vitalityPerToken = plugin.getConfig().getDouble("stats.vitality.per-token", 2.0);
        int vitalityTokens = data.getAllocatedTokens(StatType.VITALITY);
        double vitalityHealth = vitalityTokens * vitalityPerToken;
        double rebirthHealth = data.getRebirthLevel() * plugin.getRebirthManager().getVitalityHeartsPerLevel() * 2.0;
        double lifestealHealth = (data.getLifestealHearts() + lifestealHeartDelta) * 2.0;
        return baseHealth + vitalityHealth + rebirthHealth + lifestealHealth;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
