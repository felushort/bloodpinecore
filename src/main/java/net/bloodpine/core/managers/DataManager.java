package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    
    private final BloodpineCore plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public DataManager(BloodpineCore plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        setupDataFile();
    }
    
    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        
        for (String uuidString : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);
            
            String name = playerSection.getString("name", "Unknown");
            PlayerData data = new PlayerData(uuid, name);
            
            data.setTotalTokens(playerSection.getInt("totalTokens", 0));
            data.setTotalKills(playerSection.getInt("totalKills", 0));
            data.setTotalDeaths(playerSection.getInt("totalDeaths", 0));
            data.setTotalAssists(playerSection.getInt("totalAssists", 0));
            data.setLongestKillstreak(playerSection.getInt("longestKillstreak", 0));
            data.setMarked(playerSection.getBoolean("marked", false));
            data.setLifestealHearts(playerSection.getInt("lifestealHearts", 0));
            data.setRebirthLevel(playerSection.getInt("rebirthLevel", 0));
            data.setRebirthPoints(playerSection.getInt("rebirthPoints", 0));
            data.setBloodForgeLevel(playerSection.getInt("bloodForgeLevel", 0));
            data.setInsuredHearts(playerSection.getInt("insuredHearts", 0));
            data.setRedeemedCodes(new HashSet<>(playerSection.getStringList("redeemedCodes")));
            
            // Load allocated tokens
            ConfigurationSection statsSection = playerSection.getConfigurationSection("stats");
            if (statsSection != null) {
                for (StatType type : StatType.values()) {
                    int allocated = statsSection.getInt(type.getConfigKey(), 0);
                    data.setAllocatedTokens(type, allocated);
                }
            }

            int legacyAllocated = data.getTotalAllocatedTokens();
            int allocatedCost = playerSection.contains("allocatedTokenCost")
                    ? playerSection.getInt("allocatedTokenCost", legacyAllocated)
                    : legacyAllocated;
            data.setAllocatedTokenCost(allocatedCost);

            int crystalCap = plugin.getConfig().getInt("stats.crystal.max-tokens", 0);
            int crystalAllocated = data.getAllocatedTokens(StatType.CRYSTAL);
            if (crystalCap <= 0 && crystalAllocated > 0) {
                data.setAllocatedTokens(StatType.CRYSTAL, 0);
                data.setAllocatedTokenCost(Math.min(data.getAllocatedTokenCost(), data.getTotalAllocatedTokens()));
            }
            
            playerDataMap.put(uuid, data);
        }
        
        plugin.getLogger().info("Loaded data for " + playerDataMap.size() + " players");
    }
    
    public void saveData() {
        for (PlayerData data : playerDataMap.values()) {
            String path = "players." + data.getUuid().toString();
            
            dataConfig.set(path + ".name", data.getName());
            dataConfig.set(path + ".totalTokens", data.getTotalTokens());
            dataConfig.set(path + ".totalKills", data.getTotalKills());
            dataConfig.set(path + ".totalDeaths", data.getTotalDeaths());
            dataConfig.set(path + ".totalAssists", data.getTotalAssists());
            dataConfig.set(path + ".longestKillstreak", data.getLongestKillstreak());
            dataConfig.set(path + ".marked", data.isMarked());
            dataConfig.set(path + ".lifestealHearts", data.getLifestealHearts());
            dataConfig.set(path + ".rebirthLevel", data.getRebirthLevel());
            dataConfig.set(path + ".rebirthPoints", data.getRebirthPoints());
            dataConfig.set(path + ".bloodForgeLevel", data.getBloodForgeLevel());
            dataConfig.set(path + ".insuredHearts", data.getInsuredHearts());
            dataConfig.set(path + ".allocatedTokenCost", data.getAllocatedTokenCost());
            dataConfig.set(path + ".redeemedCodes", new ArrayList<>(data.getRedeemedCodes()));
            
            // Save allocated tokens
            for (StatType type : StatType.values()) {
                dataConfig.set(path + ".stats." + type.getConfigKey(), data.getAllocatedTokens(type));
            }
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> {
            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : "Unknown";
            return new PlayerData(uuid, name);
        });
    }
    
    public PlayerData getPlayerData(Player player) {
        PlayerData data = getPlayerData(player.getUniqueId());
        data.setName(player.getName());
        return data;
    }
    
    public Collection<PlayerData> getAllPlayerData() {
        return playerDataMap.values();
    }
    
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> sorted = new ArrayList<>(playerDataMap.values());
        sorted.sort((a, b) -> Integer.compare(b.getTotalTokens(), a.getTotalTokens()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
    
    public void resetAllData() {
        playerDataMap.clear();
        dataConfig = new YamlConfiguration();
        saveData();
    }
    
    public void resetPlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
        dataConfig.set("players." + uuid.toString(), null);
        saveData();
    }
}
