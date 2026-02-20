package net.bloodpine.core;

import net.bloodpine.core.commands.*;
import net.bloodpine.core.listeners.*;
import net.bloodpine.core.managers.*;
import net.bloodpine.core.gui.*;
import net.bloodpine.core.security.OpLockManager;
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
    private ExpansionDataManager expansionDataManager;
    private GameplayExpansionManager gameplayExpansionManager;
    private ExpansionGUIManager expansionGUIManager;
    private RedeemCodeManager redeemCodeManager;
    private AuthManager authManager;
    private AntiVpnManager antiVpnManager;
    private SessionLocationManager sessionLocationManager;
    private CombatLogListener combatLogListener;
    private StatsGUI statsGUI;
    private LeaderboardGUI leaderboardGUI;
    private TokenShopGUI tokenShopGUI;
    private OpLockManager opLockManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
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
        expansionDataManager = new ExpansionDataManager(this);
        gameplayExpansionManager = new GameplayExpansionManager(this);
        expansionGUIManager = new ExpansionGUIManager(this);
        redeemCodeManager = new RedeemCodeManager(this);
        authManager = new AuthManager(this);
        antiVpnManager = new AntiVpnManager(this);
        sessionLocationManager = new SessionLocationManager(this);
        combatLogListener = new CombatLogListener(this);

        // Security systems
        opLockManager = new OpLockManager(this);
        opLockManager.init();
        
        // Initialize GUIs
        statsGUI = new StatsGUI(this);
        leaderboardGUI = new LeaderboardGUI(this);
        tokenShopGUI = new TokenShopGUI(this);
        
        // Load data
        dataManager.loadData();
        expansionDataManager.load();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start tasks
        startTasks();
        
        getLogger().info("Bloodpine Ascension Core has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (opLockManager != null) {
            opLockManager.stop();
        }

        // Save all data
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (expansionDataManager != null) {
            expansionDataManager.save();
        }
        if (redeemCodeManager != null) {
            redeemCodeManager.save();
        }
        if (authManager != null) {
            authManager.save();
        }
        if (sessionLocationManager != null) {
            sessionLocationManager.saveOnlinePlayers();
            sessionLocationManager.save();
        }
        
        getLogger().info("Bloodpine Ascension Core has been disabled!");
    }
    
    private void registerCommands() {
        boolean lifestealEnabled = getConfig().getBoolean("lifesteal.enabled", true);

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
        getCommand("bloodforge").setExecutor(new BloodForgeCommand(this));
        if (lifestealEnabled) {
            getCommand("heartinsurance").setExecutor(new HeartInsuranceCommand(this));
        }
        getCommand("paytokens").setExecutor(new PayTokensCommand(this));
        if (lifestealEnabled) {
            getCommand("payhearts").setExecutor(new PayHeartsCommand(this));
        }
        getCommand("end").setExecutor(new EndCommand(this));
        getCommand("guide").setExecutor(new GuideCommand());
        getCommand("store").setExecutor(new StoreCommand());
        getCommand("givetokenitem").setExecutor(new GiveTokenItemCommand(this));
        if (lifestealEnabled) {
            getCommand("giveheartitem").setExecutor(new GiveHeartItemCommand(this));
        }
        getCommand("events").setExecutor(new EventsCommand(this));
        getCommand("blackmarket").setExecutor(new BlackMarketCommand(this));
        getCommand("pvpstats").setExecutor(new PvpStatsCommand(this));
        getCommand("gamble").setExecutor(new GambleCommand(this));
        getCommand("combatcrate").setExecutor(new CombatCrateCommand(this));
        getCommand("dailyquest").setExecutor(new DailyQuestCommand(this));
        getCommand("contract").setExecutor(new ContractCommand(this));
        getCommand("bountyboard").setExecutor(new BountyBoardCommand(this));
        getCommand("parkour").setExecutor(new ParkourCommand(this));
        getCommand("season").setExecutor(new SeasonCommand(this));
        getCommand("ascend").setExecutor(new AscendCommand(this));
        getCommand("combatreplay").setExecutor(new CombatReplayCommand(this));
        getCommand("hub").setExecutor(new HubCommand(this));
        getCommand("learn").setExecutor(new LearnCommand(this));
        getCommand("redeem").setExecutor(new RedeemCodeCommand(this));
        getCommand("redeemadmin").setExecutor(new RedeemAdminCommand(this));
        if (lifestealEnabled) {
            getCommand("sethearts").setExecutor(new SetHeartsCommand(this));
        }
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
    }
    
    private void registerListeners() {
        boolean lifestealEnabled = getConfig().getBoolean("lifesteal.enabled", true);

        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new TotemListener(this), this);
        if (lifestealEnabled) {
            getServer().getPluginManager().registerEvents(new LifestealListener(this), this);
        }
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EndLockListener(this), this);
        getServer().getPluginManager().registerEvents(combatLogListener, this);
        getServer().getPluginManager().registerEvents(new ItemRedeemListener(this), this);
        getServer().getPluginManager().registerEvents(new ExpansionListener(this), this);
        getServer().getPluginManager().registerEvents(new ExpansionGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new DuplicateNameGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new AuthGuardListener(this), this);
        // Security + mechanics
        if (opLockManager != null) {
            getServer().getPluginManager().registerEvents(new OpLockListener(opLockManager), this);
        }
        getServer().getPluginManager().registerEvents(new SilkTouchSpawnerListener(this), this);
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

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (sessionLocationManager != null) {
                sessionLocationManager.saveOnlinePlayers();
            }
        }, 200L, 600L);

        gameplayExpansionManager.startSchedulers();
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

    public ExpansionDataManager getExpansionDataManager() {
        return expansionDataManager;
    }

    public GameplayExpansionManager getGameplayExpansionManager() {
        return gameplayExpansionManager;
    }

    public ExpansionGUIManager getExpansionGUIManager() {
        return expansionGUIManager;
    }

    public RedeemCodeManager getRedeemCodeManager() {
        return redeemCodeManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public AntiVpnManager getAntiVpnManager() {
        return antiVpnManager;
    }

    public SessionLocationManager getSessionLocationManager() {
        return sessionLocationManager;
    }

    public OpLockManager getOpLockManager() {
        return opLockManager;
    }
}
