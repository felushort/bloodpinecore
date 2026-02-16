package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates configuration files and reports issues
 */
public class ConfigValidator {
    
    private final BloodpineCore plugin;
    private final List<String> warnings;
    private final List<String> errors;
    
    public ConfigValidator(BloodpineCore plugin) {
        this.plugin = plugin;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }
    
    /**
     * Validate the configuration file
     * @return true if configuration is valid
     */
    public boolean validate() {
        warnings.clear();
        errors.clear();
        
        validateTokenSettings();
        validateStatSettings();
        validateMarkedSettings();
        validateLifestealSettings();
        validateRebirthSettings();
        validateDailyRewards();
        validateCooldowns();
        
        // Report findings
        if (!warnings.isEmpty()) {
            plugin.getLogger().warning("Configuration validation warnings:");
            for (String warning : warnings) {
                plugin.getLogger().warning("  - " + warning);
            }
        }
        
        if (!errors.isEmpty()) {
            plugin.getLogger().severe("Configuration validation errors:");
            for (String error : errors) {
                plugin.getLogger().severe("  - " + error);
            }
            return false;
        }
        
        plugin.getLogger().info("Configuration validation passed" + 
            (warnings.isEmpty() ? "" : " with " + warnings.size() + " warning(s)"));
        return true;
    }
    
    private void validateTokenSettings() {
        int perKill = plugin.getConfig().getInt("tokens.per-kill", 1);
        int maxTotal = plugin.getConfig().getInt("tokens.max-total", 50);
        int markedPenalty = plugin.getConfig().getInt("tokens.marked-death-penalty", 5);
        
        if (perKill <= 0) {
            errors.add("tokens.per-kill must be greater than 0");
        }
        if (maxTotal <= 0) {
            errors.add("tokens.max-total must be greater than 0");
        }
        if (markedPenalty < 0) {
            warnings.add("tokens.marked-death-penalty is negative");
        }
        if (markedPenalty > maxTotal) {
            warnings.add("tokens.marked-death-penalty exceeds max-total");
        }
    }
    
    private void validateStatSettings() {
        ConfigurationSection stats = plugin.getConfig().getConfigurationSection("stats");
        if (stats == null) {
            errors.add("Missing 'stats' configuration section");
            return;
        }
        
        String[] statTypes = {"damage", "defense", "crystal", "totem", "vitality"};
        
        for (String stat : statTypes) {
            if (!stats.contains(stat)) {
                warnings.add("Missing stat configuration for: " + stat);
                continue;
            }
            
            int maxTokens = stats.getInt(stat + ".max-tokens", 0);
            double perToken = stats.getDouble(stat + ".per-token", 0);
            
            if (maxTokens < 0) {
                errors.add("stats." + stat + ".max-tokens cannot be negative");
            }
            if (perToken < 0) {
                errors.add("stats." + stat + ".per-token cannot be negative");
            }
        }
    }
    
    private void validateMarkedSettings() {
        int threshold = plugin.getConfig().getInt("marked.threshold", 25);
        double bountyMultiplier = plugin.getConfig().getDouble("marked.bounty-multiplier", 2.0);
        
        if (threshold <= 0) {
            errors.add("marked.threshold must be greater than 0");
        }
        if (bountyMultiplier <= 0) {
            warnings.add("marked.bounty-multiplier should be positive");
        }
    }
    
    private void validateLifestealSettings() {
        ConfigurationSection lifesteal = plugin.getConfig().getConfigurationSection("lifesteal");
        if (lifesteal == null) {
            warnings.add("Missing 'lifesteal' configuration section");
            return;
        }
        
        int startingHearts = lifesteal.getInt("starting-hearts", 10);
        int maxHearts = lifesteal.getInt("max-hearts", 20);
        int minHearts = lifesteal.getInt("min-hearts", 1);
        
        if (startingHearts <= 0) {
            errors.add("lifesteal.starting-hearts must be greater than 0");
        }
        if (maxHearts <= startingHearts) {
            warnings.add("lifesteal.max-hearts should be greater than starting-hearts");
        }
        if (minHearts <= 0) {
            errors.add("lifesteal.min-hearts must be greater than 0");
        }
        if (minHearts >= startingHearts) {
            warnings.add("lifesteal.min-hearts should be less than starting-hearts");
        }
    }
    
    private void validateRebirthSettings() {
        ConfigurationSection rebirth = plugin.getConfig().getConfigurationSection("rebirth");
        if (rebirth == null) {
            warnings.add("Missing 'rebirth' configuration section");
            return;
        }
        
        int requirement = rebirth.getInt("requirement-tokens", 50);
        double damageBonus = rebirth.getDouble("damage-bonus-per-level", 0.03);
        double defenseBonus = rebirth.getDouble("defense-bonus-per-level", 0.02);
        
        if (requirement <= 0) {
            errors.add("rebirth.requirement-tokens must be greater than 0");
        }
        if (damageBonus < 0 || damageBonus > 1.0) {
            warnings.add("rebirth.damage-bonus-per-level should be between 0 and 1.0");
        }
        if (defenseBonus < 0 || defenseBonus > 1.0) {
            warnings.add("rebirth.defense-bonus-per-level should be between 0 and 1.0");
        }
    }
    
    private void validateDailyRewards() {
        ConfigurationSection daily = plugin.getConfig().getConfigurationSection("daily-rewards");
        if (daily == null) {
            warnings.add("Missing 'daily-rewards' configuration section - using defaults");
            return;
        }
        
        int baseTokens = daily.getInt("base-tokens", 3);
        int maxStreak = daily.getInt("max-streak-bonus", 6);
        
        if (baseTokens <= 0) {
            warnings.add("daily-rewards.base-tokens should be greater than 0");
        }
        if (maxStreak < 0) {
            warnings.add("daily-rewards.max-streak-bonus should not be negative");
        }
    }
    
    private void validateCooldowns() {
        ConfigurationSection cooldowns = plugin.getConfig().getConfigurationSection("cooldowns");
        if (cooldowns == null) {
            warnings.add("Missing 'cooldowns' configuration section - using defaults");
            return;
        }
        
        for (String key : cooldowns.getKeys(false)) {
            int value = cooldowns.getInt(key, 0);
            if (value < 0) {
                warnings.add("cooldowns." + key + " should not be negative");
            }
        }
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
