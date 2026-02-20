package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class ExpansionListener implements Listener {

    private final BloodpineCore plugin;

    public ExpansionListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null) return;

        if (event.getEntity() instanceof Player victim) {
            plugin.getGameplayExpansionManager().recordDamage(attacker, victim, event.getFinalDamage(), event.getCause().name());
        }

        Entity target = event.getEntity();
        plugin.getGameplayExpansionManager().onBossDamaged(attacker, target, event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        plugin.getGameplayExpansionManager().saveReplaySnapshot(victim, killer);

        if (killer != null && !killer.equals(victim)) {
            plugin.getGameplayExpansionManager().onPlayerKill(killer, victim);
            String title = plugin.getGameplayExpansionManager().getPlayerTitle(killer);
            event.setDeathMessage(colorize("&c" + victim.getName() + " &7was erased by " + title + " &f" + killer.getName()));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = entity instanceof LivingEntity living ? living.getKiller() : null;

        plugin.getGameplayExpansionManager().onBossDeath(entity, killer);
        plugin.getGameplayExpansionManager().onRogueAssassinDeath(entity, killer);
        plugin.getGameplayExpansionManager().onCustomMobDeath(entity, killer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameplayExpansionManager().applySeasonStory(player);
        if (event.getJoinMessage() != null) {
            event.setJoinMessage(plugin.getGameplayExpansionManager().buildJoinMessage(player));
        }

        if (plugin.getGameplayExpansionManager().isArenaEventActive()) {
            plugin.getGameplayExpansionManager().normalizeArenaGear(player);
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
