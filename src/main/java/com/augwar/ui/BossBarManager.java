package com.augwar.ui;

import com.augwar.AugWar;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BossBarManager {

    private final AugWar plugin;
    private BossBar mainBar;
    private BossBar borderBar;
    private BukkitRunnable updateTask;
    private BukkitRunnable borderTask;

    public BossBarManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        mainBar = BossBar.bossBar(Component.text("AugWar"), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        borderBar = BossBar.bossBar(Component.text(""), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        for (Player p : plugin.getGameManager().getAllPlayers()) {
            p.showBossBar(mainBar);
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void update() {
        int seconds = plugin.getGameManager().getGameTimeSeconds();
        int alive = plugin.getGameManager().getAliveCount();
        int total = plugin.getGameManager().getTotalCount();

        int nextAug = plugin.getPhaseManager().getNextAugmentSeconds();
        String timeStr;
        if (nextAug > 0) {
            timeStr = "다음 증강까지: " + plugin.getMessageUtil().formatTime(nextAug);
        } else {
            int finalTime = plugin.getConfigManager().getBorderFinalTime() - seconds;
            if (finalTime > 0) {
                timeStr = "최종 자기장까지: " + plugin.getMessageUtil().formatTime(finalTime);
            } else {
                timeStr = "최후의 결전";
            }
        }

        mainBar.name(Component.text(timeStr + " | 생존: " + alive + "명"));
        float progress = Math.max(0f, Math.min(1f, (float) alive / Math.max(1, total)));
        mainBar.progress(progress);
    }

    public void showBorderShrink(int seconds) {
        // Cancel previous border task if running
        if (borderTask != null) {
            try { borderTask.cancel(); } catch (Exception ignored) {}
        }

        borderBar.name(Component.text("자기장 축소 중 - " + seconds + "초 후 완료"));
        for (Player p : plugin.getGameManager().getAllPlayers()) {
            p.showBossBar(borderBar);
        }

        final int totalSeconds = seconds;
        borderTask = new BukkitRunnable() {
            int remaining = totalSeconds;
            @Override
            public void run() {
                if (remaining <= 0) {
                    for (Player p : plugin.getGameManager().getAllPlayers()) {
                        p.hideBossBar(borderBar);
                    }
                    cancel();
                    borderTask = null;
                    return;
                }
                borderBar.name(Component.text("자기장 축소 중 - " + remaining + "초 후 완료"));
                borderBar.progress(Math.max(0f, (float) remaining / totalSeconds));
                remaining--;
            }
        };
        borderTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopUpdating() {
        if (updateTask != null) updateTask.cancel();
        if (borderTask != null) {
            try { borderTask.cancel(); } catch (Exception ignored) {}
            borderTask = null;
        }
    }

    public void removeAll() {
        if (mainBar != null || borderBar != null) {
            // Remove from all online players (not just game players)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (mainBar != null) p.hideBossBar(mainBar);
                if (borderBar != null) p.hideBossBar(borderBar);
            }
        }
    }
}
