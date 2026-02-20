package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyBoardCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public BountyBoardCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        plugin.getExpansionGUIManager().openBountyBoard(player);
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
