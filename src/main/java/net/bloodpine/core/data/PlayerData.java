package net.bloodpine.core.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    
    private final UUID uuid;
    private String name;
    private int totalTokens;
    private Map<StatType, Integer> allocatedTokens;
    private int allocatedTokenCost;
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;
    private int longestKillstreak;
    private boolean isMarked;
    private long markedSince;
    private int lifestealHearts; // Extra hearts from lifesteal (can be negative)
    private int rebirthLevel;
    private int rebirthPoints;
    private int bloodForgeLevel;
    private int insuredHearts;
    private final Set<String> redeemedCodes;
    
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.totalTokens = 0;
        this.allocatedTokens = new HashMap<>();
        this.allocatedTokenCost = 0;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.totalAssists = 0;
        this.longestKillstreak = 0;
        this.isMarked = false;
        this.markedSince = 0;
        this.lifestealHearts = 0;
        this.rebirthLevel = 0;
        this.rebirthPoints = 0;
        this.bloodForgeLevel = 0;
        this.insuredHearts = 0;
        this.redeemedCodes = new HashSet<>();
        
        // Initialize all stat types to 0
        for (StatType type : StatType.values()) {
            allocatedTokens.put(type, 0);
        }
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public void addTokens(int amount) {
        this.totalTokens += amount;
    }
    
    public void removeTokens(int amount) {
        this.totalTokens = Math.max(0, this.totalTokens - amount);
    }
    
    public int getAllocatedTokens(StatType type) {
        return allocatedTokens.getOrDefault(type, 0);
    }
    
    public void setAllocatedTokens(StatType type, int amount) {
        allocatedTokens.put(type, amount);
    }
    
    public void allocateToken(StatType type) {
        allocatedTokens.put(type, allocatedTokens.getOrDefault(type, 0) + 1);
    }
    
    public int getTotalAllocatedTokens() {
        return allocatedTokens.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getAllocatedTokenCost() {
        return allocatedTokenCost;
    }

    public void setAllocatedTokenCost(int allocatedTokenCost) {
        this.allocatedTokenCost = Math.max(0, allocatedTokenCost);
    }

    public void addAllocatedTokenCost(int amount) {
        this.allocatedTokenCost = Math.max(0, this.allocatedTokenCost + amount);
    }
    
    public int getAvailableTokens() {
        return totalTokens - allocatedTokenCost;
    }
    
    public Map<StatType, Integer> getAllocatedTokensMap() {
        return new HashMap<>(allocatedTokens);
    }
    
    public void resetAllocations() {
        for (StatType type : StatType.values()) {
            allocatedTokens.put(type, 0);
        }
        allocatedTokenCost = 0;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }
    
    public void addKill() {
        this.totalKills++;
    }
    
    public int getTotalDeaths() {
        return totalDeaths;
    }
    
    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
    
    public void addDeath() {
        this.totalDeaths++;
    }

    public int getTotalAssists() {
        return totalAssists;
    }

    public void setTotalAssists(int totalAssists) {
        this.totalAssists = Math.max(0, totalAssists);
    }

    public void addAssist() {
        this.totalAssists++;
    }

    public int getLongestKillstreak() {
        return longestKillstreak;
    }

    public void setLongestKillstreak(int longestKillstreak) {
        this.longestKillstreak = Math.max(0, longestKillstreak);
    }

    public void recordKillstreak(int streak) {
        if (streak > this.longestKillstreak) {
            this.longestKillstreak = streak;
        }
    }
    
    public boolean isMarked() {
        return isMarked;
    }
    
    public void setMarked(boolean marked) {
        this.isMarked = marked;
        if (marked) {
            this.markedSince = System.currentTimeMillis();
        } else {
            this.markedSince = 0;
        }
    }
    
    public long getMarkedSince() {
        return markedSince;
    }
    
    public int getLifestealHearts() {
        return lifestealHearts;
    }
    
    public void setLifestealHearts(int lifestealHearts) {
        this.lifestealHearts = lifestealHearts;
    }
    
    public void addLifestealHeart() {
        this.lifestealHearts++;
    }
    
    public void removeLifestealHeart() {
        this.lifestealHearts--;
    }
    
    public double getKDRatio() {
        if (totalDeaths == 0) {
            return totalKills;
        }
        return (double) totalKills / totalDeaths;
    }

    public int getRebirthLevel() {
        return rebirthLevel;
    }

    public void setRebirthLevel(int rebirthLevel) {
        this.rebirthLevel = Math.max(0, rebirthLevel);
    }

    public int getRebirthPoints() {
        return rebirthPoints;
    }

    public void setRebirthPoints(int rebirthPoints) {
        this.rebirthPoints = Math.max(0, rebirthPoints);
    }

    public void addRebirthLevels(int amount) {
        this.rebirthLevel = Math.max(0, this.rebirthLevel + amount);
    }

    public void addRebirthPoints(int amount) {
        this.rebirthPoints = Math.max(0, this.rebirthPoints + amount);
    }

    public int getBloodForgeLevel() {
        return bloodForgeLevel;
    }

    public void setBloodForgeLevel(int bloodForgeLevel) {
        this.bloodForgeLevel = Math.max(0, bloodForgeLevel);
    }

    public void addBloodForgeLevel(int amount) {
        this.bloodForgeLevel = Math.max(0, this.bloodForgeLevel + amount);
    }

    public int getInsuredHearts() {
        return insuredHearts;
    }

    public void setInsuredHearts(int insuredHearts) {
        this.insuredHearts = Math.max(0, insuredHearts);
    }

    public void addInsuredHearts(int amount) {
        this.insuredHearts = Math.max(0, this.insuredHearts + amount);
    }

    public boolean consumeInsuredHeart() {
        if (insuredHearts <= 0) {
            return false;
        }
        insuredHearts--;
        return true;
    }

    public boolean hasRedeemedCode(String code) {
        return redeemedCodes.contains(code.toUpperCase());
    }

    public void markCodeRedeemed(String code) {
        redeemedCodes.add(code.toUpperCase());
    }

    public Set<String> getRedeemedCodes() {
        return new HashSet<>(redeemedCodes);
    }

    public void setRedeemedCodes(Set<String> codes) {
        redeemedCodes.clear();
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                redeemedCodes.add(code.toUpperCase());
            }
        }
    }
}
