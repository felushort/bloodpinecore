package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.managers.ExpansionDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PvpStatsCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public PvpStatsCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length >= 1 && args[0].equalsIgnoreCase("leaderboard")) {
            showLeaderboards(player);
            return true;
        }

        Player target = player;
        if (args.length >= 1) {
            Player candidate = Bukkit.getPlayerExact(args[0]);
            if (candidate != null) target = candidate;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(target);
        double dps = plugin.getGameplayExpansionManager().getRecentDps(target);

        plugin.getExpansionGUIManager().openPvpStats(player, target);

        player.sendMessage(colorize("&c&lPvP Stats &8- &f" + target.getName()));
        player.sendMessage(colorize("&7K/D: &a" + data.getTotalKills() + "&7/&c" + data.getTotalDeaths() + " &8(" + String.format("%.2f", data.getKDRatio()) + ")"));
        player.sendMessage(colorize("&7Assists: &e" + data.getTotalAssists() + " &8| &7Longest Streak: &6" + data.getLongestKillstreak()));
        player.sendMessage(colorize("&7Recent DPS(10s): &e" + String.format("%.2f", dps)));
        player.sendMessage(colorize("&7Rebirth: &d" + data.getRebirthLevel() + " &8| &7Ascension: &5" + profile.getAscension()));
        player.sendMessage(colorize("&7Hearts Gained (Season): &c" + profile.getHeartsGainedSeason()
                + " &8| &7Contracts Completed: &e" + profile.getContractsCompletedTotal()));
        player.sendMessage(colorize("&7Rep: &a" + profile.getReputation() + " &8| &7Fame: &6" + profile.getFame() + " &8| &7Infamy: &4" + profile.getInfamy()));
        player.sendMessage(colorize("&8Ascension = late-game prestige after rebirth milestones."));
        player.sendMessage(colorize("&8Rep = honor score, Fame = elite-kill recognition, Infamy = notorious predator score."));
        player.sendMessage(colorize("&8Recent DPS = your last 10s average combat damage output."));
        player.sendMessage(colorize("&8Use /pvpstats leaderboard"));
        return true;
    }

    private void showLeaderboards(Player viewer) {
        List<PlayerData> all = new ArrayList<>(plugin.getDataManager().getAllPlayerData());

        all.sort(Comparator.comparingDouble(PlayerData::getKDRatio).reversed());
        viewer.sendMessage(colorize("&eTop KDR"));
        for (int i = 0; i < Math.min(5, all.size()); i++) {
            PlayerData data = all.get(i);
            viewer.sendMessage(colorize("&7#" + (i + 1) + " &f" + data.getName() + " &8- &e" + String.format("%.2f", data.getKDRatio())));
        }

        all.sort(Comparator.comparingInt(PlayerData::getRebirthLevel).reversed());
        viewer.sendMessage(colorize("&dTop Rebirth"));
        for (int i = 0; i < Math.min(5, all.size()); i++) {
            PlayerData data = all.get(i);
            viewer.sendMessage(colorize("&7#" + (i + 1) + " &f" + data.getName() + " &8- &d" + data.getRebirthLevel()));
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
