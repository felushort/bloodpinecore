package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {
    
    private final BloodpineCore plugin;
    
    public DamageListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        
        // Resolve the attacker (direct hit or projectile shooter)
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        
        // Check for explosion damage (no attacker needed for defense on victim)
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean isExplosion = cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION 
                          || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        
        if (attacker == null) {
            // If explosion damage with no clear attacker, still apply defense
            if (isExplosion) {
                double defenseMultiplier = plugin.getStatManager().getDefenseMultiplier(victim);
                event.setDamage(event.getDamage() * defenseMultiplier);
            }
            return;
        }
        
        // Don't process self-damage
        if (attacker.equals(victim)) return;

        // Massive spawn safe-zone: block PvP near spawn to prevent spawn killing
        if (isInSpawnSafeZone(attacker) || isInSpawnSafeZone(victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§cPvP is disabled in the spawn safe-zone.");
            return;
        }
        
        // Combat tag both players
        plugin.getCombatLogListener().tagPlayer(attacker, victim);
        plugin.getCombatLogListener().tagPlayer(victim, attacker);
        
        double damage = event.getDamage();
        
        // Apply attacker's damage multiplier
        double damageMultiplier = plugin.getStatManager().getDamageMultiplier(attacker);
        damage *= damageMultiplier;
        
        // Apply victim's defense multiplier
        double defenseMultiplier = plugin.getStatManager().getDefenseMultiplier(victim);
        damage *= defenseMultiplier;
        
        // Set the modified damage
        event.setDamage(damage);
    }

    private boolean isInSpawnSafeZone(Player player) {
        if (!plugin.getConfig().getBoolean("spawn-safe-zone.enabled", true)) {
            return false;
        }

        int radius = plugin.getConfig().getInt("spawn-safe-zone.radius", 1500);
        World world = player.getWorld();
        Location spawn = world.getSpawnLocation();

        if (!world.getName().equalsIgnoreCase(spawn.getWorld().getName())) {
            return false;
        }

        double distanceSquared = player.getLocation().distanceSquared(spawn);
        double radiusSquared = (double) radius * radius;
        return distanceSquared <= radiusSquared;
    }
}
