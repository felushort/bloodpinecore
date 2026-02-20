package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.AntiVpnManager;
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

        String incomingIp = event.getAddress() != null ? event.getAddress().getHostAddress() : "";

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

        AntiVpnManager antiVpn = plugin.getAntiVpnManager();
        if (antiVpn != null && antiVpn.isEnabled() && !antiVpn.isBypassed(incomingName)) {
            AntiVpnManager.CheckResult check = antiVpn.checkIp(incomingIp);
            boolean logAll = plugin.getConfig().getBoolean("security.anti-vpn.log-all-checks", false);
            if (logAll || check.isBlocked()) {
                plugin.getLogger().info("AntiVPN check " + incomingName + " (" + incomingIp + "): provider="
                        + check.getProvider() + ", blocked=" + check.isBlocked() + ", reason=" + check.getReason());
            }
            if (check.isBlocked()) {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        colorize(antiVpn.getKickMessage())
                );
                return;
            }
        }

        if (!plugin.getAuthManager().isAuthRequired() || !plugin.getAuthManager().isAltProtectionEnabled()) {
            return;
        }

        if (plugin.getAuthManager().isRegistered(incomingName)) {
            if (!plugin.getAuthManager().isIpAllowedForAccount(incomingName, incomingIp)) {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "This account is locked to its original network. Ask staff to verify ownership."
                );
                plugin.getLogger().warning("Blocked cracked-login for '" + incomingName + "' from unauthorized network " + incomingIp);
            }
            return;
        }

        if (plugin.getAuthManager().isExemptName(incomingName)) {
            return;
        }

        if (!plugin.getAuthManager().canRegisterFromIp(incomingIp)) {
            int count = plugin.getAuthManager().countRegisteredAccountsForIp(incomingIp);
            int max = plugin.getAuthManager().getMaxAccountsPerIp();
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "Too many accounts from your network (" + count + "/" + max + ")."
            );
            plugin.getLogger().warning("Blocked cracked registration from " + incomingIp + " (network cap reached).");
            return;
        }

        long waitSeconds = plugin.getAuthManager().getRemainingRegistrationCooldownSeconds(incomingIp);
        if (waitSeconds > 0L) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.YELLOW + "Please wait " + waitSeconds + "s before creating another account."
            );
            plugin.getLogger().warning("Blocked cracked registration from " + incomingIp + " (cooldown " + waitSeconds + "s).");
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
