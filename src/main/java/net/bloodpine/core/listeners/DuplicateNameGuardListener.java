package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class DuplicateNameGuardListener implements Listener {

    private final BloodpineCore plugin;

    public DuplicateNameGuardListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String incomingName = event.getName();
        if (incomingName == null || incomingName.isBlank()) {
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(incomingName)) {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "A player with this username is already online."
                );
                plugin.getLogger().warning("Blocked duplicate username login attempt for '" + incomingName + "'.");
                return;
            }
        }
    }
}
