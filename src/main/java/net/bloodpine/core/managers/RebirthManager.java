package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class RebirthManager {

    private final BloodpineCore plugin;

    public RebirthManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public int getRequiredTokens() {
        return plugin.getConfig().getInt("rebirth.requirement-tokens", 50);
    }

    public int getPointsPerRebirth() {
        return plugin.getConfig().getInt("rebirth.points-per-rebirth", 1);
    }

    public double getDamageBonusPerLevel() {
        return plugin.getConfig().getDouble("rebirth.damage-bonus-per-level", 0.03D);
    }

    public double getDefenseBonusPerLevel() {
        return plugin.getConfig().getDouble("rebirth.defense-bonus-per-level", 0.02D);
    }

    public double getVitalityHeartsPerLevel() {
        return plugin.getConfig().getDouble("rebirth.vitality-hearts-per-level", 0.5D);
    }

    public boolean canRebirth(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.getTotalTokens() >= getRequiredTokens();
    }

    public boolean performRebirth(Player player) {
        if (!canRebirth(player)) {
            return false;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);

        data.addRebirthLevels(1);
        data.addRebirthPoints(getPointsPerRebirth());

        data.setTotalTokens(0);
        data.resetAllocations();
        data.setMarked(false);
        data.setLifestealHearts(0);

        plugin.getStatManager().applyStats(player);
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getDataManager().saveData();

        player.sendMessage(colorize("&d&lREBIRTH! &7You are now rebirth level &f" + data.getRebirthLevel() + "&7."));
        player.sendMessage(colorize("&aYou gained &e" + getPointsPerRebirth() + " rebirth point(s)&a and permanent bonuses."));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
        return true;
    }

    public void grantRebirthLevels(Player player, int amount) {
        if (amount <= 0) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.addRebirthLevels(amount);
        data.addRebirthPoints(amount * getPointsPerRebirth());
        plugin.getStatManager().applyStats(player);
        plugin.getDisplayManager().updateDisplay(player);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
