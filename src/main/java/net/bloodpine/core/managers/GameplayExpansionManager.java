package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.utils.ItemMetaBuilder;
import net.bloodpine.core.utils.ItemUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GameplayExpansionManager {

    private final BloodpineCore plugin;
    private final Random random = new Random();

    private boolean bloodMoonActive;
    private long bloodMoonEndsAt;

    private boolean arenaEventActive;
    private boolean huntMarkedActive;
    private long huntMarkedEndsAt;
    private BukkitTask bloodMoonEndTask;
    private BukkitTask huntMarkedEndTask;

    private UUID activeBossUuid;
    private final Map<UUID, Double> bossDamage = new HashMap<>();
    private final Set<UUID> rogueAssassins = new HashSet<>();
    private UUID titanZombieUuid;
    private Location titanLastLocation;
    private long titanLastShakeMillis;
    private long titanLastStompMillis;
    private final Map<UUID, EliteMobType> eliteMobs = new HashMap<>();
    private final Map<UUID, Long> eliteAbilityCooldowns = new HashMap<>();

    private final Map<UUID, Integer> weaponKillCounter = new HashMap<>();

    private final Map<UUID, Deque<DamageLog>> incomingDamageLogs = new HashMap<>();
    private final Map<UUID, Deque<DamageLog>> outgoingDamageLogs = new HashMap<>();
    private final Map<UUID, Map<UUID, AssistContribution>> assistContributions = new HashMap<>();
    private final Map<UUID, KillComboState> killComboStates = new HashMap<>();
    private final Map<UUID, KillRecord> lastKilledBy = new HashMap<>();
    private long nextSupplyDropAtMillis;

    private boolean finalHourActive;
    private long finalHourEndsAt;

    public GameplayExpansionManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public void startSchedulers() {
        long bloodMoonIntervalMinutes = plugin.getConfig().getLong("events.blood-moon.interval-minutes", 120);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::startBloodMoon,
                20L * 60L, 20L * 60L * bloodMoonIntervalMinutes);

        long huntIntervalMinutes = plugin.getConfig().getLong("events.hunt-marked.interval-minutes", 90);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::startHuntMarked,
                20L * 120L, 20L * 60L * huntIntervalMinutes);

        if (plugin.getConfig().getBoolean("boss.auto-spawn-enabled", false)) {
            long bossIntervalMinutes = Math.max(1L, plugin.getConfig().getLong("boss.interval-minutes", 360));
            long bossIntervalTicks = 20L * 60L * bossIntervalMinutes;
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnBoss,
                bossIntervalTicks, bossIntervalTicks);
        }

        long randomEventIntervalMinutes = plugin.getConfig().getLong("events.random-world.interval-minutes", 45);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::triggerRandomWorldEvent,
                20L * 180L, 20L * 60L * randomEventIntervalMinutes);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickCustomMobAbilities, 20L * 5L, 10L);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickInfamyVisibility, 20L * 20L, 20L * 20L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickCosmetics, 20L * 3L, 20L * 3L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSeasonLifecycle, 20L * 30L, 20L * 60L);

        if (plugin.getConfig().getBoolean("supply-drop.enabled", true)) {
            long intervalMinutes = Math.max(1L, plugin.getConfig().getLong("supply-drop.interval-minutes", 90L));
            long intervalTicks = intervalMinutes * 60L * 20L;
            nextSupplyDropAtMillis = System.currentTimeMillis() + (intervalMinutes * 60_000L);
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnScheduledSupplyDrop, intervalTicks, intervalTicks);
        } else {
            nextSupplyDropAtMillis = 0L;
        }
    }

    public void startBloodMoon() {
        if (bloodMoonActive) return;
        bloodMoonActive = true;
        int durationMinutes = plugin.getConfig().getInt("events.blood-moon.duration-minutes", 20);
        bloodMoonEndsAt = System.currentTimeMillis() + durationMinutes * 60_000L;

        Bukkit.broadcastMessage(colorize("&4&lBLOOD MOON &7has begun! &c2x token rewards for " + durationMinutes + " minutes."));
        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f));

        if (bloodMoonEndTask != null) {
            bloodMoonEndTask.cancel();
            bloodMoonEndTask = null;
        }

        bloodMoonEndTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            bloodMoonActive = false;
            bloodMoonEndTask = null;
            Bukkit.broadcastMessage(colorize("&8Blood Moon has faded. Token rewards are back to normal."));
        }, durationMinutes * 60L * 20L);
    }

    public void startHuntMarked() {
        if (huntMarkedActive) return;
        huntMarkedActive = true;
        int durationMinutes = plugin.getConfig().getInt("events.hunt-marked.duration-minutes", 15);
        huntMarkedEndsAt = System.currentTimeMillis() + durationMinutes * 60_000L;
        Bukkit.broadcastMessage(colorize("&c&lHUNT THE MARKED &7is live for " + durationMinutes + " minutes!"));

        if (huntMarkedEndTask != null) {
            huntMarkedEndTask.cancel();
            huntMarkedEndTask = null;
        }

        huntMarkedEndTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            huntMarkedActive = false;
            huntMarkedEndTask = null;
            Bukkit.broadcastMessage(colorize("&7Hunt the Marked has ended."));
        }, durationMinutes * 60L * 20L);
    }

    public void stopBloodMoon() {
        if (!bloodMoonActive) return;
        bloodMoonActive = false;
        bloodMoonEndsAt = 0L;
        if (bloodMoonEndTask != null) {
            bloodMoonEndTask.cancel();
            bloodMoonEndTask = null;
        }
    }

    public void stopHuntMarked() {
        if (!huntMarkedActive) return;
        huntMarkedActive = false;
        huntMarkedEndsAt = 0L;
        if (huntMarkedEndTask != null) {
            huntMarkedEndTask.cancel();
            huntMarkedEndTask = null;
        }
    }

    public void startArenaEvent() {
        arenaEventActive = true;
        Bukkit.broadcastMessage(colorize("&6&lARENA EVENT &7started! Arena fighters now use normalized gear."));
    }

    public void stopArenaEvent() {
        arenaEventActive = false;
        Bukkit.broadcastMessage(colorize("&7Arena event ended."));
    }

    public void stopArenaEventSilently() {
        arenaEventActive = false;
    }

    public boolean isBloodMoonActive() {
        return bloodMoonActive && System.currentTimeMillis() < bloodMoonEndsAt;
    }

    public boolean isArenaEventActive() {
        return arenaEventActive;
    }

    public boolean isHuntMarkedActive() {
        return huntMarkedActive && System.currentTimeMillis() < huntMarkedEndsAt;
    }

    public double getKillTokenMultiplier(Player killer, Player victim) {
        double multiplier = 1.0;

        if (isBloodMoonActive()) {
            multiplier *= 2.0;
        }

        if (isFinalHourActive()) {
            multiplier *= plugin.getConfig().getDouble("season.final-hour.token-multiplier", 2.0);
        }

        if (isHuntMarkedActive() && plugin.getMarkedManager().isMarked(victim)) {
            multiplier *= plugin.getConfig().getDouble("events.hunt-marked.marked-kill-multiplier", 1.75);
        }

        if (isSameTeam(killer, victim)) {
            multiplier *= plugin.getConfig().getDouble("team-balance.same-team-kill-multiplier", 0.2);
        }

        int allies = getNearbyAllies(killer,
                plugin.getConfig().getInt("team-balance.ally-check-radius", 40));

        if (allies <= 0) {
            multiplier *= plugin.getConfig().getDouble("team-balance.solo-kill-multiplier", 1.25);
        }

        int nerfStart = plugin.getConfig().getInt("team-balance.proximity-nerf-start-allies", 2);
        if (allies > nerfStart) {
            double nerfPerAlly = plugin.getConfig().getDouble("team-balance.proximity-nerf-per-ally", 0.12);
            int extra = allies - nerfStart;
            multiplier *= Math.max(0.2, 1.0 - (extra * nerfPerAlly));
        }

        int teamCap = plugin.getConfig().getInt("team-balance.team-size-cap", 5);
        int teamSize = getTeamSize(killer);
        if (teamSize > teamCap) {
            multiplier *= plugin.getConfig().getDouble("team-balance.over-cap-team-kill-multiplier", 0.5);
        }

        return Math.max(0.1, multiplier);
    }

    public double getDamageModifier(Player attacker) {
        int allies = getNearbyAllies(attacker,
                plugin.getConfig().getInt("team-balance.ally-check-radius", 40));
        int nerfStart = plugin.getConfig().getInt("team-balance.proximity-nerf-start-allies", 2);

        double modifier = 1.0;
        if (allies > nerfStart) {
            double nerfPerAlly = plugin.getConfig().getDouble("team-balance.proximity-damage-nerf-per-ally", 0.08);
            modifier *= Math.max(0.6, 1.0 - (allies - nerfStart) * nerfPerAlly);
        }

        if (allies <= 0) {
            modifier *= plugin.getConfig().getDouble("team-balance.solo-damage-multiplier", 1.08);
        }

        return modifier;
    }

    public boolean isInBloodZone(Location location) {
        if (!plugin.getConfig().getBoolean("high-risk-zone.enabled", true)) return false;
        if (location == null || location.getWorld() == null) return false;

        World world = Bukkit.getWorld(plugin.getConfig().getString("high-risk-zone.world", "world"));
        if (world == null || !world.equals(location.getWorld())) return false;

        if (isFinalHourActive() && plugin.getConfig().getBoolean("season.final-hour.blood-zone-global", true)) {
            return true;
        }

        int x1 = plugin.getConfig().getInt("high-risk-zone.x1", -2500);
        int z1 = plugin.getConfig().getInt("high-risk-zone.z1", -2500);
        int x2 = plugin.getConfig().getInt("high-risk-zone.x2", 2500);
        int z2 = plugin.getConfig().getInt("high-risk-zone.z2", 2500);

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public boolean isFinalHourActive() {
        return finalHourActive && System.currentTimeMillis() < finalHourEndsAt;
    }

    public boolean areSafeZonesDisabled() {
        return isFinalHourActive() && plugin.getConfig().getBoolean("season.final-hour.disable-safe-zones", true);
    }

    public boolean isNoTotemZone(Location location) {
        return isInBloodZone(location) && plugin.getConfig().getBoolean("high-risk-zone.no-totems", true);
    }

    public int getExtraHeartLossInBloodZone(Location location) {
        if (!isInBloodZone(location)) return 0;
        return plugin.getConfig().getInt("high-risk-zone.extra-heart-loss-on-death", 1);
    }

    public void normalizeArenaGear(Player player) {
        if (!arenaEventActive) return;
        if (!isInsideArena(player.getLocation())) return;

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);

        player.getInventory().clear();
        player.getInventory().setItemInMainHand(sword);
        player.getInventory().setHelmet(helm);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        player.setFoodLevel(20);
        player.sendMessage(colorize("&6Arena loadout normalized."));
    }

    public boolean isInsideArena(Location location) {
        if (location == null || location.getWorld() == null) return false;
        World world = Bukkit.getWorld(plugin.getConfig().getString("events.arena.world", "world"));
        if (world == null || !world.equals(location.getWorld())) return false;

        int x1 = plugin.getConfig().getInt("events.arena.x1", -100);
        int z1 = plugin.getConfig().getInt("events.arena.z1", -100);
        int x2 = plugin.getConfig().getInt("events.arena.x2", 100);
        int z2 = plugin.getConfig().getInt("events.arena.z2", 100);

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public void spawnBoss() {
        UUID existingBossUuid = findExistingBossUuid();
        if (existingBossUuid != null) {
            activeBossUuid = existingBossUuid;
            return;
        }

        if (activeBossUuid != null) return;

        World world = Bukkit.getWorld(plugin.getConfig().getString("boss.world", "world"));
        if (world == null) return;

        Location loc = resolveBossSpawnLocation(world);

        Warden boss = world.spawn(loc, Warden.class, entity -> {
            entity.setCustomName(colorize("&4&lBlood Tyrant"));
            entity.setCustomNameVisible(true);
            entity.setSilent(false);
        });
        activeBossUuid = boss.getUniqueId();
        bossDamage.clear();

        Bukkit.broadcastMessage(colorize("&4&lWORLD BOSS &7has spawned at &c"
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
    }

    private UUID findExistingBossUuid() {
        String bossName = ChatColor.stripColor(colorize("&4&lBlood Tyrant"));
        for (World world : Bukkit.getWorlds()) {
            for (Warden warden : world.getEntitiesByClass(Warden.class)) {
                if (warden.isDead()) continue;
                String currentName = warden.getCustomName();
                if (currentName == null) continue;
                String stripped = ChatColor.stripColor(currentName);
                if (bossName.equalsIgnoreCase(stripped)) {
                    return warden.getUniqueId();
                }
            }
        }
        return null;
    }

    public void despawnBoss() {
        if (activeBossUuid == null) {
            bossDamage.clear();
            return;
        }

        Entity entity = Bukkit.getEntity(activeBossUuid);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }

        activeBossUuid = null;
        bossDamage.clear();
    }

    public void stopAllEvents(boolean broadcast) {
        stopBloodMoon();
        stopHuntMarked();
        stopArenaEventSilently();
        despawnBoss();
        despawnCustomMobs();

        if (broadcast) {
            Bukkit.broadcastMessage(colorize("&7All active events have been stopped by staff."));
        }
    }

    private void tickSeasonLifecycle() {
        if (!plugin.getConfig().getBoolean("season.lifecycle-enabled", true)) {
            return;
        }

        cleanupAssistTracking();
        cleanupKillBonusTracking();

        int seasonDays = Math.max(1, plugin.getConfig().getInt("season.length", 30));
        long seasonDurationMillis = seasonDays * 24L * 60L * 60L * 1000L;

        long seasonStart = plugin.getExpansionDataManager().getSeasonStartMillis();
        if (seasonStart <= 0L) {
            plugin.getExpansionDataManager().beginNextSeason(false);
            plugin.getExpansionDataManager().save();
            return;
        }

        long seasonEnd = seasonStart + seasonDurationMillis;
        long now = System.currentTimeMillis();

        if (plugin.getConfig().getBoolean("season.final-hour.enabled", true)) {
            long finalHourDurationMinutes = Math.max(1L, plugin.getConfig().getLong("season.final-hour.duration-minutes", 60L));
            long finalHourStart = seasonEnd - (finalHourDurationMinutes * 60_000L);
            if (!plugin.getExpansionDataManager().isSeasonFinalHourTriggered() && now >= finalHourStart && now < seasonEnd) {
                startFinalHourEvent(seasonEnd);
                plugin.getExpansionDataManager().setSeasonFinalHourTriggered(true);
                plugin.getExpansionDataManager().save();
            }
        }

        if (now < seasonEnd) {
            return;
        }

        if (!plugin.getExpansionDataManager().isSeasonRewardsDistributed()) {
            distributeSeasonLeaderboardRewards();
        }
        stopFinalHourEvent();

        boolean resetSeasonCounters = plugin.getConfig().getBoolean("season.reset-season-counters-on-rollover", true);
        plugin.getExpansionDataManager().beginNextSeason(resetSeasonCounters);
        plugin.getExpansionDataManager().save();

        Bukkit.broadcastMessage(colorize("&c&lSeason rollover complete. &7Welcome to &fSeason "
                + plugin.getExpansionDataManager().getSeasonNumber() + "&7."));
    }

    private void cleanupAssistTracking() {
        long now = System.currentTimeMillis();
        for (UUID victimUuid : new ArrayList<>(assistContributions.keySet())) {
            trimAssistContributions(victimUuid, now);
        }
    }

    private void cleanupKillBonusTracking() {
        long now = System.currentTimeMillis();
        long revengeWindowMs = Math.max(60_000L, plugin.getConfig().getLong("bonus-kills.revenge.window-seconds", 600L) * 1000L);
        long comboWindowMs = Math.max(10_000L, plugin.getConfig().getLong("bonus-kills.combo.window-seconds", 45L) * 1000L);

        lastKilledBy.entrySet().removeIf(entry -> now - entry.getValue().timestampMillis() > revengeWindowMs);
        killComboStates.entrySet().removeIf(entry -> now - entry.getValue().lastKillMillis() > comboWindowMs);
    }

    public void startFinalHourEventManually() {
        int seasonDays = Math.max(1, plugin.getConfig().getInt("season.length", 30));
        long seasonDurationMillis = seasonDays * 24L * 60L * 60L * 1000L;
        long seasonEnd = plugin.getExpansionDataManager().getSeasonStartMillis() + seasonDurationMillis;
        startFinalHourEvent(seasonEnd);
        plugin.getExpansionDataManager().setSeasonFinalHourTriggered(true);
        plugin.getExpansionDataManager().save();
    }

    public void stopFinalHourEventManually() {
        stopFinalHourEvent();
        plugin.getExpansionDataManager().setSeasonFinalHourTriggered(false);
        plugin.getExpansionDataManager().save();
        Bukkit.broadcastMessage(colorize("&7Final hour event has been stopped by staff."));
    }

    private void startFinalHourEvent(long seasonEndMillis) {
        if (isFinalHourActive()) {
            return;
        }
        finalHourActive = true;
        finalHourEndsAt = seasonEndMillis;

        Bukkit.broadcastMessage(colorize("&4&lFINAL HOUR &7has begun! Blood Zone has expanded, safe-zones are disabled, and token rewards are boosted."));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 0.8f);
        }
    }

    private void stopFinalHourEvent() {
        finalHourActive = false;
        finalHourEndsAt = 0L;
    }

    public void distributeSeasonLeaderboardRewards() {
        List<PlayerData> leaderboard = new ArrayList<>(plugin.getDataManager().getAllPlayerData());
        leaderboard.sort(Comparator.comparingInt(PlayerData::getTotalTokens).reversed());

        if (leaderboard.isEmpty()) {
            plugin.getExpansionDataManager().setSeasonRewardsDistributed(true);
            plugin.getExpansionDataManager().save();
            return;
        }

        Bukkit.broadcastMessage(colorize("&6&lSEASON REWARDS &7Top 10 players have earned permanent cosmetic prestige."));
        int max = Math.min(10, leaderboard.size());
        for (int index = 0; index < max; index++) {
            int rank = index + 1;
            PlayerData playerData = leaderboard.get(index);
            ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(playerData.getUuid());
            profile.addSeasonPlacement(rank);

            String rankColor = rank == 1 ? "&6" : (rank <= 3 ? "&d" : "&b");
            Bukkit.broadcastMessage(colorize(rankColor + "#" + rank + " &f" + playerData.getName() + " &7(" + playerData.getTotalTokens() + " tokens)"));

            Player online = Bukkit.getPlayer(playerData.getUuid());
            if (online != null) {
                online.sendMessage(colorize("&6Season reward unlocked: permanent prestige cosmetics applied."));
                plugin.getDisplayManager().updateDisplay(online);
            }
        }

        plugin.getExpansionDataManager().setSeasonRewardsDistributed(true);
        plugin.getExpansionDataManager().save();
    }

    private Location resolveBossSpawnLocation(World world) {
        boolean randomSpawn = plugin.getConfig().getBoolean("boss.random-spawn.enabled", true);
        if (!randomSpawn) {
            return new Location(world,
                    plugin.getConfig().getDouble("boss.x", 0),
                    plugin.getConfig().getDouble("boss.y", 120),
                    plugin.getConfig().getDouble("boss.z", 0));
        }

        int maxDistanceFromNearest = Math.max(50,
                plugin.getConfig().getInt("boss.random-spawn.max-distance-from-nearest-player", 500));
        int minDistanceFromNearest = Math.max(0,
                plugin.getConfig().getInt("boss.random-spawn.min-distance-from-nearest-player", 80));
        if (minDistanceFromNearest >= maxDistanceFromNearest) {
            minDistanceFromNearest = Math.max(0, maxDistanceFromNearest - 10);
        }

        List<Player> worldPlayers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (player.isOnline() && !player.isDead()) {
                worldPlayers.add(player);
            }
        }

        if (!worldPlayers.isEmpty()) {
            double maxDistanceSquared = (double) maxDistanceFromNearest * maxDistanceFromNearest;
            for (int attempt = 0; attempt < 120; attempt++) {
                Player anchor = worldPlayers.get(random.nextInt(worldPlayers.size()));
                Location candidate = resolveSpawnNearPlayer(anchor, maxDistanceFromNearest, minDistanceFromNearest);
                if (candidate == null || candidate.getWorld() == null) {
                    continue;
                }

                Player nearest = findNearestOnlinePlayer(candidate.getWorld(), candidate);
                if (nearest == null) {
                    continue;
                }
                if (nearest.getLocation().distanceSquared(candidate) <= maxDistanceSquared) {
                    return candidate;
                }
            }
        }

        return new Location(world,
                plugin.getConfig().getDouble("boss.x", 0),
                plugin.getConfig().getDouble("boss.y", 120),
                plugin.getConfig().getDouble("boss.z", 0));
    }

    public void onBossDamaged(Player attacker, Entity boss, double damage) {
        if (activeBossUuid == null || !boss.getUniqueId().equals(activeBossUuid)) return;
        bossDamage.put(attacker.getUniqueId(), bossDamage.getOrDefault(attacker.getUniqueId(), 0.0) + damage);
    }

    public void onBossDeath(Entity entity, Player killer) {
        if (activeBossUuid == null || !entity.getUniqueId().equals(activeBossUuid)) return;
        activeBossUuid = null;

        plugin.getLogger().info("Boss defeated. Participants tracked: " + bossDamage.size() + ", killer=" + (killer != null ? killer.getName() : "none"));

        if (killer != null && !bossDamage.containsKey(killer.getUniqueId())) {
            bossDamage.put(killer.getUniqueId(), 1.0);
        }

        int minPlayers = plugin.getConfig().getInt("boss.min-players-for-reward", 2);
        if (bossDamage.size() < minPlayers) {
            if (killer != null) {
                int soloReward = plugin.getConfig().getInt("boss.solo-kill-token-reward", 10);
                int bundleItems = plugin.getConfig().getInt("boss.solo-token-bundle-items", 2);
                grantBossReward(killer, soloReward, bundleItems, true);
                plugin.getLogger().info("Boss solo reward granted to " + killer.getName() + " for " + soloReward + " tokens + items");
                Bukkit.broadcastMessage(colorize("&7Boss defeated by &f" + killer.getName() + "&7. Reduced solo reward granted."));
            } else {
                plugin.getLogger().warning("Boss died with no eligible killer and too few participants for rewards.");
                Bukkit.broadcastMessage(colorize("&7Boss was defeated, but too few players participated for rewards."));
            }
            bossDamage.clear();
            return;
        }

        int baseBundle = plugin.getConfig().getInt("boss.token-bundle", 14);
        int bundleItems = plugin.getConfig().getInt("boss.token-bundle-items", 4);
        for (UUID uuid : bossDamage.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            grantBossReward(player, baseBundle, bundleItems, false);
            plugin.getLogger().info("Boss group reward granted to " + player.getName() + " for " + baseBundle + " tokens + items");
        }

        int buffSeconds = plugin.getConfig().getInt("boss.server-buff-seconds", 300);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, buffSeconds * 20, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, buffSeconds * 20, 0, true, true, true));
        }
        Bukkit.broadcastMessage(colorize("&6The boss has fallen! Server-wide combat buff active for " + (buffSeconds / 60) + " minutes."));

        bossDamage.clear();
    }

    private void grantBossReward(Player player, int tokenAmount, int bundleItems, boolean solo) {
        int before = plugin.getTokenManager().getTokens(player);
        plugin.getTokenManager().giveTokens(player, tokenAmount);
        int after = plugin.getTokenManager().getTokens(player);
        int credited = Math.max(0, after - before);
        int overflow = Math.max(0, tokenAmount - credited);

        int totalItems = Math.max(0, bundleItems + overflow);
        if (totalItems > 0) {
            player.getInventory().addItem(ItemUtils.createTokenItem(totalItems));
        }

        int enchantedGapples = plugin.getConfig().getInt("boss.reward-enchanted-gapple", 1);
        int totems = plugin.getConfig().getInt("boss.reward-totems", 3);
        int goldenApples = plugin.getConfig().getInt("boss.reward-golden-apples", 0);

        String mode = solo ? "Solo " : "";
        player.sendMessage(colorize("&6" + mode + "Boss Reward &7» &aBanked: &e" + credited
                + " &8| &aToken Items: &e" + totalItems));

        if (enchantedGapples > 0) {
            player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, enchantedGapples));
        }
        if (totems > 0) {
            player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, totems));
        }
        if (goldenApples > 0) {
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, goldenApples));
        }
    }

    public void triggerRandomWorldEvent() {
        if (!plugin.getConfig().getBoolean("events.random-world.enabled", true)) return;

        int roll = random.nextInt(9);
        switch (roll) {
            case 0 -> spawnSupplyDrop();
            case 1 -> spawnMeteorCrash();
            case 2 -> spawnRogueAssassin();
            case 3 -> empowerMarkedTemporarily();
            case 4 -> spawnTitanZombie();
            case 5 -> spawnAbyssStalker();
            case 6 -> spawnPlagueBroodmother();
            case 7 -> spawnInfernalBrute();
            default -> spawnDreadHusk();
        }
    }

    public void spawnTitanZombieEvent() {
        spawnTitanZombie();
    }

    public boolean spawnEliteMobEvent(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        String normalized = key.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "abyss", "abyss-stalker", "stalker" -> {
                spawnAbyssStalker();
                yield true;
            }
            case "plague", "plague-broodmother", "broodmother" -> {
                spawnPlagueBroodmother();
                yield true;
            }
            case "infernal", "infernal-brute", "brute", "piglin" -> {
                spawnInfernalBrute();
                yield true;
            }
            case "dread", "dread-husk", "husk" -> {
                spawnDreadHusk();
                yield true;
            }
            default -> false;
        };
    }

    private void spawnTitanZombie() {
        if (!plugin.getConfig().getBoolean("events.random-world.titan.enabled", true)) return;
        if (titanZombieUuid != null) {
            Entity existing = Bukkit.getEntity(titanZombieUuid);
            if (existing != null && !existing.isDead()) return;
        }

        Player anchor = getRandomOnlinePlayer();
        if (anchor == null) return;

        Location loc = resolveSpawnNearPlayer(anchor,
                plugin.getConfig().getInt("events.random-world.spread", 1500));
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;

        Zombie titan = world.spawn(loc, Zombie.class, entity -> {
            entity.setAdult();
            entity.setShouldBurnInDay(false);
            entity.setCustomName(colorize("&4&lColossus Devourer"));
            entity.setCustomNameVisible(true);

            double hp = plugin.getConfig().getDouble("events.random-world.titan.health", 600.0);
            Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(hp);
            entity.setHealth(hp);

            double damage = plugin.getConfig().getDouble("events.random-world.titan.damage", 18.0);
            Objects.requireNonNull(entity.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(damage);
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.34);

            AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
            if (scaleAttribute != null) {
                scaleAttribute.setBaseValue(1.9);
            }

            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 3600, 1, true, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 3600, 1, true, false));

            equipMobHelmet(entity, Material.NETHERITE_HELMET);
        });

        titanZombieUuid = titan.getUniqueId();
        titanLastLocation = titan.getLocation().clone();
        titanLastShakeMillis = 0L;
        titanLastStompMillis = 0L;

        titan.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
        titan.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        titan.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        titan.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        titan.getEquipment().setItemInMainHandDropChance(0.0f);
        titan.getEquipment().setChestplateDropChance(0.0f);
        titan.getEquipment().setLeggingsDropChance(0.0f);
        titan.getEquipment().setBootsDropChance(0.0f);

        Bukkit.broadcastMessage(colorize("&4&lTITAN EVENT &7A giant undead colossus is rampaging at &c"
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
    }

    private void spawnAbyssStalker() {
        spawnEliteMob(EliteMobType.ABYSS_STALKER);
    }

    private void spawnPlagueBroodmother() {
        spawnEliteMob(EliteMobType.PLAGUE_BROODMOTHER);
    }

    private void spawnInfernalBrute() {
        spawnEliteMob(EliteMobType.INFERNAL_BRUTE);
    }

    private void spawnDreadHusk() {
        spawnEliteMob(EliteMobType.DREAD_HUSK);
    }

    private void spawnEliteMob(EliteMobType type) {
        Player anchor = getRandomOnlinePlayer();
        if (anchor == null) return;

        Location loc = resolveSpawnNearPlayer(anchor,
                plugin.getConfig().getInt("events.random-world.spread", 1500));
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;

        LivingEntity mob;
        switch (type) {
            case ABYSS_STALKER -> {
                mob = world.spawn(loc, WitherSkeleton.class);
                mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 1200, 1, true, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 1200, 0, true, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 1200, 1, true, false));
                equipMobHelmet(mob, Material.NETHERITE_HELMET);
                mob.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            }
            case PLAGUE_BROODMOTHER -> {
                mob = world.spawn(loc, ZombieVillager.class);
                mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 1200, 0, true, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 1200, 1, true, false));
                equipMobHelmet(mob, Material.TURTLE_HELMET);
                mob.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            }
            case INFERNAL_BRUTE -> {
                mob = world.spawn(loc, PiglinBrute.class);
                if (mob instanceof PiglinBrute piglinBrute) {
                    piglinBrute.setImmuneToZombification(true);
                }
                mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 1200, 1, true, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 1200, 0, true, false));
                equipMobHelmet(mob, Material.NETHERITE_HELMET);
                mob.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            }
            case DREAD_HUSK -> {
                mob = world.spawn(loc, Husk.class);
                mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SHOVEL));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 1200, 0, true, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 1200, 0, true, false));
                equipMobHelmet(mob, Material.NETHERITE_HELMET);
                mob.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            }
            default -> {
                return;
            }
        }

        mob.setCustomName(colorize(type.displayName));
        mob.setCustomNameVisible(true);

        double health = plugin.getConfig().getDouble("events.random-world.elites." + type.configKey + ".health", type.defaultHealth);
        double damage = plugin.getConfig().getDouble("events.random-world.elites." + type.configKey + ".damage", type.defaultDamage);

        Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
        mob.setHealth(health);
        Objects.requireNonNull(mob.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(damage);

        eliteMobs.put(mob.getUniqueId(), type);
        eliteAbilityCooldowns.put(mob.getUniqueId(), 0L);
        Bukkit.broadcastMessage(colorize("&5" + ChatColor.stripColor(type.displayName)
                + " &7has appeared at &d" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
    }

    private void tickCustomMobAbilities() {
        tickTitanZombieAbilities();
        tickEliteMobAbilities();
    }

    private void tickTitanZombieAbilities() {
        if (titanZombieUuid == null) return;

        Entity entity = Bukkit.getEntity(titanZombieUuid);
        if (!(entity instanceof Zombie titan) || titan.isDead()) {
            titanZombieUuid = null;
            titanLastLocation = null;
            titanLastShakeMillis = 0L;
            return;
        }

        Location location = titan.getLocation();
        breakBlocksInTitanPath(location, titan.getVelocity());

        if (titanLastLocation != null && titanLastLocation.getWorld() == location.getWorld()) {
            double moved = location.distanceSquared(titanLastLocation);
            long now = System.currentTimeMillis();
            long cooldownMs = plugin.getConfig().getLong("events.random-world.titan.shake-cooldown-millis", 800L);

            if (moved >= 0.20 && now - titanLastShakeMillis >= cooldownMs) {
                titanLastShakeMillis = now;
                shakePlayersNearTitan(location);
            }
        }

        long now = System.currentTimeMillis();
        if (now - titanLastStompMillis >= 3000L) {
            boolean hasNearbyPlayers = !location.getNearbyPlayers(8.0).isEmpty();
            if (hasNearbyPlayers) {
                titanLastStompMillis = now;
                titanStomp(location);
            }
        }

        titanLastLocation = location.clone();
    }

    private void titanStomp(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        world.playSound(location, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 0.6, 0), 2, 0.5, 0.2, 0.5, 0.01);

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) > 64.0) continue;

            Vector knockback = player.getLocation().toVector().subtract(location.toVector());
            if (knockback.lengthSquared() < 0.01) {
                knockback = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
            }
            knockback.normalize().multiply(0.45).setY(0.20);
            player.damage(2.0);
            player.setVelocity(player.getVelocity().add(knockback));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, true, true));
        }
    }

    private void breakBlocksInTitanPath(Location center, Vector velocity) {
        World world = center.getWorld();
        if (world == null) return;

        int radius = Math.max(1, plugin.getConfig().getInt("events.random-world.titan.block-break-radius", 1));
        Vector direction = velocity.clone();
        if (direction.lengthSquared() < 0.02) {
            direction = center.getDirection();
        }
        direction.setY(0);
        if (direction.lengthSquared() < 0.001) {
            direction = new Vector(1, 0, 0);
        }
        direction.normalize();

        Location front = center.clone().add(direction.multiply(radius));
        int baseX = front.getBlockX();
        int baseY = front.getBlockY();
        int baseZ = front.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 1; y <= 2; y++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    if (isTitanBreakable(block.getType())) {
                        block.setType(Material.AIR, false);
                        world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2,
                                block.getBlockData());
                    }
                }
            }
        }
    }

    private boolean isTitanBreakable(Material material) {
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) return false;
        if (material == Material.BEDROCK || material == Material.END_PORTAL_FRAME || material == Material.BARRIER) return false;
        if (material == Material.CRYING_OBSIDIAN || material == Material.OBSIDIAN || material == Material.REINFORCED_DEEPSLATE) return false;
        if (!material.isBlock()) return false;
        if (!material.isSolid() && !material.name().contains("LEAVES") && !material.name().contains("GLASS")) return false;
        return true;
    }

    private void shakePlayersNearTitan(Location titanLocation) {
        World world = titanLocation.getWorld();
        if (world == null) return;

        double shakeRadius = plugin.getConfig().getDouble("events.random-world.titan.shake-radius", 50.0);
        double radiusSquared = shakeRadius * shakeRadius;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(titanLocation) > radiusSquared) continue;

            Vector push = new Vector((random.nextDouble() - 0.5) * 0.08, 0.02, (random.nextDouble() - 0.5) * 0.08);
            player.setVelocity(player.getVelocity().add(push));
            player.playSound(player.getLocation(), Sound.BLOCK_BASALT_STEP, 0.35f, 0.6f);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.01);
        }
    }

    private void tickEliteMobAbilities() {
        if (eliteMobs.isEmpty()) return;

        Iterator<Map.Entry<UUID, EliteMobType>> iterator = eliteMobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EliteMobType> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity mob) || mob.isDead()) {
                iterator.remove();
                eliteAbilityCooldowns.remove(entry.getKey());
                continue;
            }

            long now = System.currentTimeMillis();
            long readyAt = eliteAbilityCooldowns.getOrDefault(entry.getKey(), 0L);
            if (now < readyAt) {
                continue;
            }

            switch (entry.getValue()) {
                case ABYSS_STALKER -> {
                    Player target = findNearestPlayer(mob, 18.0);
                    if (target != null) {
                        Location blink = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1.5));
                        blink.setY(target.getLocation().getY());
                        mob.teleport(blink);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1, true, true));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, true));
                        mob.getWorld().playSound(blink, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                        mob.getWorld().spawnParticle(Particle.PORTAL, blink.add(0, 1, 0), 60, 0.3, 0.4, 0.3, 0.01);
                        eliteAbilityCooldowns.put(entry.getKey(), now + 4500L);
                    }
                }
                case PLAGUE_BROODMOTHER -> {
                    mob.getWorld().spawnParticle(Particle.ENTITY_EFFECT, mob.getLocation().add(0, 1, 0), 35, 1.8, 0.6, 1.8, 0.01);
                    for (Entity nearby : mob.getNearbyEntities(6, 3, 6)) {
                        if (nearby instanceof Player player) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1, true, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 1, true, true));
                        }
                    }
                    int summons = 1 + random.nextInt(2);
                    for (int i = 0; i < summons; i++) {
                        mob.getWorld().spawn(mob.getLocation(), CaveSpider.class, spider -> {
                            spider.setCustomName(colorize("&2Plague Spawn"));
                            spider.setCustomNameVisible(false);
                        });
                    }
                    eliteAbilityCooldowns.put(entry.getKey(), now + 5000L);
                }
                case INFERNAL_BRUTE -> {
                    mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
                    mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 40, 1.8, 0.5, 1.8, 0.04);
                    for (Entity nearby : mob.getNearbyEntities(5, 2, 5)) {
                        if (nearby instanceof Player player) {
                            player.setFireTicks(Math.max(player.getFireTicks(), 80));
                            player.damage(3.0);
                        }
                    }
                    mob.getWorld().createExplosion(mob.getLocation(), 1.6f, false, false);
                    eliteAbilityCooldowns.put(entry.getKey(), now + 5500L);
                }
                case DREAD_HUSK -> {
                    mob.getWorld().spawnParticle(Particle.ASH, mob.getLocation().add(0, 1, 0), 45, 1.5, 0.5, 1.5, 0.01);
                    for (Entity nearby : mob.getNearbyEntities(6, 2, 6)) {
                        if (nearby instanceof Player player) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, true, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, true, true));
                        }
                    }
                    mob.getWorld().spawn(mob.getLocation(), Husk.class, summon -> {
                        summon.setCustomName(colorize("&6Dread Minion"));
                        summon.setCustomNameVisible(false);
                        equipMobHelmet(summon, Material.CHAINMAIL_HELMET);
                    });
                    eliteAbilityCooldowns.put(entry.getKey(), now + 6000L);
                }
                default -> {
                }
            }
        }
    }

    private void spawnScheduledSupplyDrop() {
        if (!plugin.getConfig().getBoolean("supply-drop.enabled", true)) {
            nextSupplyDropAtMillis = 0L;
            return;
        }
        spawnSupplyDrop(true);
        long intervalMinutes = Math.max(1L, plugin.getConfig().getLong("supply-drop.interval-minutes", 90L));
        nextSupplyDropAtMillis = System.currentTimeMillis() + (intervalMinutes * 60_000L);
    }

    private void spawnSupplyDrop() {
        spawnSupplyDrop(false);
    }

    public void forceSupplyDrop() {
        spawnSupplyDrop(false);
    }

    private void spawnSupplyDrop(boolean scheduled) {
        World world = Bukkit.getWorld(plugin.getConfig().getString("high-risk-zone.world", "world"));
        if (world == null) {
            return;
        }

        int x1 = plugin.getConfig().getInt("high-risk-zone.x1", -2500);
        int z1 = plugin.getConfig().getInt("high-risk-zone.z1", -2500);
        int x2 = plugin.getConfig().getInt("high-risk-zone.x2", 2500);
        int z2 = plugin.getConfig().getInt("high-risk-zone.z2", 2500);

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int x = random.nextInt(Math.max(1, maxX - minX + 1)) + minX;
        int z = random.nextInt(Math.max(1, maxZ - minZ + 1)) + minZ;
        int y = world.getHighestBlockYAt(x, z);

        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            int tokenItems = 5 + random.nextInt(8);
            chest.getBlockInventory().addItem(ItemUtils.createTokenItem(tokenItems));
            chest.getBlockInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 6 + random.nextInt(5)));
            chest.getBlockInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1 + random.nextInt(2)));
            chest.getBlockInventory().addItem(createPotion(PotionType.STRENGTH));
            chest.getBlockInventory().addItem(createPotion(PotionType.REGENERATION));

            double scrollChance = plugin.getConfig().getDouble("supply-drop.stat-scroll-chance", 0.35);
            if (random.nextDouble() <= scrollChance) {
                chest.getBlockInventory().addItem(ItemUtils.createStatBoostScroll(1));
            }
        }

        String marker = scheduled ? "&6&lSCHEDULED SUPPLY DROP" : "&eSupply Drop";
        Bukkit.broadcastMessage(colorize(marker + " &7landed in Blood Zone at &f" + x + ", " + y + ", " + z));

        int despawnMinutes = Math.max(1, plugin.getConfig().getInt("supply-drop.despawn-minutes", 20));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block existing = world.getBlockAt(x, y, z);
            if (existing.getType() == Material.CHEST) {
                existing.setType(Material.AIR);
                world.spawnParticle(Particle.CLOUD, existing.getLocation().add(0.5, 0.5, 0.5), 12, 0.25, 0.25, 0.25, 0.01);
            }
        }, despawnMinutes * 60L * 20L);
    }

    private ItemStack createPotion(PotionType potionType) {
        ItemStack potion = new ItemStack(Material.POTION, 1);
        if (potion.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(potionType);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private void spawnMeteorCrash() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events.random-world.world", "world"));
        if (world == null) return;
        int spread = plugin.getConfig().getInt("events.random-world.spread", 1500);
        int x = random.nextInt(spread * 2) - spread;
        int z = random.nextInt(spread * 2) - spread;
        int y = world.getHighestBlockYAt(x, z);
        Location loc = new Location(world, x, y, z);

        world.createExplosion(loc, 2.0f, false, false);
        world.strikeLightningEffect(loc);

        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            chest.getBlockInventory().addItem(ItemUtils.createTokenItem(4));
            chest.getBlockInventory().addItem(new ItemStack(Material.NETHERITE_SCRAP, 2));
        }

        Bukkit.broadcastMessage(colorize("&cMeteor crash detected at &f" + x + ", " + y + ", " + z));
    }

    private void spawnRogueAssassin() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        Player target = online.get(random.nextInt(online.size()));
        Location loc = target.getLocation().add(5, 0, 5);
        Vindicator vindicator = target.getWorld().spawn(loc, Vindicator.class);
        vindicator.setCustomName(colorize("&8&lRogue Assassin"));
        vindicator.setCustomNameVisible(true);
        vindicator.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 1, true, true));
        vindicator.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 180, 0, true, true));

        rogueAssassins.add(vindicator.getUniqueId());

        Bukkit.broadcastMessage(colorize("&8A Rogue Assassin has entered the world near &f" + target.getName() + "&8!"));
    }

    private void empowerMarkedTemporarily() {
        int seconds = plugin.getConfig().getInt("events.random-world.marked-invincible-seconds", 20);
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getMarkedManager().isMarked(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 4, true, true));
                count++;
            }
        }

        if (count > 0) {
            Bukkit.broadcastMessage(colorize("&5Chaos Surge: Marked players are temporarily invincible for " + seconds + "s."));
        }
    }

    public boolean isRogueAssassin(Entity entity) {
        return rogueAssassins.contains(entity.getUniqueId());
    }

    public void onRogueAssassinDeath(Entity entity, Player killer) {
        if (!isRogueAssassin(entity)) return;
        rogueAssassins.remove(entity.getUniqueId());

        int reward = plugin.getConfig().getInt("events.random-world.rogue-assassin-token-reward", 10);
        if (killer != null) {
            plugin.getTokenManager().giveTokens(killer, reward);
            killer.sendMessage(colorize("&aYou eliminated the Rogue Assassin and earned " + reward + " tokens."));
        }
    }

    public void onCustomMobDeath(Entity entity, Player killer) {
        if (entity == null) return;

        if (titanZombieUuid != null && entity.getUniqueId().equals(titanZombieUuid)) {
            titanZombieUuid = null;
            titanLastLocation = null;
            titanLastShakeMillis = 0L;
            titanLastStompMillis = 0L;

            int reward = plugin.getConfig().getInt("events.random-world.titan.reward-tokens", 35);
            if (killer != null) {
                plugin.getTokenManager().giveTokens(killer, reward);
                killer.getInventory().addItem(ItemUtils.createTokenItem(Math.max(8, reward / 2)));
                killer.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, 2));
                killer.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                killer.sendMessage(colorize("&6You slayed the Colossus Devourer and earned " + reward + " tokens."));
                Bukkit.broadcastMessage(colorize("&6" + killer.getName() + " defeated the Titan!"));
            }
            return;
        }

        EliteMobType type = eliteMobs.remove(entity.getUniqueId());
        if (type == null) return;

        int reward = plugin.getConfig().getInt("events.random-world.elites." + type.configKey + ".reward-tokens", type.defaultReward);
        if (killer != null) {
            plugin.getTokenManager().giveTokens(killer, reward);
            killer.getInventory().addItem(ItemUtils.createTokenItem(Math.max(3, reward / 2)));
            switch (type) {
                case ABYSS_STALKER -> killer.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 8));
                case PLAGUE_BROODMOTHER -> killer.getInventory().addItem(new ItemStack(Material.SPIDER_EYE, 16));
                case INFERNAL_BRUTE -> killer.getInventory().addItem(new ItemStack(Material.BLAZE_ROD, 6));
                case DREAD_HUSK -> killer.getInventory().addItem(new ItemStack(Material.SAND, 32));
            }
            killer.sendMessage(colorize("&dYou killed " + ChatColor.stripColor(type.displayName) + " and earned " + reward + " tokens."));
        }
    }

    private void despawnCustomMobs() {
        if (titanZombieUuid != null) {
            Entity titan = Bukkit.getEntity(titanZombieUuid);
            if (titan != null && !titan.isDead()) {
                titan.remove();
            }
        }

        titanZombieUuid = null;
        titanLastLocation = null;
        titanLastShakeMillis = 0L;
        titanLastStompMillis = 0L;

        for (UUID uuid : new ArrayList<>(eliteMobs.keySet())) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        eliteMobs.clear();
        eliteAbilityCooldowns.clear();
    }

    private Player getRandomOnlinePlayer() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return null;
        return online.get(random.nextInt(online.size()));
    }

    private Player findNearestPlayer(LivingEntity source, double radius) {
        Player nearest = null;
        double bestDistance = radius * radius;
        for (Player player : source.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(source.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void equipMobHelmet(LivingEntity mob, Material material) {
        if (mob == null || material == null || mob.getEquipment() == null) return;

        ItemStack helmet = new ItemStack(material);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            helmet.setItemMeta(meta);
        }

        mob.getEquipment().setHelmet(helmet);
        mob.getEquipment().setHelmetDropChance(0.0f);
    }

    private Location resolveSpawnNearPlayer(Player anchor, int spread) {
        World world = anchor.getWorld();
        int safeSpread = Math.max(50, spread);

        for (int attempt = 0; attempt < 60; attempt++) {
            int x = anchor.getLocation().getBlockX() + random.nextInt(safeSpread * 2 + 1) - safeSpread;
            int z = anchor.getLocation().getBlockZ() + random.nextInt(safeSpread * 2 + 1) - safeSpread;
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            if (isValidDrySpawn(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private Location resolveSpawnNearPlayer(Player anchor, int maxDistance, int minDistance) {
        World world = anchor.getWorld();
        int safeMax = Math.max(50, maxDistance);
        int safeMin = Math.max(0, Math.min(minDistance, safeMax - 1));
        Location anchorLocation = anchor.getLocation();

        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0);
            double radius = safeMin + (random.nextDouble() * (safeMax - safeMin));
            int x = anchorLocation.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = anchorLocation.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            if (isValidDrySpawn(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private Player findNearestOnlinePlayer(World world, Location location) {
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(location);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private boolean isValidDrySpawn(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);

        if (feet.getType().isSolid() || head.getType().isSolid()) return false;
        if (!below.getType().isSolid()) return false;
        if (below.isLiquid() || feet.isLiquid() || head.isLiquid()) return false;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block nearbyFeet = world.getBlockAt(x + dx, y, z + dz);
                Block nearbyBelow = world.getBlockAt(x + dx, y - 1, z + dz);
                if (nearbyFeet.isLiquid() || nearbyBelow.isLiquid()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void onPlayerKill(Player killer, Player victim) {
        if (killer == null || victim == null) {
            return;
        }
        onPlayerKill(killer, victim.getName(), plugin.getDataManager().getPlayerData(victim), victim.getLocation());
        awardAssistRewards(killer, victim.getUniqueId());
        awardClutchBonus(killer);
        assistContributions.remove(victim.getUniqueId());
    }

    public void onPlayerKill(Player killer, String victimName, PlayerData victimData) {
        onPlayerKill(killer, victimName, victimData, killer != null ? killer.getLocation() : null);
    }

    private void onPlayerKill(Player killer, String victimName, PlayerData victimData, Location victimLocation) {
        if (killer == null || victimName == null || victimName.isBlank() || victimData == null) {
            return;
        }

        updateReputation(killer, victimData);
        updateContracts(killer, victimName);
        updateDailyQuest(killer);
        applyExtraKillBonuses(killer, victimData, victimLocation);
        updateKillBonusTracking(killer, victimData.getUuid());
        killComboStates.remove(victimData.getUuid());
        applyWeaponIdentity(killer);
    }

    private void updateDailyQuest(Player killer) {
        plugin.getExpansionDataManager().resetDailyIfNeeded(killer);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(killer);
        profile.addDailyQuestProgress(1);
    }

    private void updateContracts(Player killer, String victimName) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(killer);
        if (!profile.getContractTarget().isEmpty() && victimName.equalsIgnoreCase(profile.getContractTarget())) {
            profile.setContractProgress(profile.getContractProgress() + 1);
            if (profile.getContractProgress() >= profile.getContractGoal()) {
                int reward = plugin.getConfig().getInt("spawn.contract-reward", 12);
                plugin.getTokenManager().giveTokens(killer, reward);
                killer.sendMessage(colorize("&aContract complete! +" + reward + " tokens."));
                profile.addContractCompletion();
                profile.setContractTarget("");
                profile.setContractProgress(0);
                profile.setContractGoal(2);
            }
        }
    }

    private void updateReputation(Player killer, PlayerData victimData) {
        ExpansionDataManager.ExpansionProfile killerProfile = plugin.getExpansionDataManager().getProfile(killer);

        int lowTokenThreshold = plugin.getConfig().getInt("reputation.low-token-threshold", 5);
        int highTokenThreshold = plugin.getConfig().getInt("reputation.high-token-threshold", 25);

        if (victimData.getTotalTokens() <= lowTokenThreshold) {
            killerProfile.addReputation(-2);
            killerProfile.addInfamy(2);
            killer.sendMessage(colorize("&cYou hunted weak prey. Reputation decreased."));
        }

        if (victimData.getTotalTokens() >= highTokenThreshold || victimData.isMarked()) {
            killerProfile.addReputation(2);
            killerProfile.addFame(3);
            killer.sendMessage(colorize("&aFame rises! You defeated a dangerous target."));
        }
    }

    private void applyExtraKillBonuses(Player killer, PlayerData victimData, Location victimLocation) {
        if (!plugin.getConfig().getBoolean("bonus-kills.enabled", true)) {
            return;
        }

        int totalBonus = 0;
        long now = System.currentTimeMillis();
        UUID killerUuid = killer.getUniqueId();
        PlayerData killerData = plugin.getDataManager().getPlayerData(killer);

        if (plugin.getConfig().getBoolean("bonus-kills.revenge.enabled", true)) {
            KillRecord record = lastKilledBy.get(killerUuid);
            long windowMs = Math.max(30_000L, plugin.getConfig().getLong("bonus-kills.revenge.window-seconds", 600L) * 1000L);
            if (record != null
                    && record.killerUuid().equals(victimData.getUuid())
                    && now - record.timestampMillis() <= windowMs) {
                int reward = Math.max(1, plugin.getConfig().getInt("bonus-kills.revenge.tokens", 3));
                plugin.getTokenManager().giveTokens(killer, reward);
                totalBonus += reward;
                killer.sendMessage(colorize("&6&lREVENGE &7» &aYou settled the score: &e+" + reward + " tokens"));
                if (plugin.getConfig().getBoolean("bonus-kills.revenge.broadcast", true)) {
                    Bukkit.broadcastMessage(colorize("&6" + killer.getName() + " got revenge on " + victimData.getName() + "!"));
                }
                lastKilledBy.remove(killerUuid);
            }
        }

        if (plugin.getConfig().getBoolean("bonus-kills.underdog.enabled", true)) {
            int minGap = Math.max(1, plugin.getConfig().getInt("bonus-kills.underdog.min-token-gap", 10));
            int tokenGap = victimData.getTotalTokens() - killerData.getTotalTokens();
            if (tokenGap >= minGap) {
                int reward = Math.max(1, plugin.getConfig().getInt("bonus-kills.underdog.tokens", 2));
                plugin.getTokenManager().giveTokens(killer, reward);
                totalBonus += reward;
                killer.sendMessage(colorize("&b&lUNDERDOG &7» &aTarget had +" + tokenGap + " more tokens: &e+" + reward + " tokens"));
            }
        }

        if (plugin.getConfig().getBoolean("bonus-kills.last-stand.enabled", true)) {
            double maxHearts = Math.max(1.0, plugin.getConfig().getDouble("bonus-kills.last-stand.max-hearts", 4.0));
            if (killer.getHealth() <= (maxHearts * 2.0)) {
                int reward = Math.max(1, plugin.getConfig().getInt("bonus-kills.last-stand.tokens", 2));
                plugin.getTokenManager().giveTokens(killer, reward);
                totalBonus += reward;
                killer.sendMessage(colorize("&c&lLAST STAND &7» &aWon while low HP: &e+" + reward + " tokens"));
            }
        }

        if (plugin.getConfig().getBoolean("bonus-kills.blood-zone.enabled", true)) {
            Location zoneCheck = victimLocation != null ? victimLocation : killer.getLocation();
            if (zoneCheck != null && isInBloodZone(zoneCheck)) {
                int reward = Math.max(1, plugin.getConfig().getInt("bonus-kills.blood-zone.tokens", 1));
                plugin.getTokenManager().giveTokens(killer, reward);
                totalBonus += reward;
                killer.sendMessage(colorize("&4&lBLOOD ZONE &7» &aHigh-risk kill bonus: &e+" + reward + " tokens"));
            }
        }

        if (plugin.getConfig().getBoolean("bonus-kills.combo.enabled", true)) {
            long comboWindowMs = Math.max(10_000L, plugin.getConfig().getLong("bonus-kills.combo.window-seconds", 45L) * 1000L);
            KillComboState previous = killComboStates.get(killerUuid);
            int combo = (previous != null && now - previous.lastKillMillis() <= comboWindowMs) ? previous.combo() + 1 : 1;
            killComboStates.put(killerUuid, new KillComboState(combo, now));

            if (combo >= 2) {
                int maxStacks = Math.max(1, plugin.getConfig().getInt("bonus-kills.combo.max-stacks", 5));
                int stacks = Math.min(maxStacks, combo - 1);
                int perStack = Math.max(1, plugin.getConfig().getInt("bonus-kills.combo.tokens-per-stack", 1));
                int reward = stacks * perStack;

                plugin.getTokenManager().giveTokens(killer, reward);
                totalBonus += reward;
                killer.sendMessage(colorize("&d&lCOMBO x" + combo + " &7» &e+" + reward + " tokens"));

                int announceAt = Math.max(3, plugin.getConfig().getInt("bonus-kills.combo.announce-at", 4));
                if (combo >= announceAt) {
                    Bukkit.broadcastMessage(colorize("&d" + killer.getName() + " is on a combo rampage (&fx" + combo + "&d)!"));
                }

                int frenzyThreshold = Math.max(3, plugin.getConfig().getInt("bonus-kills.combo.frenzy-threshold", 4));
                if (combo >= frenzyThreshold) {
                    int frenzySeconds = Math.max(4, plugin.getConfig().getInt("bonus-kills.combo.frenzy-seconds", 12));
                    int speedAmp = Math.max(0, plugin.getConfig().getInt("bonus-kills.combo.frenzy-speed-level", 1));
                    int resistanceAmp = Math.max(0, plugin.getConfig().getInt("bonus-kills.combo.frenzy-resistance-level", 0));
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, frenzySeconds * 20, speedAmp, true, true, true));
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, frenzySeconds * 20, resistanceAmp, true, true, true));
                    killer.sendMessage(colorize("&5&lFRENZY &7» &aCombo boost applied for " + frenzySeconds + "s"));
                }
            }
        }

        if (totalBonus > 0) {
            plugin.getSidebarManager().updatePlayer(killer);
            plugin.getDisplayManager().updateDisplay(killer);
        }
    }

    private void updateKillBonusTracking(Player killer, UUID victimUuid) {
        if (killer == null || victimUuid == null) {
            return;
        }
        lastKilledBy.put(victimUuid, new KillRecord(killer.getUniqueId(), System.currentTimeMillis()));
    }

    public void applyWeaponIdentity(Player killer) {
        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Material type = item.getType();
        if (!(type.name().endsWith("_SWORD") || type.name().endsWith("_AXE"))) {
            return;
        }

        UUID uuid = killer.getUniqueId();
        int kills = weaponKillCounter.getOrDefault(uuid, 0) + 1;
        weaponKillCounter.put(uuid, kills);

        ItemMetaBuilder.applyIdentity(item, killer, kills, plugin.getDataManager().getPlayerData(killer).getRebirthLevel());
    }

    public void recordDamage(Player attacker, Player victim, double damage, String causeName) {
        long now = System.currentTimeMillis();

        outgoingDamageLogs.computeIfAbsent(attacker.getUniqueId(), ignored -> new ArrayDeque<>())
                .addLast(new DamageLog(now, victim.getName(), damage, causeName));
        incomingDamageLogs.computeIfAbsent(victim.getUniqueId(), ignored -> new ArrayDeque<>())
                .addLast(new DamageLog(now, attacker.getName(), damage, causeName));

        Map<UUID, AssistContribution> victimContrib =
                assistContributions.computeIfAbsent(victim.getUniqueId(), ignored -> new HashMap<>());
        AssistContribution contribution = victimContrib.get(attacker.getUniqueId());
        double cumulativeDamage = (contribution == null ? 0.0 : contribution.damage()) + Math.max(0.0, damage);
        victimContrib.put(attacker.getUniqueId(), new AssistContribution(cumulativeDamage, now));

        trimLogs(outgoingDamageLogs.get(attacker.getUniqueId()), now);
        trimLogs(incomingDamageLogs.get(victim.getUniqueId()), now);
        trimAssistContributions(victim.getUniqueId(), now);
        trimAssistContributions(attacker.getUniqueId(), now);
    }

    private void trimLogs(Deque<DamageLog> logs, long now) {
        long keepMs = 30_000L;
        while (!logs.isEmpty() && now - logs.getFirst().timestamp > keepMs) {
            logs.removeFirst();
        }
    }

    private void trimAssistContributions(UUID victimUuid, long now) {
        Map<UUID, AssistContribution> entries = assistContributions.get(victimUuid);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        long keepMs = Math.max(5000L, plugin.getConfig().getLong("assists.window-seconds", 20L) * 1000L);
        entries.entrySet().removeIf(entry -> now - entry.getValue().lastHitMillis() > keepMs);
        if (entries.isEmpty()) {
            assistContributions.remove(victimUuid);
        }
    }

    private void awardAssistRewards(Player killer, UUID victimUuid) {
        if (!plugin.getConfig().getBoolean("assists.enabled", true)) {
            return;
        }

        Map<UUID, AssistContribution> entries = assistContributions.get(victimUuid);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        trimAssistContributions(victimUuid, now);
        entries = assistContributions.get(victimUuid);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        double totalDamage = 0.0;
        for (AssistContribution contribution : entries.values()) {
            totalDamage += Math.max(0.0, contribution.damage());
        }
        if (totalDamage <= 0.0) {
            return;
        }

        double minShare = Math.min(1.0, Math.max(0.0, plugin.getConfig().getDouble("assists.min-damage-share", 0.30)));
        double rewardMultiplier = Math.max(0.0, plugin.getConfig().getDouble("assists.reward-multiplier", 0.5));
        int baseKillReward = Math.max(1, plugin.getConfig().getInt("tokens.per-kill", 1));
        int assistReward = Math.max(1, (int) Math.round(baseKillReward * rewardMultiplier));

        for (Map.Entry<UUID, AssistContribution> entry : entries.entrySet()) {
            UUID assistantUuid = entry.getKey();
            if (assistantUuid.equals(killer.getUniqueId())) {
                continue;
            }

            double share = entry.getValue().damage() / totalDamage;
            if (share < minShare) {
                continue;
            }

            Player assistant = Bukkit.getPlayer(assistantUuid);
            if (assistant == null || !assistant.isOnline()) {
                continue;
            }

            plugin.getTokenManager().giveTokens(assistant, assistReward);
            plugin.getDataManager().getPlayerData(assistant).addAssist();
            assistant.sendMessage(colorize("&bAssist reward: &e+" + assistReward + " tokens &7("
                    + String.format("%.0f", share * 100) + "% damage dealt)"));
            plugin.getSidebarManager().updatePlayer(assistant);
            plugin.getDisplayManager().updateDisplay(assistant);
        }
    }

    private void awardClutchBonus(Player killer) {
        if (!plugin.getConfig().getBoolean("clutch.enabled", true)) {
            return;
        }

        UUID killerUuid = killer.getUniqueId();
        long now = System.currentTimeMillis();
        trimAssistContributions(killerUuid, now);

        Map<UUID, AssistContribution> incoming = assistContributions.get(killerUuid);
        if (incoming == null || incoming.isEmpty()) {
            return;
        }

        double minDamagePerAttacker = Math.max(0.0, plugin.getConfig().getDouble("clutch.min-damage-per-attacker", 4.0));
        int attackers = 0;
        for (Map.Entry<UUID, AssistContribution> entry : incoming.entrySet()) {
            if (entry.getKey().equals(killerUuid)) continue;
            if (entry.getValue().damage() >= minDamagePerAttacker) {
                attackers++;
            }
        }

        if (attackers < 2) {
            return;
        }

        int bonus = attackers >= 3
                ? plugin.getConfig().getInt("clutch.tokens-vs-3", 4)
                : plugin.getConfig().getInt("clutch.tokens-vs-2", 2);
        bonus = Math.max(1, bonus);

        plugin.getTokenManager().giveTokens(killer, bonus);
        killer.sendMessage(colorize("&d&lCLUTCH &7» &aOutnumbered win detected (&f1v" + attackers + "&a): &e+" + bonus + " tokens"));

        if (attackers >= 3) {
            Bukkit.broadcastMessage(colorize("&d" + killer.getName() + " just clutched a &f1v" + attackers + "&d fight!"));
        }
    }

    public double getRecentDps(Player player) {
        Deque<DamageLog> logs = outgoingDamageLogs.get(player.getUniqueId());
        if (logs == null || logs.isEmpty()) return 0.0;

        long now = System.currentTimeMillis();
        long window = 10_000L;
        double total = 0.0;
        for (DamageLog log : logs) {
            if (now - log.timestamp <= window) {
                total += log.damage;
            }
        }
        return total / 10.0;
    }

    public List<DamageLog> getIncomingDamage(Player player, int limit) {
        Deque<DamageLog> logs = incomingDamageLogs.get(player.getUniqueId());
        if (logs == null || logs.isEmpty()) return Collections.emptyList();

        List<DamageLog> list = new ArrayList<>(logs);
        list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public void clearAssistTracking(UUID victimUuid) {
        if (victimUuid != null) {
            assistContributions.remove(victimUuid);
        }
    }

    public void saveReplaySnapshot(Player victim, Player killer) {
        File file = new File(plugin.getDataFolder(), "combat-replays.yml");
        YamlConfiguration replayConfig = YamlConfiguration.loadConfiguration(file);

        String id = String.valueOf(System.currentTimeMillis());
        replayConfig.set("replays." + id + ".victim", victim.getName());
        replayConfig.set("replays." + id + ".killer", killer != null ? killer.getName() : "Unknown");
        replayConfig.set("replays." + id + ".world", victim.getWorld().getName());
        replayConfig.set("replays." + id + ".x", victim.getLocation().getBlockX());
        replayConfig.set("replays." + id + ".z", victim.getLocation().getBlockZ());

        List<String> lines = new ArrayList<>();
        for (DamageLog log : getIncomingDamage(victim, 8)) {
            lines.add(log.source() + "|" + String.format("%.2f", log.damage()) + "|" + log.cause() + "|" + log.timestamp());
        }
        replayConfig.set("replays." + id + ".damage", lines);

        try {
            replayConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save combat replay snapshot: " + e.getMessage());
        }
    }

    public void tickInfamyVisibility() {
        int threshold = plugin.getConfig().getInt("reputation.infamy-map-threshold", 12);
        for (Player player : Bukkit.getOnlinePlayers()) {
            ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
            if (profile.getInfamy() >= threshold) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, false, false, false));
                Bukkit.broadcastMessage(colorize("&8Infamous target spotted: &c" + player.getName() +
                        " &7at &f" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockZ()));
            }
        }
    }

    private void tickCosmetics() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

            if (data.getRebirthLevel() >= plugin.getConfig().getInt("cosmetics.rebirth-trail-level", 3)) {
                player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
            }
            if (profile.getAscension() >= 1) {
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 4, 0.3, 0.4, 0.3, 0.2);
            }
        }
    }

    public String getPlayerTitle(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        int seasonTier = getSeasonCosmeticTier(profile);
        if (seasonTier >= 3) return "&6&lSEASON CHAMPION";
        if (seasonTier >= 2) return "&d&lSEASON ELITE";
        if (seasonTier >= 1) return "&bSeason Top 10";
        if (profile.getAscension() >= 3) return "&5&lELITE";
        if (profile.getAscension() >= 1) return "&dAscended";
        if (profile.getFame() >= 20) return "&6Legend";
        if (profile.getInfamy() >= 12) return "&4Infamous";
        if (profile.getReputation() >= 10) return "&aHonored";
        return "&7Wanderer";
    }

    public String getTabNameColor(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        int seasonTier = getSeasonCosmeticTier(profile);
        if (seasonTier >= 3) return "&6";
        if (seasonTier >= 2) return "&d";
        if (seasonTier >= 1) return "&b";
        return "&f";
    }

    public String buildJoinMessage(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        int seasonTier = getSeasonCosmeticTier(profile);
        if (seasonTier > 0) {
            return colorize("&6✦ &8[&cBloodpine&8] &f" + player.getName() + " &7entered the season. &6✦");
        }
        return colorize("&8[&cBloodpine&8] &7" + player.getName() + " entered the season.");
    }

    private int getSeasonCosmeticTier(ExpansionDataManager.ExpansionProfile profile) {
        if (profile.getSeasonChampionFinishes() > 0) return 3;
        if (profile.getSeasonTop3Finishes() > 0) return 2;
        if (profile.getSeasonTop10Finishes() > 0) return 1;
        return 0;
    }

    public int getNearbyAllies(Player player, int radius) {
        int allies = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player other)) continue;
            if (other.equals(player)) continue;
            if (isSameTeam(player, other)) {
                allies++;
            }
        }
        return allies;
    }

    private boolean isSameTeam(Player a, Player b) {
        if (a.getScoreboard() == null || b.getScoreboard() == null) return false;
        org.bukkit.scoreboard.Team ta = a.getScoreboard().getEntryTeam(a.getName());
        org.bukkit.scoreboard.Team tb = b.getScoreboard().getEntryTeam(b.getName());
        return ta != null && tb != null && ta.getName().equalsIgnoreCase(tb.getName());
    }

    private int getTeamSize(Player player) {
        if (player.getScoreboard() == null) return 1;
        org.bukkit.scoreboard.Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) return 1;
        return team.getEntries().size();
    }

    public String getEventStatusLine() {
        return colorize("&7BloodMoon: " + (isBloodMoonActive() ? "&aON" : "&cOFF")
                + " &8| &7Arena: " + (arenaEventActive ? "&aON" : "&cOFF")
                + " &8| &7HuntMarked: " + (isHuntMarkedActive() ? "&aON" : "&cOFF")
                + " &8| &7FinalHour: " + (isFinalHourActive() ? "&aON" : "&cOFF")
                + " &8| &7Supply: " + getSupplyDropEtaSuffix());
    }

    private String getSupplyDropEtaSuffix() {
        if (!plugin.getConfig().getBoolean("supply-drop.enabled", true)) {
            return "&8disabled";
        }
        if (nextSupplyDropAtMillis <= 0L) {
            return "&7pending";
        }

        long remainingMs = Math.max(0L, nextSupplyDropAtMillis - System.currentTimeMillis());
        long totalSeconds = remainingMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return "&e" + minutes + "m " + seconds + "s";
    }

    public void startParkour(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        profile.setParkourStartMillis(System.currentTimeMillis());
        player.sendMessage(colorize("&bParkour timer started. Use /parkour finish at the end pad."));
    }

    public void finishParkour(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        if (profile.getParkourStartMillis() <= 0) {
            player.sendMessage(colorize("&cUse /parkour start first."));
            return;
        }

        long elapsed = System.currentTimeMillis() - profile.getParkourStartMillis();
        profile.setParkourStartMillis(0);

        long rewardCutoff = plugin.getConfig().getLong("spawn.parkour-fast-time-millis", 120000L);
        int reward = elapsed <= rewardCutoff
                ? plugin.getConfig().getInt("spawn.parkour-fast-reward", 6)
                : plugin.getConfig().getInt("spawn.parkour-reward", 3);
        plugin.getTokenManager().giveTokens(player, reward);

        if (profile.getParkourBestMillis() == 0 || elapsed < profile.getParkourBestMillis()) {
            profile.setParkourBestMillis(elapsed);
            player.sendMessage(colorize("&aNew parkour record: &f" + formatMillis(elapsed)));
        }

        player.sendMessage(colorize("&bParkour complete in &f" + formatMillis(elapsed) + " &b(+" + reward + " tokens)"));
    }

    public void tryAscend(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        int minRebirth = plugin.getConfig().getInt("endgame.ascension-min-rebirth", 5);
        int tokenCost = plugin.getConfig().getInt("endgame.ascension-token-cost", 40);
        int maxAscension = plugin.getConfig().getInt("endgame.max-ascension", 5);

        if (data.getRebirthLevel() < minRebirth) {
            player.sendMessage(colorize("&cYou need rebirth level " + minRebirth + " to ascend."));
            return;
        }
        if (profile.getAscension() >= maxAscension) {
            player.sendMessage(colorize("&eYou are already at max ascension."));
            return;
        }
        if (data.getAvailableTokens() < tokenCost) {
            player.sendMessage(colorize("&cYou need " + tokenCost + " available tokens to ascend."));
            return;
        }

        plugin.getTokenManager().removeTokens(player, tokenCost);
        profile.addAscension(1);
        player.sendMessage(colorize("&5&lASCENSION! &dYou reached Ascension " + profile.getAscension()));
        Bukkit.broadcastMessage(colorize("&d" + player.getName() + " has ascended to tier " + profile.getAscension() + "!"));
    }

    public void applySeasonStory(Player player) {
        String seasonName = plugin.getConfig().getString("seasonal-story.current-name", "Blood Plague");
        String seasonTag = plugin.getConfig().getString("seasonal-story.current-tag", "S")
                + plugin.getExpansionDataManager().getSeasonNumber();
        player.sendMessage(colorize("&8[" + seasonTag + "] &7Current arc: &c" + seasonName));
    }

    public String getSeasonStory() {
        String name = plugin.getConfig().getString("seasonal-story.current-name", "Blood Plague");
        List<String> beats = plugin.getConfig().getStringList("seasonal-story.beats");
        if (beats.isEmpty()) {
            beats = List.of("Blood Plague", "Marked Uprising", "Fallen Kings");
        }
        return colorize("&cCurrent Arc: &f" + name + " &8| &7Full saga: &f" + String.join(" &8→ &f", beats));
    }

    public String getSeasonTimingLine() {
        int seasonDays = Math.max(1, plugin.getConfig().getInt("season.length", 30));
        long seasonDurationMillis = seasonDays * 24L * 60L * 60L * 1000L;
        long seasonStart = plugin.getExpansionDataManager().getSeasonStartMillis();
        long seasonEnd = seasonStart + seasonDurationMillis;
        long remaining = Math.max(0L, seasonEnd - System.currentTimeMillis());

        long totalMinutes = remaining / 60_000L;
        long days = totalMinutes / (60L * 24L);
        long hours = (totalMinutes % (60L * 24L)) / 60L;
        long minutes = totalMinutes % 60L;

        return colorize("&7Season " + plugin.getExpansionDataManager().getSeasonNumber() + " ends in &f"
                + days + "d " + hours + "h " + minutes + "m"
                + " &8| &7Final Hour: " + (isFinalHourActive() ? "&aACTIVE" : "&cpending"));
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long min = seconds / 60;
        long sec = seconds % 60;
        return min + "m " + sec + "s";
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private enum EliteMobType {
        ABYSS_STALKER("abyss-stalker", "&5&lAbyss Stalker", 130.0, 12.0, 16),
        PLAGUE_BROODMOTHER("plague-broodmother", "&2&lPlague Broodmother", 160.0, 10.0, 18),
        INFERNAL_BRUTE("infernal-brute", "&c&lInfernal Brute", 200.0, 14.0, 20),
        DREAD_HUSK("dread-husk", "&6&lDread Husk", 180.0, 11.0, 17);

        private final String configKey;
        private final String displayName;
        private final double defaultHealth;
        private final double defaultDamage;
        private final int defaultReward;

        EliteMobType(String configKey, String displayName, double defaultHealth, double defaultDamage, int defaultReward) {
            this.configKey = configKey;
            this.displayName = displayName;
            this.defaultHealth = defaultHealth;
            this.defaultDamage = defaultDamage;
            this.defaultReward = defaultReward;
        }
    }

    private record AssistContribution(double damage, long lastHitMillis) {}
    private record KillComboState(int combo, long lastKillMillis) {}
    private record KillRecord(UUID killerUuid, long timestampMillis) {}

    public record DamageLog(long timestamp, String source, double damage, String cause) {}
}
