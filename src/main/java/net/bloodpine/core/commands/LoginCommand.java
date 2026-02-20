package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public LoginCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cPlayers only."));
            return true;
        }

        if (!plugin.getAuthManager().isAuthRequired()) {
            player.sendMessage(colorize("&eAuthentication is disabled while online-mode is enabled."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(colorize("&cUsage: /login <password>"));
            return true;
        }

        if (!plugin.getAuthManager().isRegistered(player.getName())) {
            player.sendMessage(colorize("&cYou are not registered. Use /register <password>."));
            return true;
        }

        if (!plugin.getAuthManager().checkPassword(player.getName(), args[0])) {
            player.sendMessage(colorize("&cWrong password."));
            return true;
        }

        String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "";
        if (!plugin.getAuthManager().isIpAllowedForAccount(player.getName(), ip)) {
            player.sendMessage(colorize("&cThis account is locked to its original network."));
            return true;
        }

        plugin.getAuthManager().recordSuccessfulLogin(player.getName(), ip);
        plugin.getAuthManager().markAuthenticated(player);
        plugin.getSessionLocationManager().restoreLocation(player);
        ensurePlayableGamemode(player);
        player.sendMessage(colorize("&aLogged in successfully."));
        return true;
    }

    private void ensurePlayableGamemode(Player player) {
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.ADVENTURE) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(colorize("&eYour gamemode was reset to &fSURVIVAL &eto restore normal interaction."));
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
