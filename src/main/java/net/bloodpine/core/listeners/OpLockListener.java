package net.bloodpine.core.listeners;

import net.bloodpine.core.security.OpLockManager;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class OpLockListener implements Listener {

    private final OpLockManager opLockManager;

    public OpLockListener(OpLockManager opLockManager) {
        this.opLockManager = opLockManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!opLockManager.shouldBlockOpCommands()) {
            return;
        }
        if (!opLockManager.isOpOrDeopCommand(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(opLockManager.getBlockedMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!opLockManager.shouldBlockOpCommands()) {
            return;
        }
        if (!opLockManager.isOpOrDeopCommand(event.getCommand())) {
            return;
        }

        event.setCancelled(true);
        send(event.getSender(), opLockManager.getBlockedMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        if (!opLockManager.shouldBlockOpCommands()) {
            return;
        }
        if (!opLockManager.isOpOrDeopCommand(event.getCommand())) {
            return;
        }

        event.setCancelled(true);
        send(event.getSender(), opLockManager.getBlockedMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        opLockManager.enforceNow();
    }

    private void send(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isBlank()) {
            sender.sendMessage(message);
        }
    }
}
