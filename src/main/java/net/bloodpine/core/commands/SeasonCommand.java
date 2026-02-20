package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SeasonCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public SeasonCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        plugin.getExpansionGUIManager().openSeason(player);
        player.sendMessage(plugin.getGameplayExpansionManager().getSeasonStory());
        player.sendMessage(plugin.getGameplayExpansionManager().getSeasonTimingLine());
        net.bloodpine.core.managers.ExpansionDataManager.ExpansionProfile profile =
                plugin.getExpansionDataManager().getProfile(player);
        player.sendMessage(colorize("&7Season cosmetics: &bTop10 x" + profile.getSeasonTop10Finishes()
                + " &dTop3 x" + profile.getSeasonTop3Finishes()
                + " &6Champions x" + profile.getSeasonChampionFinishes()));
        player.sendMessage(colorize("&7Wipes now align to seasonal lore arcs."));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
