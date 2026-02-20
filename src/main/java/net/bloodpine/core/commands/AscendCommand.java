package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.ExpansionDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AscendCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public AscendCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openAscension(player);
        }

        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
            int minRebirth = plugin.getConfig().getInt("endgame.ascension-min-rebirth", 5);
            int tokenCost = plugin.getConfig().getInt("endgame.ascension-token-cost", 40);
            player.sendMessage(colorize("&5Ascension"));
            player.sendMessage(colorize("&7Current Tier: &d" + profile.getAscension()));
            player.sendMessage(colorize("&7Requires Rebirth: &f" + minRebirth + " &8| &7Cost: &e" + tokenCost + " tokens"));
            player.sendMessage(colorize("&8Use /ascend to ascend."));
            return true;
        }

        plugin.getGameplayExpansionManager().tryAscend(player);
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
