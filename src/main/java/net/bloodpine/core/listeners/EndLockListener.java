package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EndLockListener implements Listener {

    private final BloodpineCore plugin;

    public EndLockListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (!isEndLocked()) {
            return;
        }

        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }

        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(colorize("&cThe End is currently closed."));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!isEndLocked()) {
            return;
        }

        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }

        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            player.sendMessage(colorize("&cThe End is currently closed."));
        }
    }

    private boolean isEndLocked() {
        return plugin.getConfig().getBoolean("end-control.enabled", true)
                && !plugin.getConfig().getBoolean("end-control.open", false);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
