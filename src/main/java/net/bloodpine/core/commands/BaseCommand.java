package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base command class providing common functionality for all commands
 */
public abstract class BaseCommand implements CommandExecutor {
    
    protected final BloodpineCore plugin;
    
    public BaseCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return execute(sender, command, label, args);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing command /" + label + ": " + e.getMessage());
            e.printStackTrace();
            sender.sendMessage(colorize("&cAn error occurred while executing this command. Please contact an administrator."));
            return true;
        }
    }
    
    /**
     * Execute the command logic
     * @param sender Command sender
     * @param command Command
     * @param label Command label
     * @param args Command arguments
     * @return true if command was handled
     */
    protected abstract boolean execute(CommandSender sender, Command command, String label, String[] args);
    
    /**
     * Check if sender is a player
     * @param sender Command sender
     * @return true if sender is a player
     */
    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use this command!"));
            return false;
        }
        return true;
    }
    
    /**
     * Parse integer from string with error handling
     * @param sender Command sender to send error messages to
     * @param value String to parse
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive), or -1 for no max
     * @return Parsed integer, or null if invalid
     */
    protected Integer parseInteger(CommandSender sender, String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min) {
                sender.sendMessage(colorize(getPrefix() + "&cValue must be at least " + min + "!"));
                return null;
            }
            if (max != -1 && parsed > max) {
                sender.sendMessage(colorize(getPrefix() + "&cValue cannot exceed " + max + "!"));
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize(getPrefix() + "&cInvalid number: " + value));
            return null;
        }
    }
    
    /**
     * Get the configured message prefix
     * @return Message prefix
     */
    protected String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&c&lBloodpine &7» ");
    }
    
    /**
     * Colorize a message
     * @param message Message to colorize
     * @return Colorized message
     */
    protected String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Send a prefixed message to sender
     * @param sender Recipient
     * @param message Message to send (will be prefixed and colorized)
     */
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(getPrefix() + message));
    }
}
