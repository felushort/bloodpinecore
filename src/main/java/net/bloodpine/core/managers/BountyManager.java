package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, Integer> bounties;
    
    public BountyManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.bounties = new HashMap<>();
    }
    
    public void placeBounty(Player target, Player placer, int amount) {
        if (amount <= 0) {
            placer.sendMessage(colorize("&cBounty must be greater than 0!"));
            return;
        }
        
        int currentTokens = plugin.getTokenManager().getTokens(placer);
        if (currentTokens < amount) {
            placer.sendMessage(colorize("&cYou don't have enough tokens!"));
            return;
        }
        
        // Take tokens from placer
        plugin.getTokenManager().removeTokens(placer, amount);
        
        // Add to target's bounty
        UUID targetUUID = target.getUniqueId();
        int currentBounty = bounties.getOrDefault(targetUUID, 0);
        bounties.put(targetUUID, currentBounty + amount);
        
        // Broadcast
        String message = colorize("&c&l" + placer.getName() + " &7placed a &e" + amount + " token &7bounty on &c&l" + target.getName() + "&7!");
        Bukkit.broadcastMessage(message);
        
        placer.sendMessage(colorize("&aYou placed a &e" + amount + " token &abounty on &f" + target.getName()));
        target.sendMessage(colorize("&c&l⚠ WARNING! &7A bounty of &e" + bounties.get(targetUUID) + " tokens &7has been placed on your head!"));
    }
    
    public void claimBounty(Player killer, Player victim) {
        UUID victimUUID = victim.getUniqueId();
        int bounty = bounties.getOrDefault(victimUUID, 0);
        
        if (bounty > 0) {
            plugin.getTokenManager().giveTokens(killer, bounty);
            bounties.remove(victimUUID);
            
            killer.sendMessage(colorize("&a&lBOUNTY CLAIMED! &e+" + bounty + " tokens!"));
            Bukkit.broadcastMessage(colorize("&c&l" + killer.getName() + " &7claimed the &e" + bounty + " token &7bounty on &c" + victim.getName() + "&7!"));
        }
    }
    
    public int getBounty(Player player) {
        return bounties.getOrDefault(player.getUniqueId(), 0);
    }
    
    public boolean hasBounty(Player player) {
        return bounties.containsKey(player.getUniqueId()) && bounties.get(player.getUniqueId()) > 0;
    }

    public void clearBounty(UUID uuid) {
        bounties.remove(uuid);
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
