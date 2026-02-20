package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.ExpansionDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DailyQuestCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public DailyQuestCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openDailyQuest(player);
        }

        plugin.getExpansionDataManager().resetDailyIfNeeded(player);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        int goal = plugin.getConfig().getInt("spawn.daily-quest-kill-goal", 3);
        int reward = plugin.getConfig().getInt("spawn.daily-quest-reward", 10);

        if (args.length >= 1 && args[0].equalsIgnoreCase("claim")) {
            if (profile.isDailyQuestClaimed()) {
                player.sendMessage(colorize("&eDaily quest already claimed."));
                return true;
            }
            if (profile.getDailyQuestProgress() < goal) {
                player.sendMessage(colorize("&cYou need " + goal + " kills. Progress: " + profile.getDailyQuestProgress()));
                return true;
            }
            profile.setDailyQuestClaimed(true);
            plugin.getTokenManager().giveTokens(player, reward);
            player.sendMessage(colorize("&aDaily quest complete! +" + reward + " tokens."));
            return true;
        }

        player.sendMessage(colorize("&bDaily Quest: Get " + goal + " PvP kills"));
        player.sendMessage(colorize("&7Progress: &f" + profile.getDailyQuestProgress() + "/" + goal));
        player.sendMessage(colorize("&7Reward: &e" + reward + " tokens"));
        player.sendMessage(colorize("&8Use /dailyquest claim when complete."));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
