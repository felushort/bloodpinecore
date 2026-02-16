package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class SidebarManager {

    private final BloodpineCore plugin;

    public SidebarManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) {
            return;
        }

        Scoreboard board = player.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("bp_sidebar");
        if (obj == null) {
            obj = board.registerNewObjective("bp_sidebar", "dummy", colorize(plugin.getConfig().getString("sidebar.title", "&c&lBloodpine")));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        String serverIp = plugin.getConfig().getString("sidebar.server-ip", "play.bloodpine.net");

        int score = 15;
        set(obj, score--, "&f" + player.getName());
        set(obj, score--, "&7Ping: &a" + player.getPing() + "ms");
        set(obj, score--, "&7IP: &e" + serverIp);
        set(obj, score--, "&7Tok: &6" + data.getTotalTokens());
        set(obj, score--, "&7K/D: &a" + data.getTotalKills() + "&7/&c" + data.getTotalDeaths());
    }

    public void clearPlayer(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            return;
        }
        Objective obj = board.getObjective("bp_sidebar");
        if (obj != null) {
            obj.unregister();
        }
    }

    private void set(Objective objective, int score, String line) {
        objective.getScore(colorize(line)).setScore(score);
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
