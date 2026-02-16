package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.Achievement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Command to view achievements
 */
public class AchievementsCommand extends BaseCommand {
    
    public AchievementsCommand(BloodpineCore plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }
        
        Player player = (Player) sender;
        Set<Achievement> unlocked = plugin.getAchievementManager().getAchievements(player.getUniqueId());
        
        sender.sendMessage(colorize("&6&m-----&r &6&lAchievements &6&m-----"));
        sender.sendMessage(colorize("&7Progress: &e" + unlocked.size() + "&7/&e" + Achievement.values().length));
        sender.sendMessage("");
        
        for (Achievement achievement : Achievement.values()) {
            boolean hasIt = unlocked.contains(achievement);
            String status = hasIt ? "&a✓" : "&7✗";
            String name = hasIt ? "&a" + achievement.getName() : "&7" + achievement.getName();
            
            sender.sendMessage(colorize(status + " " + name));
            sender.sendMessage(colorize("  &8" + achievement.getDescription() + " &7(+&e" + achievement.getTokenReward() + " tokens&7)"));
        }
        
        sender.sendMessage(colorize("&6&m---------------------------"));
        
        return true;
    }
}
