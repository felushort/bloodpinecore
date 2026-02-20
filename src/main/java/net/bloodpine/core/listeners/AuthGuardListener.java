package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuthGuardListener implements Listener {

    private final BloodpineCore plugin;

    public AuthGuardListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getAuthManager().clearAuthentication(player);

        if (plugin.getAuthManager().isRegistered(player.getName())) {
            player.sendMessage(colorize("&ePlease login: &f/login <password>"));
        } else {
            player.sendMessage(colorize("&ePlease register: &f/register <password>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAuthManager().clearAuthentication(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }

        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticated(player)) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        if (message.startsWith("/login ") || message.equals("/login")
                || message.startsWith("/l ") || message.equals("/l")
                || message.startsWith("/register ") || message.equals("/register")
                || message.startsWith("/reg ") || message.equals("/reg")) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou must login first: &f/login <password>"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou must login before chatting."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!plugin.getAuthManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        cancelIfNotAuthenticated(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelIfNotAuthenticated(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        cancelIfNotAuthenticated(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        cancelIfNotAuthenticated(event.getPlayer(), event);
    }

    private void cancelIfNotAuthenticated(Player player, org.bukkit.event.Cancellable event) {
        if (!plugin.getAuthManager().isAuthRequired()) {
            return;
        }
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
