package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatLogListener implements Listener {

    private final BloodpineCore plugin;
    private final Map<UUID, Long> combatTag;
    private final Map<UUID, UUID> lastAttacker;
    private final Map<UUID, CombatLoggerBody> bodiesByEntity;
    private final Map<UUID, UUID> bodyEntityByPlayer;

    public CombatLogListener(BloodpineCore plugin) {
        this.plugin = plugin;
        this.combatTag = new HashMap<>();
        this.lastAttacker = new HashMap<>();
        this.bodiesByEntity = new HashMap<>();
        this.bodyEntityByPlayer = new HashMap<>();
    }

    public void tagPlayer(Player player, Player attacker) {
        UUID uuid = player.getUniqueId();
        boolean wasInCombat = isInCombat(player);
        combatTag.put(uuid, System.currentTimeMillis());
        lastAttacker.put(uuid, attacker.getUniqueId());

        if (!wasInCombat) {
            player.sendMessage(colorize("&c&l⚔ You are now in combat! Don't log out!"));
        }
    }

    public boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTag.containsKey(uuid)) {
            return false;
        }

        long tagTime = combatTag.get(uuid);
        long currentTime = System.currentTimeMillis();

        if (currentTime - tagTime > 15_000L) {
            removeCombatTag(player);
            return false;
        }

        return true;
    }

    public void removeCombatTag(Player player) {
        UUID uuid = player.getUniqueId();
        combatTag.remove(uuid);
        lastAttacker.remove(uuid);
        player.sendMessage(colorize("&a&l✓ You are no longer in combat."));
    }

    public void clearPlayerState(UUID uuid) {
        combatTag.remove(uuid);
        lastAttacker.remove(uuid);

        UUID bodyUuid = bodyEntityByPlayer.remove(uuid);
        if (bodyUuid != null) {
            bodiesByEntity.remove(bodyUuid);
            Entity entity = Bukkit.getEntity(bodyUuid);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isInCombat(player)) {
            return;
        }

        int bodySeconds = Math.max(3, plugin.getConfig().getInt("combat-logger.body-seconds", 10));
        UUID attackerUuid = lastAttacker.get(player.getUniqueId());
        combatTag.remove(player.getUniqueId());
        lastAttacker.remove(player.getUniqueId());

        Zombie body = spawnCombatLoggerBody(player);
        if (body == null) {
            return;
        }

        CombatLoggerBody bodyData = new CombatLoggerBody(
                player.getUniqueId(),
                player.getName(),
                body.getUniqueId(),
                body.getLocation().clone(),
                attackerUuid,
                System.currentTimeMillis() + (bodySeconds * 1000L)
        );
        bodiesByEntity.put(body.getUniqueId(), bodyData);
        bodyEntityByPlayer.put(player.getUniqueId(), body.getUniqueId());

        Bukkit.broadcastMessage(colorize("&c" + player.getName() + " combat logged. Their body remains vulnerable for " + bodySeconds + "s."));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            CombatLoggerBody active = bodiesByEntity.remove(body.getUniqueId());
            if (active == null) {
                return;
            }
            bodyEntityByPlayer.remove(active.playerUuid());

            Entity entity = Bukkit.getEntity(active.entityUuid());
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }

            Bukkit.broadcastMessage(colorize("&7" + active.playerName() + " survived their combat-log timer."));
        }, bodySeconds * 20L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID entityUuid = event.getEntity().getUniqueId();
        CombatLoggerBody bodyData = bodiesByEntity.remove(entityUuid);
        if (bodyData == null) {
            return;
        }

        bodyEntityByPlayer.remove(bodyData.playerUuid());
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = event.getEntity().getKiller();
        if (killer == null && bodyData.lastAttackerUuid() != null) {
            killer = Bukkit.getPlayer(bodyData.lastAttackerUuid());
        }

        handleCombatLoggerDeath(bodyData, killer);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        combatTag.remove(player.getUniqueId());
        lastAttacker.remove(player.getUniqueId());
    }

    private Zombie spawnCombatLoggerBody(Player player) {
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return null;
        }

        Zombie body = location.getWorld().spawn(location, Zombie.class, zombie -> {
            zombie.setAdult();
            zombie.setAI(false);
            zombie.setSilent(true);
            zombie.setCanPickupItems(false);
            zombie.setShouldBurnInDay(false);
            zombie.setRemoveWhenFarAway(false);
            zombie.setCustomName(colorize("&c&lCombat Logger &7- &f" + player.getName()));
            zombie.setCustomNameVisible(true);

            double health = Math.max(2.0, Math.min(40.0, player.getHealth()));
            if (zombie.getAttribute(Attribute.MAX_HEALTH) != null) {
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            }
            zombie.setHealth(health);
        });

        ItemStack helmet = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        if (helmet.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            helmet.setItemMeta(skullMeta);
        }
        body.getEquipment().setHelmet(helmet);

        body.getEquipment().setChestplate(player.getInventory().getChestplate());
        body.getEquipment().setLeggings(player.getInventory().getLeggings());
        body.getEquipment().setBoots(player.getInventory().getBoots());
        body.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());
        body.getEquipment().setItemInOffHand(player.getInventory().getItemInOffHand());

        body.getEquipment().setHelmetDropChance(0.0f);
        body.getEquipment().setChestplateDropChance(0.0f);
        body.getEquipment().setLeggingsDropChance(0.0f);
        body.getEquipment().setBootsDropChance(0.0f);
        body.getEquipment().setItemInMainHandDropChance(0.0f);
        body.getEquipment().setItemInOffHandDropChance(0.0f);

        return body;
    }

    private void handleCombatLoggerDeath(CombatLoggerBody bodyData, Player killer) {
        boolean lifestealEnabled = plugin.getConfig().getBoolean("lifesteal.enabled", true);
        PlayerData victimData = plugin.getDataManager().getPlayerData(bodyData.playerUuid());
        plugin.getGameplayExpansionManager().clearAssistTracking(bodyData.playerUuid());
        victimData.addDeath();

        if (lifestealEnabled) {
            boolean insured = victimData.consumeInsuredHeart();
            if (!insured) {
                victimData.removeLifestealHeart();
                int extraLoss = plugin.getGameplayExpansionManager().getExtraHeartLossInBloodZone(bodyData.logoutLocation());
                for (int index = 0; index < extraLoss; index++) {
                    victimData.removeLifestealHeart();
                }
            }
        }

        if (killer != null && killer.isOnline()) {
            PlayerData killerData = plugin.getDataManager().getPlayerData(killer);
            killerData.addKill();
            plugin.getKillstreakManager().addKill(killer);

            int baseReward = Math.max(1, plugin.getConfig().getInt("tokens.per-kill", 1));
            int boostMultiplier = plugin.getBoostManager().getTokenMultiplier(killer);
            double eventMultiplier = 1.0;
            if (plugin.getGameplayExpansionManager().isBloodMoonActive()) {
                eventMultiplier *= 2.0;
            }
            if (plugin.getGameplayExpansionManager().isFinalHourActive()) {
                eventMultiplier *= plugin.getConfig().getDouble("season.final-hour.token-multiplier", 2.0);
            }

            int reward = Math.max(1, (int) Math.round(baseReward * boostMultiplier * eventMultiplier));
            plugin.getTokenManager().giveTokens(killer, reward);

            if (victimData.isMarked()) {
                int penalty = Math.max(0, plugin.getConfig().getInt("tokens.marked-death-penalty", 5));
                victimData.removeTokens(penalty);
                int threshold = plugin.getConfig().getInt("marked.threshold", 25);
                if (victimData.getTotalTokens() < threshold) {
                    victimData.setMarked(false);
                }

                int bonus = Math.max(1, (int) Math.round(penalty * boostMultiplier * eventMultiplier));
                plugin.getTokenManager().giveTokens(killer, bonus);
                killer.sendMessage(colorize("&aMarked combat-logger kill bonus: &e+" + bonus + " tokens"));
            }

            if (lifestealEnabled) {
                killerData.addLifestealHeart();
                plugin.getExpansionDataManager().getProfile(killer).addHeartsGainedSeason(1);
            }
            plugin.getGameplayExpansionManager().onPlayerKill(killer, bodyData.playerName(), victimData);
            plugin.getStatManager().applyStats(killer);
            plugin.getDisplayManager().updateDisplay(killer);
            plugin.getSidebarManager().updatePlayer(killer);

            killer.sendMessage(colorize("&c" + bodyData.playerName() + " combat logged and their body was slain."));
            killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
            Bukkit.broadcastMessage(colorize("&c&l" + bodyData.playerName() + " combat logged and was killed!"));
        } else {
            Bukkit.broadcastMessage(colorize("&c&l" + bodyData.playerName() + " combat logger body died."));
        }

        if (lifestealEnabled && wouldBeEliminated(victimData)) {
            eliminateOfflinePlayer(bodyData.playerName());
        }
    }

    private boolean wouldBeEliminated(PlayerData data) {
        double minHealth = plugin.getConfig().getDouble("lifesteal.min-hearts", 1.0) * 2.0;
        double maxHealth = computeMaxHealth(data);
        return maxHealth <= minHealth;
    }

    private double computeMaxHealth(PlayerData data) {
        int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
        double baseHealth = startingHearts * 2.0;
        double lifestealHealth = data.getLifestealHearts() * 2.0;
        double vitalityHealth = data.getAllocatedTokens(StatType.VITALITY)
                * plugin.getConfig().getDouble("stats.vitality.per-token", 2.0);
        double rebirthHealth = data.getRebirthLevel() * plugin.getRebirthManager().getVitalityHeartsPerLevel() * 2.0;
        double maxHealthCap = plugin.getConfig().getDouble("lifesteal.max-hearts", 22.0) * 2.0;

        double total = baseHealth + lifestealHealth + vitalityHealth + rebirthHealth;
        if (maxHealthCap > 0) {
            return Math.min(maxHealthCap, total);
        }
        return total;
    }

    private void eliminateOfflinePlayer(String playerName) {
        int seasonLength = plugin.getConfig().getInt("season.length", 30);
        long banDuration = seasonLength * 24L * 60L * 60L * 1000L;
        Date banExpiry = new Date(System.currentTimeMillis() + banDuration);

        Bukkit.getBanList(BanList.Type.NAME).addBan(
                playerName,
                ChatColor.RED + "You have been eliminated!\n"
                        + ChatColor.GRAY + "You ran out of hearts.\n"
                        + ChatColor.YELLOW + "See you next season!",
                banExpiry,
                "Bloodpine Lifesteal System"
        );

        Bukkit.broadcastMessage(colorize("&c&l⚠ " + playerName + " HAS BEEN ELIMINATED! &7They are banned until next season (&f"
                + new SimpleDateFormat("MMM dd, yyyy").format(banExpiry) + "&7)."));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private record CombatLoggerBody(
            UUID playerUuid,
            String playerName,
            UUID entityUuid,
            Location logoutLocation,
            UUID lastAttackerUuid,
            long expiresAt
    ) {}
}
