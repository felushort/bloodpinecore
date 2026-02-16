package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TokenShopCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public TokenShopCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        plugin.getTokenShopGUI().open(player);
        return true;
    }
}
