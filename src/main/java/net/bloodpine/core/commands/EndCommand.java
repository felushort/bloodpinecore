package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public EndCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command."));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(colorize("&cOnly OPs can use this command."));
            return true;
        }

        String ownerName = plugin.getConfig().getString("end-control.owner", "Felushort");
        if (!player.getName().equalsIgnoreCase(ownerName)) {
            player.sendMessage(colorize("&cOnly &f" + ownerName + " &ccan control The End."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(colorize("&cUsage: /end <open|close|status>"));
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "open" -> {
                plugin.getConfig().set("end-control.open", true);
                plugin.saveConfig();
                player.sendMessage(colorize("&aThe End is now &fOPEN&a."));
            }
            case "close" -> {
                plugin.getConfig().set("end-control.open", false);
                plugin.saveConfig();
                player.sendMessage(colorize("&eThe End is now &fCLOSED&e."));
            }
            case "status" -> {
                boolean open = plugin.getConfig().getBoolean("end-control.open", false);
                player.sendMessage(colorize("&7End status: " + (open ? "&aOPEN" : "&cCLOSED")));
            }
            default -> player.sendMessage(colorize("&cUsage: /end <open|close|status>"));
        }

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
