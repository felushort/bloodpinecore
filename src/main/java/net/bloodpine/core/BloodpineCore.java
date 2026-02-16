package net.bloodpine.core;

import net.bloodpine.core.commands.*;
import net.bloodpine.core.listeners.*;
import net.bloodpine.core.managers.*;
import net.bloodpine.core.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BloodpineCore extends JavaPlugin {
    
    private static BloodpineCore instance;
    private DataManager dataManager;
    private TokenManager tokenManager;
    private StatManager statManager;
    private DisplayManager displayManager;
    private MarkedManager markedManager;
    private KillstreakManager killstreakManager;
    private BountyManager bountyManager;
    private BoostManager boostManager;
    private RebirthManager rebirthManager;
    private SidebarManager sidebarManager;
    private AchievementManager achievementManager;
    private DailyRewardManager dailyRewardManager;
    private CooldownManager cooldownManager;
    private CombatStatsManager combatStatsManager;
    private CombatLogListener combatLogListener;
    private StatsGUI statsGUI;
    private LeaderboardGUI leaderboardGUI;
    private TokenShopGUI tokenShopGUI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Validate configuration
        ConfigValidator validator = new ConfigValidator(this);
        if (!validator.validate()) {
            getLogger().severe("Configuration validation failed! Plugin may not work correctly.");
            getLogger().severe("Please fix the configuration errors and reload the plugin.");
        }
        
        // Initialize managers
        dataManager = new DataManager(this);
        tokenManager = new TokenManager(this);
        statManager = new StatManager(this);
        displayManager = new DisplayManager(this);
        markedManager = new MarkedManager(this);
        killstreakManager = new KillstreakManager(this);
        bountyManager = new BountyManager(this);
        boostManager = new BoostManager(this);
        rebirthManager = new RebirthManager(this);
        sidebarManager = new SidebarManager(this);
        achievementManager = new AchievementManager(this);
        dailyRewardManager = new DailyRewardManager(this);
        cooldownManager = new CooldownManager(this);
        combatStatsManager = new CombatStatsManager(this);
        combatLogListener = new CombatLogListener(this);
        
        // Initialize GUIs
        statsGUI = new StatsGUI(this);
        leaderboardGUI = new LeaderboardGUI(this);
        tokenShopGUI = new TokenShopGUI(this);
        
        // Load data
        dataManager.loadData();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start tasks
        startTasks();
        
        // Start auto-save
        dataManager.startAutoSave();
        
        getLogger().info("Bloodpine Ascension Core has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (dataManager != null) {
            dataManager.saveData();
        }
        
        getLogger().info("Bloodpine Ascension Core has been disabled!");
    }
    
    private void registerCommands() {
        getCommand("menu").setExecutor(new MenuCommand(this));
        getCommand("tokens").setExecutor(new TokensCommand(this));
        getCommand("allocate").setExecutor(new AllocateCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("resetstats").setExecutor(new ResetStatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("bounty").setExecutor(new BountyCommand(this));
        getCommand("bloodpine").setExecutor(new AdminCommand(this));
        getCommand("givetoken").setExecutor(new GiveTokenCommand(this));
        getCommand("rebirth").setExecutor(new RebirthCommand(this));
        getCommand("tshop").setExecutor(new TokenShopCommand(this));
        getCommand("paytokens").setExecutor(new PayTokensCommand(this));
        getCommand("payhearts").setExecutor(new PayHeartsCommand(this));
        getCommand("end").setExecutor(new EndCommand(this));
        getCommand("guide").setExecutor(new GuideCommand());
        getCommand("givetokenitem").setExecutor(new GiveTokenItemCommand(this));
        getCommand("giveheartitem").setExecutor(new GiveHeartItemCommand(this));
        getCommand("achievements").setExecutor(new AchievementsCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("combatstats").setExecutor(new CombatStatsCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new TotemListener(this), this);
        getServer().getPluginManager().registerEvents(new LifestealListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EndLockListener(this), this);
        getServer().getPluginManager().registerEvents(combatLogListener, this);
        getServer().getPluginManager().registerEvents(new ItemRedeemListener(this), this);
    }
    
    private void startTasks() {
        // Update nametags periodically
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            displayManager.updateAllDisplays();
            sidebarManager.updateAll();
        }, 20L, getConfig().getLong("display.update-interval", 20L));
        
        // Check and update marked players
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            markedManager.checkMarkedPlayers();
        }, 100L, 100L);
        
        // Cleanup cooldowns periodically (every 5 minutes)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            cooldownManager.cleanup();
        }, 6000L, 6000L);
    }
    
    public static BloodpineCore getInstance() {
        return instance;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public TokenManager getTokenManager() {
        return tokenManager;
    }
    
    public StatManager getStatManager() {
        return statManager;
    }
    
    public DisplayManager getDisplayManager() {
        return displayManager;
    }
    
    public MarkedManager getMarkedManager() {
        return markedManager;
    }
    
    public KillstreakManager getKillstreakManager() {
        return killstreakManager;
    }
    
    public BountyManager getBountyManager() {
        return bountyManager;
    }
    
    public StatsGUI getStatsGUI() {
        return statsGUI;
    }
    
    public LeaderboardGUI getLeaderboardGUI() {
        return leaderboardGUI;
    }
    
    public CombatLogListener getCombatLogListener() {
        return combatLogListener;
    }
    
    public BoostManager getBoostManager() {
        return boostManager;
    }

    public RebirthManager getRebirthManager() {
        return rebirthManager;
    }

    public TokenShopGUI getTokenShopGUI() {
        return tokenShopGUI;
    }

    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }
    
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public DailyRewardManager getDailyRewardManager() {
        return dailyRewardManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public CombatStatsManager getCombatStatsManager() {
        return combatStatsManager;
    }
}
