package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class StatManager {
    
    private final BloodpineCore plugin;
    
    public StatManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    public boolean allocateTokens(Player player, StatType statType, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Check if player has enough available tokens
        if (data.getAvailableTokens() < amount) {
            return false;
        }
        
        // Check if allocation would exceed cap
        int currentAllocation = data.getAllocatedTokens(statType);
        int maxTokens = plugin.getConfig().getInt("stats." + statType.getConfigKey() + ".max-tokens");
        
        if (currentAllocation + amount > maxTokens) {
            return false;
        }
        
        // Allocate tokens
        for (int i = 0; i < amount; i++) {
            data.allocateToken(statType);
        }
        
        // Apply stat changes
        applyStats(player);
        
        return true;
    }
    
    public void resetStats(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        data.resetAllocations();
        applyStats(player);
    }
    
    public void applyStats(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Apply vitality (health)
        int vitalityTokens = data.getAllocatedTokens(StatType.VITALITY);
        double heartsPerToken = plugin.getConfig().getDouble("stats.vitality.per-token", 1.0);
        
        // Base health from config starting hearts
        int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
        double baseHealth = startingHearts * 2.0; // starting hearts in HP
        
        // Lifesteal hearts (each heart = 2 HP)
        double lifestealHealth = data.getLifestealHearts() * 2.0;
        
        // Vitality bonus health
        double vitalityHealth = vitalityTokens * heartsPerToken;
        double rebirthHealth = data.getRebirthLevel() * plugin.getRebirthManager().getVitalityHeartsPerLevel() * 2.0;
        
        // Calculate final max health with limits
        double maxHearts = plugin.getConfig().getDouble("lifesteal.max-hearts", 20) * 2.0;
        double minHealth = plugin.getConfig().getDouble("lifesteal.min-hearts", 1) * 2.0;
        double finalHealth = Math.max(minHealth, Math.min(maxHearts, baseHealth + lifestealHealth + vitalityHealth + rebirthHealth));
        
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(finalHealth);
        
        // Health is clamped to current max
        if (player.getHealth() > player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
    }
    
    public double getDamageMultiplier(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int damageTokens = data.getAllocatedTokens(StatType.DAMAGE);
        double percentPerToken = plugin.getConfig().getDouble("stats.damage.per-token", 1.0);
        double tokenMultiplier = 1.0 + (damageTokens * percentPerToken / 100.0);
        double rebirthMultiplier = 1.0 + (data.getRebirthLevel() * plugin.getRebirthManager().getDamageBonusPerLevel());
        return tokenMultiplier * rebirthMultiplier;
    }
    
    public double getDefenseMultiplier(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int defenseTokens = data.getAllocatedTokens(StatType.DEFENSE);
        double percentPerToken = plugin.getConfig().getDouble("stats.defense.per-token", 1.0);
        double tokenMultiplier = 1.0 - (defenseTokens * percentPerToken / 100.0);
        double rebirthReduction = data.getRebirthLevel() * plugin.getRebirthManager().getDefenseBonusPerLevel();
        return Math.max(0.05, tokenMultiplier * (1.0 - rebirthReduction));
    }
    
    public double getCrystalMultiplier(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int crystalTokens = data.getAllocatedTokens(StatType.CRYSTAL);
        double percentPerToken = plugin.getConfig().getDouble("stats.crystal.per-token", 1.0);
        double tokenMultiplier = 1.0 + (crystalTokens * percentPerToken / 100.0);
        double rebirthMultiplier = 1.0 + (data.getRebirthLevel() * (plugin.getRebirthManager().getDamageBonusPerLevel() / 2.0));
        return tokenMultiplier * rebirthMultiplier;
    }
    
    public int getTotemDelay(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int totemTokens = data.getAllocatedTokens(StatType.TOTEM);
        int reductionPerToken = plugin.getConfig().getInt("stats.totem.per-token", 50);
        return Math.max(0, totemTokens * reductionPerToken);
    }
    
    public int getAllocatedTokens(Player player, StatType statType) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        return data.getAllocatedTokens(statType);
    }
    
    public int getMaxTokensForStat(StatType statType) {
        return plugin.getConfig().getInt("stats." + statType.getConfigKey() + ".max-tokens", 10);
    }
}
