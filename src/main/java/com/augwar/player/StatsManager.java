package com.augwar.player;

import com.augwar.AugWar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class StatsManager {

    private final AugWar plugin;
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(AugWar plugin) {
        this.plugin = plugin;
        loadStats();
    }

    private void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "player_stats.yml");
        if (!statsFile.exists()) {
            try { statsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void recordGame(AugPlayer ap) {
        String path = "players." + ap.getUuid().toString();
        statsConfig.set(path + ".name", ap.getName());
        statsConfig.set(path + ".games-played", statsConfig.getInt(path + ".games-played", 0) + 1);
        statsConfig.set(path + ".kills", statsConfig.getInt(path + ".kills", 0) + ap.getKills());
        statsConfig.set(path + ".deaths", statsConfig.getInt(path + ".deaths", 0) + (ap.isAlive() ? 0 : 1));
        statsConfig.set(path + ".total-damage-dealt", statsConfig.getDouble(path + ".total-damage-dealt", 0) + ap.getDamageDealt());
        statsConfig.set(path + ".total-damage-taken", statsConfig.getDouble(path + ".total-damage-taken", 0) + ap.getDamageTaken());
        statsConfig.set(path + ".total-survival-time", statsConfig.getInt(path + ".total-survival-time", 0) + ap.getSurvivalSeconds());

        int killStreak = ap.getKills();
        if (killStreak > statsConfig.getInt(path + ".best-kill-streak", 0)) {
            statsConfig.set(path + ".best-kill-streak", killStreak);
        }
        // Save after recording to prevent data loss on crash
        saveAll();
    }

    public void recordWin(AugPlayer ap) {
        String path = "players." + ap.getUuid().toString();
        statsConfig.set(path + ".wins", statsConfig.getInt(path + ".wins", 0) + 1);
        // Save immediately after recording to prevent data loss on crash
        saveAll();
    }

    public String getStatsDisplay(String uuid) {
        String path = "players." + uuid;
        if (!statsConfig.contains(path)) return "전적 기록이 없습니다.";
        String name = statsConfig.getString(path + ".name", "???");
        int games = statsConfig.getInt(path + ".games-played", 0);
        int wins = statsConfig.getInt(path + ".wins", 0);
        int kills = statsConfig.getInt(path + ".kills", 0);
        int deaths = statsConfig.getInt(path + ".deaths", 0);
        int streak = statsConfig.getInt(path + ".best-kill-streak", 0);
        return name + " | 게임: " + games + " | 승리: " + wins + " | 킬: " + kills + " | 데스: " + deaths + " | 최고 연속킬: " + streak;
    }

    public void saveAll() {
        try { statsConfig.save(statsFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
