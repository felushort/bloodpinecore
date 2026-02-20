package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class OpCommandLockListener implements Listener {

    private final BloodpineCore plugin;

    public OpCommandLockListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (isOpGrantCommand(message)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(colorize("&cOP commands are disabled. OP can only be changed by editing ops.json and restarting."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        String commandLine = event.getCommand();
        if (isOpGrantCommand(commandLine)) {
            event.setCancelled(true);
            logBlocked(event.getSender(), commandLine);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        String commandLine = event.getCommand();
        if (isOpGrantCommand(commandLine)) {
            event.setCancelled(true);
            logBlocked(event.getSender(), commandLine);
        }
    }

    private boolean isOpGrantCommand(String rawCommandLine) {
        if (rawCommandLine == null) {
            return false;
        }

        String line = rawCommandLine.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }

        if (line.isEmpty()) {
            return false;
        }

        String[] parts = line.split("\\s+", 2);
        String label = parts[0];
        int colon = label.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < label.length()) {
            label = label.substring(colon + 1);
        }

        return label.equalsIgnoreCase("op");
    }

    private void logBlocked(CommandSender sender, String commandLine) {
        String senderName = sender == null ? "unknown" : sender.getName();
        plugin.getLogger().warning("Blocked OP command from " + senderName + ": " + commandLine);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
