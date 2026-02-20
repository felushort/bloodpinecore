package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExpansionGUIManager {

    private final BloodpineCore plugin;

    public ExpansionGUIManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public void openMainHub(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, colorize("&c&lBloodpine Hub"));

        gui.setItem(10, item(Material.DIAMOND_SWORD, "&cPvP Stats", List.of("&7Open detailed PvP profile panel")));
        gui.setItem(11, item(Material.RED_MUSHROOM, "&dEvents", List.of("&7Manage/view BloodMoon, Arena, Hunt")));
        gui.setItem(12, item(Material.ENDER_CHEST, "&8Black Market", List.of("&7Contraband and risky perks")));
        gui.setItem(13, item(Material.GOLD_INGOT, "&6Economy", List.of("&7Gambling and combat crates")));
        gui.setItem(14, item(Material.CHEST, "&cBounty Board", List.of("&7Top targets and implied bounties")));
        gui.setItem(15, item(Material.BOOK, "&8Season Arc", List.of("&7Current wipe narrative and lore")));
        gui.setItem(16, item(Material.OAK_SIGN, "&aLearn Bloodpine", List.of("&7New player tutorial and quick-start")));

        gui.setItem(28, item(Material.WRITABLE_BOOK, "&bDaily Quest", List.of("&7Daily PvP objective + rewards")));
        gui.setItem(29, item(Material.MAP, "&6Contracts", List.of("&7Target contracts and progress")));
        gui.setItem(30, item(Material.BEACON, "&5Ascension", List.of("&7Endgame prestige beyond rebirth")));
        gui.setItem(31, item(Material.RABBIT_FOOT, "&bParkour", List.of("&7Spawn challenge timer and rewards")));
        gui.setItem(32, item(Material.RED_DYE, "&4Combat Replay", List.of("&7Recent incoming damage timeline")));

        gui.setItem(40, item(Material.CLOCK, "&eRefresh", List.of("&7Reload this hub panel")));

        player.openInventory(gui);
    }

        public void openLearnCenter(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, colorize("&a&lLearn Bloodpine"));

        gui.setItem(10, item(Material.WRITABLE_BOOK, "&f1) The Core Loop", List.of(
            "&7Kill players to earn tokens",
            "&7Use /menu to allocate stats",
            "&7Rebirth at milestones for scaling"
        )));

        gui.setItem(11, item(Material.RED_DYE, "&c2) Lifesteal Rules", List.of(
            "&7PvP death: lose hearts",
            "&7PvP kill: gain hearts",
            "&7Run out of hearts = elimination"
        )));

        gui.setItem(12, item(Material.NETHER_STAR, "&d3) Marked & Bounties", List.of(
            "&7High tokens can make you marked",
            "&7Marked players are high-value targets",
            "&7Use /bounty and /bountyboard"
        )));

        gui.setItem(13, item(Material.CLOCK, "&64) Live Events", List.of(
            "&7Blood Moon = faster progression",
            "&7Arena and hunt rotate",
            "&7Boss spawns on timer"
        )));

        gui.setItem(14, item(Material.WARDEN_SPAWN_EGG, "&55) World Boss", List.of(
            "&7Boss rewards include tokens + items",
            "&7Join with others for stronger payout",
            "&7Use /events to check status"
        )));

        gui.setItem(15, item(Material.ENDER_CHEST, "&86) Black Market", List.of(
            "&730+ tokens unlocks access",
            "&7High risk perks and contraband",
            "&7Use /blackmarket"
        )));

        gui.setItem(28, item(Material.DIAMOND_SWORD, "&bOpen PvP Stats", List.of("&7Click to run /pvpstats")));
        gui.setItem(29, item(Material.GOLD_INGOT, "&6Open Economy", List.of("&7Click to run /gamble")));
        gui.setItem(30, item(Material.MAP, "&eOpen Contracts", List.of("&7Click to run /contract")));
        gui.setItem(31, item(Material.PAPER, "&cOpen Bounty Board", List.of("&7Click to run /bountyboard")));
        gui.setItem(32, item(Material.BEACON, "&5Open Ascension", List.of("&7Click to run /ascend")));
        gui.setItem(33, item(Material.BOOK, "&aOpen Full Guide Book", List.of("&7Click to run /guide")));

        gui.setItem(49, item(Material.COMPASS, "&eOpen Main Hub", List.of("&7Back to /hub")));

        player.openInventory(gui);
        }

    public void openPvpStats(Player viewer, Player target) {
        PlayerData data = plugin.getDataManager().getPlayerData(target);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(target);
        double dps = plugin.getGameplayExpansionManager().getRecentDps(target);

        Inventory gui = Bukkit.createInventory(null, 45, colorize("&c&lPvP Stats &8» &f" + target.getName()));

        gui.setItem(10, item(Material.DIAMOND_SWORD, "&c&lKDR", List.of(
                "&7Kills: &a" + data.getTotalKills(),
                "&7Deaths: &c" + data.getTotalDeaths(),
                "&7Ratio: &e" + String.format("%.2f", data.getKDRatio())
        )));

        gui.setItem(12, item(Material.BLAZE_POWDER, "&6&lRecent DPS", List.of(
                "&7Current: &e" + String.format("%.2f", dps),
                "&8Damage per second over last 10s.",
                "&8Shows your recent PvP burst output."
        )));

        gui.setItem(14, item(Material.NETHER_STAR, "&5&lAscension", List.of(
                "&7Current Tier: &d" + profile.getAscension(),
                "&8Ascension is endgame prestige after high rebirth.",
                "&8It grants top-tier identity and status."
        )));

        gui.setItem(20, item(Material.EMERALD, "&a&lReputation", List.of(
                "&7Value: &a" + profile.getReputation(),
                "&8Goes up from worthy fights.",
                "&8Drops if you farm weak low-token players."
        )));

        gui.setItem(22, item(Material.GOLD_INGOT, "&6&lFame", List.of(
                "&7Value: &6" + profile.getFame(),
                "&8Fame rises when you beat high-value targets.",
                "&8Used as a social flex/ranking identity."
        )));

        gui.setItem(24, item(Material.REDSTONE, "&4&lInfamy", List.of(
                "&7Value: &4" + profile.getInfamy(),
                "&8Infamy rises from predatory kills.",
                "&8High infamy can make you publicly visible."
        )));

        gui.setItem(28, item(Material.IRON_AXE, "&6&lLongest Streak", List.of(
                "&7Best Killstreak: &6" + data.getLongestKillstreak(),
                "&8Tracks your best uninterrupted PvP run."
        )));

        gui.setItem(30, item(Material.NETHER_WART, "&c&lHearts Gained", List.of(
                "&7This Season: &c" + profile.getHeartsGainedSeason(),
                "&8How many lifesteal hearts you've earned this season."
        )));

        gui.setItem(32, item(Material.WRITABLE_BOOK, "&e&lContracts", List.of(
                "&7Completed: &e" + profile.getContractsCompletedTotal(),
                "&8Lifetime contract completions."
        )));

        gui.setItem(34, item(Material.GOLD_NUGGET, "&a&lAssists", List.of(
                "&7Total Assists: &a" + data.getTotalAssists(),
                "&8Earned by contributing meaningful damage."
        )));

        gui.setItem(31, item(Material.PAPER, "&bView Leaderboards", List.of("&7Click to run /pvpstats leaderboard")));
        gui.setItem(40, item(Material.CLOCK, "&eRefresh", List.of("&7Click to refresh this panel")));

        viewer.openInventory(gui);
    }

    public void openEvents(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, colorize("&d&lEvents Panel"));

        gui.setItem(10, item(Material.RED_MUSHROOM, "&4Start Blood Moon", List.of("&72x tokens for 20 minutes")));
        gui.setItem(12, item(Material.COMPASS, "&cStart Hunt Marked", List.of("&7Global hunt event")));
        gui.setItem(14, item(Material.IRON_SWORD, "&6Start Arena", List.of("&7Normalize arena gear")));
        gui.setItem(16, item(Material.WARDEN_SPAWN_EGG, "&5Spawn Boss", List.of("&7Spawn world boss")));
        gui.setItem(24, item(Material.CLOCK, "&4Start Final Hour", List.of("&7Season finale chaos modifiers")));
        gui.setItem(26, item(Material.CHEST, "&6Force Supply Drop", List.of("&7Instant chest drop in blood zone")));
        gui.setItem(22, item(Material.BOOK, "&2Bosses Removed", List.of("&7Only Blood Tyrant is enabled.", "&7Use Spawn Boss for Blood Tyrant.")));
        gui.setItem(30, item(Material.BARRIER, "&7Stop Arena", List.of("&7End arena normalization event")));
        gui.setItem(32, item(Material.REDSTONE_BLOCK, "&4Stop All Events", List.of("&7Instantly stop Blood Moon, Hunt, Arena, Boss")));
        gui.setItem(34, item(Material.BLACK_WOOL, "&8Stop Final Hour", List.of("&7Disable final hour modifiers")));
        gui.setItem(40, item(Material.COMPASS, "&eStatus", List.of(plugin.getGameplayExpansionManager().getEventStatusLine())));

        player.openInventory(gui);
    }

    public void openBlackMarket(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&8&lBlack Market"));
        gui.setItem(10, item(Material.ENDER_PEARL, "&5Warp To Dealer", List.of("&7Requires 30+ total tokens")));
        gui.setItem(12, item(Material.ENCHANTED_BOOK, "&4Illegal Enchant Tome", List.of("&7Risky contraband enchant")));
        gui.setItem(14, item(Material.POTION, "&6Risky Buff", List.of("&7Could buff or curse you")));
        gui.setItem(16, item(Material.BLAZE_ROD, "&eCombat Perk", List.of("&7Temp combat advantages")));
        player.openInventory(gui);
    }

    public void openDailyQuest(Player player) {
        plugin.getExpansionDataManager().resetDailyIfNeeded(player);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        int goal = plugin.getConfig().getInt("spawn.daily-quest-kill-goal", 3);
        int reward = plugin.getConfig().getInt("spawn.daily-quest-reward", 10);

        Inventory gui = Bukkit.createInventory(null, 27, colorize("&b&lDaily Quest"));
        gui.setItem(11, item(Material.IRON_SWORD, "&fToday's Objective", List.of(
                "&7Get &e" + goal + " &7PvP kills",
                "&7Progress: &e" + profile.getDailyQuestProgress() + "&7/" + goal,
                "&7Reward: &6" + reward + " tokens"
        )));
        gui.setItem(15, item(Material.CHEST, "&aClaim Reward", List.of("&7Click to run /dailyquest claim")));
        player.openInventory(gui);
    }

    public void openContract(Player player) {
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&6&lContracts"));

        String target = profile.getContractTarget().isEmpty() ? "None" : profile.getContractTarget();
        gui.setItem(11, item(Material.PLAYER_HEAD, "&fActive Target", List.of(
                "&7Target: &e" + target,
                "&7Progress: &e" + profile.getContractProgress() + "&7/" + profile.getContractGoal()
        )));
        gui.setItem(15, item(Material.WRITABLE_BOOK, "&aNew Contract", List.of("&7Click to reroll contract target")));
        player.openInventory(gui);
    }

    public void openBountyBoard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, colorize("&c&lBounty Board"));

        List<PlayerData> list = new ArrayList<>(plugin.getDataManager().getAllPlayerData());
        list.sort(Comparator.comparingInt(PlayerData::getTotalTokens).reversed());

        int slot = 10;
        int count = 0;
        for (PlayerData data : list) {
            if (count >= 28) break;
            if (slot >= 53) break;
            int bounty = (int) (data.getTotalTokens() * plugin.getConfig().getDouble("marked.bounty-multiplier", 2.0));
            gui.setItem(slot, item(Material.PAPER, "&f" + data.getName(), List.of(
                    "&7Tokens: &e" + data.getTotalTokens(),
                    "&7Implied Bounty: &c" + bounty
            )));
            slot++;
            count++;
        }

        gui.setItem(49, item(Material.CLOCK, "&eRefresh", List.of("&7Click to refresh board")));
        player.openInventory(gui);
    }

    public void openAscension(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        int needRebirth = plugin.getConfig().getInt("endgame.ascension-min-rebirth", 5);
        int cost = plugin.getConfig().getInt("endgame.ascension-token-cost", 40);

        Inventory gui = Bukkit.createInventory(null, 27, colorize("&5&lAscension"));
        gui.setItem(11, item(Material.NETHER_STAR, "&dAscension Status", List.of(
                "&7Current Ascension: &d" + profile.getAscension(),
                "&7Your Rebirth: &5" + data.getRebirthLevel(),
                "&7Required Rebirth: &f" + needRebirth,
                "&7Token Cost: &e" + cost
        )));
        gui.setItem(15, item(Material.BEACON, "&aAscend Now", List.of("&7Click to run /ascend")));
        player.openInventory(gui);
    }

    public void openEconomy(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&6&lEconomy Sinks"));
        gui.setItem(10, item(Material.GOLD_NUGGET, "&eGamble 10", List.of("&7Click to run /gamble 10")));
        gui.setItem(11, item(Material.GOLD_INGOT, "&eGamble 25", List.of("&7Click to run /gamble 25")));
        gui.setItem(12, item(Material.GOLD_BLOCK, "&eGamble 50", List.of("&7Click to run /gamble 50")));
        gui.setItem(14, item(Material.CHEST, "&cOpen Combat Crate", List.of("&7Click to run /combatcrate open")));
        gui.setItem(15, item(Material.ANVIL, "&4Blood Forge", List.of("&7Burn tokens for permanent damage scaling", "&7Click to run /bloodforge forge")));
        gui.setItem(16, item(Material.SHIELD, "&bHeart Insurance", List.of("&7Protect one future heart loss", "&7Click to run /heartinsurance buy")));
        player.openInventory(gui);
    }

    public void openParkour(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&b&lParkour Challenge"));
        gui.setItem(11, item(Material.LIME_WOOL, "&aStart Timer", List.of("&7Click to run /parkour start")));
        gui.setItem(15, item(Material.RED_WOOL, "&cFinish Timer", List.of("&7Click to run /parkour finish")));
        player.openInventory(gui);
    }

    public void openSeason(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, colorize("&8&lSeason Arc"));
        gui.setItem(13, item(Material.BOOK, "&cCurrent Story Arc", List.of(
                ChatColor.stripColor(plugin.getGameplayExpansionManager().getSeasonStory())
        )));
        player.openInventory(gui);
    }

    public void openCombatReplay(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, colorize("&4&lCombat Replay"));

        List<GameplayExpansionManager.DamageLog> logs = plugin.getGameplayExpansionManager().getIncomingDamage(player, 28);
        int slot = 10;
        for (GameplayExpansionManager.DamageLog log : logs) {
            if (slot >= 53) break;
            gui.setItem(slot, item(Material.RED_DYE, "&fFrom: &c" + log.source(), List.of(
                    "&7Damage: &c" + String.format("%.2f", log.damage()),
                    "&7Cause: &e" + log.cause(),
                    "&8Time: " + log.timestamp()
            )));
            slot++;
        }

        gui.setItem(49, item(Material.CLOCK, "&eRefresh", List.of("&7Click to refresh replay")));
        player.openInventory(gui);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorize(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
