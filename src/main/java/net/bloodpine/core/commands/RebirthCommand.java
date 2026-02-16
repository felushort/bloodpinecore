package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RebirthCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public RebirthCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sendInfo(player, data);
            return true;
        }

        int required = plugin.getRebirthManager().getRequiredTokens();
        if (!plugin.getRebirthManager().canRebirth(player)) {
            player.sendMessage(colorize("&cYou need &e" + required + " tokens &cto rebirth. Current: &e" + data.getTotalTokens()));
            player.sendMessage(colorize("&7Use &f/rebirth info &7to view bonuses."));
            return true;
        }

        boolean success = plugin.getRebirthManager().performRebirth(player);
        if (!success) {
            player.sendMessage(colorize("&cRebirth failed. Try again."));
        }
        return true;
    }

    private void sendInfo(Player player, PlayerData data) {
        player.sendMessage(colorize("&d&lRebirth System"));
        player.sendMessage(colorize("&7Level: &f" + data.getRebirthLevel()));
        player.sendMessage(colorize("&7Rebirth Points: &f" + data.getRebirthPoints()));
        player.sendMessage(colorize("&7Required Tokens: &e" + plugin.getRebirthManager().getRequiredTokens()));
        player.sendMessage(colorize("&7Bonuses per level:"));
        player.sendMessage(colorize(" &8• &c+" + (plugin.getRebirthManager().getDamageBonusPerLevel() * 100.0D) + "% damage"));
        player.sendMessage(colorize(" &8• &9+" + (plugin.getRebirthManager().getDefenseBonusPerLevel() * 100.0D) + "% defense"));
        player.sendMessage(colorize(" &8• &a+" + plugin.getRebirthManager().getVitalityHeartsPerLevel() + " hearts"));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
