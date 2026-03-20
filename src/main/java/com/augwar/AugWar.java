package com.augwar;

import com.augwar.commands.AugWarCommand;
import com.augwar.commands.AugWarTabCompleter;
import com.augwar.game.*;
import com.augwar.augment.AugmentManager;
import com.augwar.augment.AugmentRegistry;
import com.augwar.augment.synergy.SynergyManager;
import com.augwar.gui.AugmentSelectGUI;
import com.augwar.gui.AugmentCheckGUI;
import com.augwar.player.StatsManager;
import com.augwar.listeners.*;
import com.augwar.ui.*;
import com.augwar.util.ConfigManager;
import com.augwar.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class AugWar extends JavaPlugin {

    private static AugWar instance;
    private ConfigManager configManager;
    private MessageUtil messageUtil;
    private GameManager gameManager;
    private PhaseManager phaseManager;
    private FreezeManager freezeManager;
    private DisconnectManager disconnectManager;
    private TeamManager teamManager;
    private AugmentRegistry augmentRegistry;
    private AugmentManager augmentManager;
    private SynergyManager synergyManager;
    private AugmentSelectGUI augmentSelectGUI;
    private AugmentCheckGUI augmentCheckGUI;
    private StatsManager statsManager;
    private ScoreboardManager scoreboardManager;
    private BossBarManager bossBarManager;
    private TabListManager tabListManager;
    private TitleManager titleManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        messageUtil = new MessageUtil(this);

        augmentRegistry = new AugmentRegistry(this);
        augmentManager = new AugmentManager(this);
        synergyManager = new SynergyManager(this);

        statsManager = new StatsManager(this);
        teamManager = new TeamManager(this);
        freezeManager = new FreezeManager(this);
        disconnectManager = new DisconnectManager(this);
        gameManager = new GameManager(this);
        phaseManager = new PhaseManager(this);

        augmentSelectGUI = new AugmentSelectGUI(this);
        augmentCheckGUI = new AugmentCheckGUI(this);

        scoreboardManager = new ScoreboardManager(this);
        bossBarManager = new BossBarManager(this);
        tabListManager = new TabListManager(this);
        titleManager = new TitleManager(this);

        AugWarCommand cmd = new AugWarCommand(this);
        getCommand("aw").setExecutor(cmd);
        getCommand("aw").setTabCompleter(new AugWarTabCompleter(this));

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new AugmentListener(this), this);

        getLogger().info("=============================");
        getLogger().info("  AugWar - 증강전쟁 v" + getDescription().getVersion());
        getLogger().info("  Made by JG");
        getLogger().info("=============================");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.getState() != GameState.WAITING) {
            gameManager.forceStop();
        }
        if (statsManager != null) {
            statsManager.saveAll();
        }
        getLogger().info("AugWar 비활성화 완료.");
    }

    public static AugWar getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageUtil getMessageUtil() { return messageUtil; }
    public GameManager getGameManager() { return gameManager; }
    public PhaseManager getPhaseManager() { return phaseManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public DisconnectManager getDisconnectManager() { return disconnectManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public AugmentRegistry getAugmentRegistry() { return augmentRegistry; }
    public AugmentManager getAugmentManager() { return augmentManager; }
    public SynergyManager getSynergyManager() { return synergyManager; }
    public AugmentSelectGUI getAugmentSelectGUI() { return augmentSelectGUI; }
    public AugmentCheckGUI getAugmentCheckGUI() { return augmentCheckGUI; }
    public StatsManager getStatsManager() { return statsManager; }
    public ScoreboardManager getScoreboardMgr() { return scoreboardManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public TabListManager getTabListManager() { return tabListManager; }
    public TitleManager getTitleManager() { return titleManager; }
}
