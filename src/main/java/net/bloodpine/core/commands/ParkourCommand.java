package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ParkourCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public ParkourCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openParkour(player);
            player.sendMessage(colorize("&bUse /parkour start or /parkour finish"));
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            plugin.getGameplayExpansionManager().startParkour(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("finish")) {
            plugin.getGameplayExpansionManager().finishParkour(player);
            return true;
        }

        player.sendMessage(colorize("&cUsage: /parkour <start|finish>"));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
