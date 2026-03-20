package com.augwar.util;

import com.augwar.AugWar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    private final AugWar plugin;
    private FileConfiguration augmentsConfig;
    private File augmentsFile;

    public ConfigManager(AugWar plugin) {
        this.plugin = plugin;
        loadAugmentsConfig();
    }

    private void loadAugmentsConfig() {
        augmentsFile = new File(plugin.getDataFolder(), "augments.yml");
        if (!augmentsFile.exists()) {
            plugin.saveResource("augments.yml", false);
        }
        augmentsConfig = YamlConfiguration.loadConfiguration(augmentsFile);
    }

    public void reload() {
        plugin.reloadConfig();
        loadAugmentsConfig();
    }

    public FileConfiguration getConfig() { return plugin.getConfig(); }
    public FileConfiguration getAugmentsConfig() { return augmentsConfig; }

    // Game settings
    public int getMinPlayers() { return getConfig().getInt("game.min-players", 4); }
    public int getMaxPlayers() { return getConfig().getInt("game.max-players", 16); }
    public String getGameMode() { return getConfig().getString("game.mode", "solo"); }
    public int getTeamSize() { return getConfig().getInt("game.team-size", 2); }

    // World settings
    public int getBorderInitial() { return getConfig().getInt("world.border-initial", 300); }
    public int getBorderPhase1() { return getConfig().getInt("world.border-phase1", 200); }
    public int getBorderPhase2() { return getConfig().getInt("world.border-phase2", 80); }
    public int getBorderFinal() { return getConfig().getInt("world.border-final", 25); }
    public int getBorderShrinkTime() { return getConfig().getInt("world.border-shrink-time", 60); }
    public double getBorderDamagePhase1() { return getConfig().getDouble("world.border-damage-phase1", 1.0); }
    public double getBorderDamagePhase2() { return getConfig().getDouble("world.border-damage-phase2", 1.5); }
    public double getBorderDamageFinal() { return getConfig().getDouble("world.border-damage-final", 2.0); }

    // Timing
    public int getCountdown() { return getConfig().getInt("timing.countdown", 30); }
    public int getAugment1st() { return getConfig().getInt("timing.augment-1st", 180); }
    public int getAugment2nd() { return getConfig().getInt("timing.augment-2nd", 360); }
    public int getAugment3rd() { return getConfig().getInt("timing.augment-3rd", 540); }
    public int getAugment4th() { return getConfig().getInt("timing.augment-4th", 720); }
    public int getBorderFinalTime() { return getConfig().getInt("timing.border-final-time", 900); }

    // Augment settings
    public int getGuiTimeout() { return getConfig().getInt("augments.gui-timeout", 5); }
    public int getCandidates() { return getConfig().getInt("augments.candidates", 3); }
    public boolean isKillUpgrade() { return getConfig().getBoolean("augments.kill-upgrade", true); }
    public boolean isKillStreakBonus() { return getConfig().getBoolean("augments.kill-streak-bonus", true); }
    public boolean isSynergyEnabled() { return getConfig().getBoolean("augments.synergy-enabled", true); }
    public List<String> getDisabledAugments() { return getConfig().getStringList("augments.disabled"); }

    // Anti-camp
    public boolean isAntiCampEnabled() { return getConfig().getBoolean("anti-camp.enabled", true); }
    public int getAntiCampStartAfter() { return getConfig().getInt("anti-camp.start-after", 180); }
    public int getAntiCampIdleTime() { return getConfig().getInt("anti-camp.idle-time", 240); }

    // Disconnect
    public int getDisconnectTimeout() { return getConfig().getInt("disconnect.timeout", 60); }
    public boolean isDropItemsOnTimeout() { return getConfig().getBoolean("disconnect.drop-items", true); }

    // Messages
    public String getPrefix() { return getConfig().getString("messages.prefix", "[AugWar]"); }

    // Augment-specific config
    public boolean isAugmentEnabled(String id) {
        return augmentsConfig.getBoolean("augments." + id + ".enabled", true);
    }

    public double getAugmentDouble(String id, String key, double def) {
        return augmentsConfig.getDouble("augments." + id + "." + key, def);
    }

    public int getAugmentInt(String id, String key, int def) {
        return augmentsConfig.getInt("augments." + id + "." + key, def);
    }

    public boolean getAugmentBool(String id, String key, boolean def) {
        return augmentsConfig.getBoolean("augments." + id + "." + key, def);
    }
}
