package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RedeemCodeManager {

    public record RedeemReward(int tokens, int hearts, int totems, int goldenApples, int experienceBottles) {
    }

    private final BloodpineCore plugin;
    private final Map<String, RedeemReward> rewards = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public RedeemCodeManager(BloodpineCore plugin) {
        this.plugin = plugin;
        setupFile();
        load();
    }

    private void setupFile() {
        dataFile = new File(plugin.getDataFolder(), "redeem-codes.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create redeem-codes.yml!");
                exception.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void load() {
        rewards.clear();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection codes = dataConfig.getConfigurationSection("codes");
        if (codes != null) {
            for (String rawCode : codes.getKeys(false)) {
                String code = normalize(rawCode);
                String base = "codes." + rawCode + ".";
                int tokens = Math.max(0, dataConfig.getInt(base + "tokens", 0));
                int hearts = Math.max(0, dataConfig.getInt(base + "hearts", 0));
                int totems = Math.max(0, dataConfig.getInt(base + "totems", 0));
                int goldenApples = Math.max(0, dataConfig.getInt(base + "goldenApples", 0));
                int experienceBottles = Math.max(0, dataConfig.getInt(base + "experienceBottles", 0));
                rewards.put(code, new RedeemReward(tokens, hearts, totems, goldenApples, experienceBottles));
            }
        }

        if (!rewards.containsKey("RELEASE")) {
            rewards.put("RELEASE", new RedeemReward(20, 1, 1, 8, 16));
            save();
        }
    }

    public void save() {
        dataConfig.set("codes", null);
        for (Map.Entry<String, RedeemReward> entry : rewards.entrySet()) {
            String code = entry.getKey();
            RedeemReward reward = entry.getValue();
            String base = "codes." + code + ".";
            dataConfig.set(base + "tokens", reward.tokens());
            dataConfig.set(base + "hearts", reward.hearts());
            dataConfig.set(base + "totems", reward.totems());
            dataConfig.set(base + "goldenApples", reward.goldenApples());
            dataConfig.set(base + "experienceBottles", reward.experienceBottles());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save redeem-codes.yml!");
            exception.printStackTrace();
        }
    }

    public RedeemReward getReward(String code) {
        return rewards.get(normalize(code));
    }

    public void setCode(String code, RedeemReward reward) {
        rewards.put(normalize(code), reward);
    }

    public boolean removeCode(String code) {
        return rewards.remove(normalize(code)) != null;
    }

    public Set<String> getCodes() {
        return Collections.unmodifiableSet(rewards.keySet());
    }

    public Map<String, RedeemReward> getAllRewards() {
        return Collections.unmodifiableMap(rewards);
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
