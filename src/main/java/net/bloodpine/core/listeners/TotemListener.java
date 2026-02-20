package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TotemListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public TotemListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.getGameplayExpansionManager().isNoTotemZone(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cTotems are disabled in the Blood Zone.");
            return;
        }
        
        if (event.isCancelled()) return;
        
        // Get totem tokens allocated
        int totemTokens = plugin.getDataManager().getPlayerData(player)
                .getAllocatedTokens(net.bloodpine.core.data.StatType.TOTEM);
        
        if (totemTokens <= 0) return;
        
        // Give absorption hearts based on totem tokens (1 heart per token)
        int absorptionLevel = totemTokens - 1; // Level 0 = 2 absorption hearts, Level 1 = 4, etc.
        // Each level of Absorption gives 2 extra hearts, we want 1 per token
        // So we apply after a tick delay to override vanilla totem absorption
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Remove existing absorption
            player.removePotionEffect(PotionEffectType.ABSORPTION);
            // Apply our boosted absorption: totemTokens absorption hearts
            // Absorption level translates: level 0 = 2 hearts, level 1 = 4 hearts...
            // We want totemTokens hearts of absorption, so (totemTokens / 2) rounded up as level
            int level = Math.max(0, (int) Math.ceil(totemTokens / 2.0) - 1);
            // Duration: 10 seconds (200 ticks), slightly longer than vanilla's 5s
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, level, true, true, true));
            
            // Also give brief Resistance I to make totem pops more survivable
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, true, true));
        }, 1L);
    }
}
