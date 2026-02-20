package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BloodForgeCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public BloodForgeCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("blood-forge.enabled", true)) {
            player.sendMessage(colorize("&cBlood Forge is currently disabled."));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int cost = Math.max(1, plugin.getConfig().getInt("blood-forge.cost", 20));
        int maxLevel = Math.max(1, plugin.getConfig().getInt("blood-forge.max-level", 10));
        double bonusPerLevel = plugin.getConfig().getDouble("blood-forge.damage-bonus-per-level", 0.01);

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            int level = Math.min(maxLevel, data.getBloodForgeLevel());
            double bonusPercent = level * bonusPerLevel * 100.0;
            player.sendMessage(colorize("&4&lBlood Forge"));
            player.sendMessage(colorize("&7Level: &c" + level + "&7/&c" + maxLevel));
            player.sendMessage(colorize("&7Permanent damage bonus: &a+" + String.format("%.2f", bonusPercent) + "%"));
            player.sendMessage(colorize("&7Forge cost: &e" + cost + " available tokens per level"));
            player.sendMessage(colorize("&8Use &f/bloodforge forge &8to upgrade."));
            return true;
        }

        if (!args[0].equalsIgnoreCase("forge")) {
            player.sendMessage(colorize("&cUsage: /bloodforge [info|forge]"));
            return true;
        }

        int level = data.getBloodForgeLevel();
        if (level >= maxLevel) {
            player.sendMessage(colorize("&eYour Blood Forge is already maxed."));
            return true;
        }

        if (data.getAvailableTokens() < cost) {
            player.sendMessage(colorize("&cYou need &e" + cost + " &cavailable tokens. You have &e" + data.getAvailableTokens()));
            return true;
        }

        plugin.getTokenManager().removeTokens(player, cost);
        data.addBloodForgeLevel(1);
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);

        int newLevel = Math.min(maxLevel, data.getBloodForgeLevel());
        double newBonusPercent = newLevel * bonusPerLevel * 100.0;
        player.sendMessage(colorize("&4&lFORGED &7» &aBlood Forge upgraded to &cLv." + newLevel
                + "&a (&f+" + String.format("%.2f", newBonusPercent) + "% damage&a)"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 0.8f);
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
