package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public RegisterCommand(BloodpineCore plugin) {
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
            player.sendMessage(colorize("&cUsage: /register <password>"));
            return true;
        }

        String password = args[0];
        if (password.length() < 4) {
            player.sendMessage(colorize("&cPassword must be at least 4 characters."));
            return true;
        }

        if (plugin.getAuthManager().isRegistered(player.getName())) {
            player.sendMessage(colorize("&cYou are already registered. Use /login <password>."));
            return true;
        }

        String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "";

        if (!plugin.getAuthManager().isExemptName(player.getName())) {
            if (!plugin.getAuthManager().canRegisterFromIp(ip)) {
                int count = plugin.getAuthManager().countRegisteredAccountsForIp(ip);
                int max = plugin.getAuthManager().getMaxAccountsPerIp();
                player.sendMessage(colorize("&cToo many accounts from your network (" + count + "/" + max + ")."));
                return true;
            }

            long waitSeconds = plugin.getAuthManager().getRemainingRegistrationCooldownSeconds(ip);
            if (waitSeconds > 0L) {
                player.sendMessage(colorize("&ePlease wait &f" + waitSeconds + "s &ebefore registering another account."));
                return true;
            }
        }

        boolean registered = plugin.getAuthManager().register(player.getName(), password, ip);
        if (!registered) {
            player.sendMessage(colorize("&cRegistration failed. Try again."));
            return true;
        }

        plugin.getAuthManager().markAuthenticated(player);
        plugin.getSessionLocationManager().restoreLocation(player);
        ensurePlayableGamemode(player);
        player.sendMessage(colorize("&aRegistered and logged in successfully."));
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
