package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExpansionDataManager {

    private final BloodpineCore plugin;
    private final Map<UUID, ExpansionProfile> profiles = new HashMap<>();
    private File file;
    private FileConfiguration config;
    private long seasonStartMillis = System.currentTimeMillis();
    private int seasonNumber = 1;
    private boolean seasonFinalHourTriggered;
    private boolean seasonRewardsDistributed;

    public ExpansionDataManager(BloodpineCore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        file = new File(plugin.getDataFolder(), "expansion-data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create expansion-data.yml");
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
        profiles.clear();

        ConfigurationSection root = config.getConfigurationSection("players");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) continue;

                ExpansionProfile profile = new ExpansionProfile();
                profile.reputation = section.getInt("reputation", 0);
                profile.fame = section.getInt("fame", 0);
                profile.infamy = section.getInt("infamy", 0);
                profile.ascension = section.getInt("ascension", 0);
                profile.dailyQuestDate = section.getString("dailyQuestDate", "");
                profile.dailyQuestProgress = section.getInt("dailyQuestProgress", 0);
                profile.dailyQuestClaimed = section.getBoolean("dailyQuestClaimed", false);
                profile.contractTarget = section.getString("contractTarget", "");
                profile.contractProgress = section.getInt("contractProgress", 0);
                profile.contractGoal = section.getInt("contractGoal", 2);
                profile.parkourBestMillis = section.getLong("parkourBestMillis", 0L);
                profile.heartsGainedSeason = section.getInt("heartsGainedSeason", 0);
                profile.contractsCompletedSeason = section.getInt("contractsCompletedSeason", 0);
                profile.contractsCompletedTotal = section.getInt("contractsCompletedTotal", 0);
                profile.seasonTop10Finishes = section.getInt("seasonTop10Finishes", 0);
                profile.seasonTop3Finishes = section.getInt("seasonTop3Finishes", 0);
                profile.seasonChampionFinishes = section.getInt("seasonChampionFinishes", 0);
                profile.lastSeasonRank = section.getInt("lastSeasonRank", 0);

                profiles.put(uuid, profile);
            }
        }

        seasonStartMillis = config.getLong("season-state.start-millis", System.currentTimeMillis());
        seasonNumber = Math.max(1, config.getInt("season-state.number", 1));
        seasonFinalHourTriggered = config.getBoolean("season-state.final-hour-triggered", false);
        seasonRewardsDistributed = config.getBoolean("season-state.rewards-distributed", false);
    }

    public void save() {
        config.set("season-state.start-millis", seasonStartMillis);
        config.set("season-state.number", seasonNumber);
        config.set("season-state.final-hour-triggered", seasonFinalHourTriggered);
        config.set("season-state.rewards-distributed", seasonRewardsDistributed);

        for (Map.Entry<UUID, ExpansionProfile> entry : profiles.entrySet()) {
            String path = "players." + entry.getKey();
            ExpansionProfile profile = entry.getValue();

            config.set(path + ".reputation", profile.reputation);
            config.set(path + ".fame", profile.fame);
            config.set(path + ".infamy", profile.infamy);
            config.set(path + ".ascension", profile.ascension);
            config.set(path + ".dailyQuestDate", profile.dailyQuestDate);
            config.set(path + ".dailyQuestProgress", profile.dailyQuestProgress);
            config.set(path + ".dailyQuestClaimed", profile.dailyQuestClaimed);
            config.set(path + ".contractTarget", profile.contractTarget);
            config.set(path + ".contractProgress", profile.contractProgress);
            config.set(path + ".contractGoal", profile.contractGoal);
            config.set(path + ".parkourBestMillis", profile.parkourBestMillis);
            config.set(path + ".heartsGainedSeason", profile.heartsGainedSeason);
            config.set(path + ".contractsCompletedSeason", profile.contractsCompletedSeason);
            config.set(path + ".contractsCompletedTotal", profile.contractsCompletedTotal);
            config.set(path + ".seasonTop10Finishes", profile.seasonTop10Finishes);
            config.set(path + ".seasonTop3Finishes", profile.seasonTop3Finishes);
            config.set(path + ".seasonChampionFinishes", profile.seasonChampionFinishes);
            config.set(path + ".lastSeasonRank", profile.lastSeasonRank);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save expansion-data.yml");
            e.printStackTrace();
        }
    }

    public ExpansionProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public ExpansionProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new ExpansionProfile());
    }

    public void resetDailyIfNeeded(Player player) {
        ExpansionProfile profile = getProfile(player);
        String today = LocalDate.now().toString();
        if (!today.equals(profile.dailyQuestDate)) {
            profile.dailyQuestDate = today;
            profile.dailyQuestProgress = 0;
            profile.dailyQuestClaimed = false;
        }
    }

    public long getSeasonStartMillis() {
        return seasonStartMillis;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public boolean isSeasonFinalHourTriggered() {
        return seasonFinalHourTriggered;
    }

    public void setSeasonFinalHourTriggered(boolean seasonFinalHourTriggered) {
        this.seasonFinalHourTriggered = seasonFinalHourTriggered;
    }

    public boolean isSeasonRewardsDistributed() {
        return seasonRewardsDistributed;
    }

    public void setSeasonRewardsDistributed(boolean seasonRewardsDistributed) {
        this.seasonRewardsDistributed = seasonRewardsDistributed;
    }

    public void beginNextSeason(boolean resetSeasonCounters) {
        seasonNumber++;
        seasonStartMillis = System.currentTimeMillis();
        seasonFinalHourTriggered = false;
        seasonRewardsDistributed = false;

        if (resetSeasonCounters) {
            for (ExpansionProfile profile : profiles.values()) {
                profile.resetSeasonCounters();
            }
        }
    }

    public static class ExpansionProfile {
        private int reputation;
        private int fame;
        private int infamy;
        private int ascension;
        private int heartsGainedSeason;
        private int contractsCompletedSeason;
        private int contractsCompletedTotal;
        private int seasonTop10Finishes;
        private int seasonTop3Finishes;
        private int seasonChampionFinishes;
        private int lastSeasonRank;
        private String dailyQuestDate = "";
        private int dailyQuestProgress;
        private boolean dailyQuestClaimed;
        private String contractTarget = "";
        private int contractProgress;
        private int contractGoal = 2;
        private long parkourBestMillis;
        private long parkourStartMillis;

        public int getReputation() {
            return reputation;
        }

        public void addReputation(int amount) {
            reputation += amount;
        }

        public int getFame() {
            return fame;
        }

        public void addFame(int amount) {
            fame += amount;
        }

        public int getInfamy() {
            return infamy;
        }

        public void addInfamy(int amount) {
            infamy += amount;
        }

        public int getAscension() {
            return ascension;
        }

        public void addAscension(int amount) {
            ascension = Math.max(0, ascension + amount);
        }

        public int getHeartsGainedSeason() {
            return heartsGainedSeason;
        }

        public void addHeartsGainedSeason(int amount) {
            heartsGainedSeason = Math.max(0, heartsGainedSeason + amount);
        }

        public int getContractsCompletedSeason() {
            return contractsCompletedSeason;
        }

        public int getContractsCompletedTotal() {
            return contractsCompletedTotal;
        }

        public void addContractCompletion() {
            contractsCompletedSeason++;
            contractsCompletedTotal++;
        }

        public int getSeasonTop10Finishes() {
            return seasonTop10Finishes;
        }

        public int getSeasonTop3Finishes() {
            return seasonTop3Finishes;
        }

        public int getSeasonChampionFinishes() {
            return seasonChampionFinishes;
        }

        public int getLastSeasonRank() {
            return lastSeasonRank;
        }

        public void addSeasonPlacement(int rank) {
            if (rank <= 0) {
                return;
            }
            if (rank <= 10) {
                seasonTop10Finishes++;
            }
            if (rank <= 3) {
                seasonTop3Finishes++;
            }
            if (rank == 1) {
                seasonChampionFinishes++;
            }
            lastSeasonRank = rank;
        }

        public void resetSeasonCounters() {
            heartsGainedSeason = 0;
            contractsCompletedSeason = 0;
            lastSeasonRank = 0;
            dailyQuestProgress = 0;
            dailyQuestClaimed = false;
            contractTarget = "";
            contractProgress = 0;
            contractGoal = 2;
            parkourStartMillis = 0L;
        }

        public int getDailyQuestProgress() {
            return dailyQuestProgress;
        }

        public void addDailyQuestProgress(int amount) {
            dailyQuestProgress = Math.max(0, dailyQuestProgress + amount);
        }

        public boolean isDailyQuestClaimed() {
            return dailyQuestClaimed;
        }

        public void setDailyQuestClaimed(boolean dailyQuestClaimed) {
            this.dailyQuestClaimed = dailyQuestClaimed;
        }

        public String getContractTarget() {
            return contractTarget;
        }

        public void setContractTarget(String contractTarget) {
            this.contractTarget = contractTarget == null ? "" : contractTarget;
        }

        public int getContractProgress() {
            return contractProgress;
        }

        public void setContractProgress(int contractProgress) {
            this.contractProgress = Math.max(0, contractProgress);
        }

        public int getContractGoal() {
            return contractGoal;
        }

        public void setContractGoal(int contractGoal) {
            this.contractGoal = Math.max(1, contractGoal);
        }

        public long getParkourBestMillis() {
            return parkourBestMillis;
        }

        public void setParkourBestMillis(long parkourBestMillis) {
            this.parkourBestMillis = Math.max(0L, parkourBestMillis);
        }

        public long getParkourStartMillis() {
            return parkourStartMillis;
        }

        public void setParkourStartMillis(long parkourStartMillis) {
            this.parkourStartMillis = Math.max(0L, parkourStartMillis);
        }
    }
}
