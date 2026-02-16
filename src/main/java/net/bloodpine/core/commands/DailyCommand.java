package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to claim daily rewards
 */
public class DailyCommand extends BaseCommand {
    
    public DailyCommand(BloodpineCore plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player, "daily", 5)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player, "daily", 5);
            sendMessage(sender, "&cPlease wait &e" + remaining + " seconds &cbefore using this command again!");
            return true;
        }
        
        boolean claimed = plugin.getDailyRewardManager().claimReward(player);
        
        if (claimed) {
            plugin.getCooldownManager().setCooldown(player, "daily");
            
            // Check for achievements
            plugin.getAchievementManager().checkAchievements(player);
        }
        
        return true;
    }
}
