package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.CombatStatsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to view detailed combat statistics
 */
public class CombatStatsCommand extends BaseCommand {
    
    public CombatStatsCommand(BloodpineCore plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }
        
        Player player = (Player) sender;
        CombatStatsManager.CombatStats stats = plugin.getCombatStatsManager().getStats(player);
        
        sender.sendMessage(colorize("&c&m-----&r &c&lCombat Statistics &c&m-----"));
        sender.sendMessage(colorize("&7Damage Dealt: &c" + String.format("%.1f", stats.getDamageDealt())));
        sender.sendMessage(colorize("&7Damage Taken: &9" + String.format("%.1f", stats.getDamageTaken())));
        sender.sendMessage(colorize("&7Damage Ratio: &e" + String.format("%.2f", stats.getDamageRatio())));
        sender.sendMessage("");
        sender.sendMessage(colorize("&7Hits Landed: &a" + stats.getHitsLanded()));
        sender.sendMessage(colorize("&7Hits Taken: &c" + stats.getHitsTaken()));
        sender.sendMessage(colorize("&7Critical Hits: &6" + stats.getCriticalHits()));
        sender.sendMessage("");
        sender.sendMessage(colorize("&7Avg Damage/Hit: &e" + String.format("%.2f", stats.getAverageDamagePerHit())));
        sender.sendMessage(colorize("&7Crit Rate: &6" + String.format("%.1f%%", stats.getCriticalHitRate() * 100)));
        sender.sendMessage(colorize("&c&m------------------------------"));
        sender.sendMessage(colorize("&8Stats reset on death or server restart"));
        
        return true;
    }
}
