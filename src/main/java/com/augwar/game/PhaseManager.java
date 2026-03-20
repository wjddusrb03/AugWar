package com.augwar.game;

import com.augwar.AugWar;
import com.augwar.augment.AugmentTier;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class PhaseManager {

    private final AugWar plugin;
    private BukkitRunnable phaseTask;
    private final List<BukkitRunnable> borderTasks = new ArrayList<>();
    private int currentPhase = 0;
    private boolean[] phaseTriggered = new boolean[6];

    public PhaseManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public int getCurrentPhase() { return currentPhase; }

    public void start() {
        currentPhase = 0;
        phaseTriggered = new boolean[6];

        phaseTask = new BukkitRunnable() {
            @Override
            public void run() {
                GameState gs = plugin.getGameManager().getState();
                if (gs == GameState.ENDING || gs == GameState.WAITING) { cancel(); return; }
                if (gs == GameState.FROZEN) return; // Skip during freeze

                int seconds = plugin.getGameManager().getGameTimeSeconds();
                checkPhases(seconds);
            }
        };
        phaseTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkPhases(int seconds) {
        var cfg = plugin.getConfigManager();

        // Phase 1: 1st augment (3 min)
        if (!phaseTriggered[0] && seconds >= cfg.getAugment1st()) {
            phaseTriggered[0] = true;
            currentPhase = 1;
            plugin.getFreezeManager().startAugmentSelection("SILVER", 1);
        }

        // Phase 2: 2nd augment (6 min)
        if (!phaseTriggered[1] && seconds >= cfg.getAugment2nd()) {
            phaseTriggered[1] = true;
            currentPhase = 2;
            plugin.getFreezeManager().startAugmentSelection("GOLD", 2);
        }

        // Phase 3: 3rd augment + border phase 1 (9 min)
        if (!phaseTriggered[2] && seconds >= cfg.getAugment3rd()) {
            phaseTriggered[2] = true;
            currentPhase = 3;
            plugin.getFreezeManager().startAugmentSelection("GOLD", 3);
            // Start border shrink after freeze ends
            BukkitRunnable borderTask = new BukkitRunnable() {
                @Override
                public void run() {
                    GameState gs = plugin.getGameManager().getState();
                    if (gs == GameState.ENDING || gs == GameState.WAITING) { cancel(); return; }
                    if (gs != GameState.FROZEN) {
                        shrinkBorder(cfg.getBorderPhase1(), cfg.getBorderShrinkTime(), cfg.getBorderDamagePhase1());
                        cancel();
                    }
                }
            };
            borderTask.runTaskTimer(plugin, 20L, 20L);
            borderTasks.add(borderTask);
        }

        // Phase 4: 4th augment + border phase 2 (12 min)
        if (!phaseTriggered[3] && seconds >= cfg.getAugment4th()) {
            phaseTriggered[3] = true;
            currentPhase = 4;
            plugin.getFreezeManager().startAugmentSelection("PRISM", 4);
            BukkitRunnable borderTask = new BukkitRunnable() {
                @Override
                public void run() {
                    GameState gs = plugin.getGameManager().getState();
                    if (gs == GameState.ENDING || gs == GameState.WAITING) { cancel(); return; }
                    if (gs != GameState.FROZEN) {
                        shrinkBorder(cfg.getBorderPhase2(), cfg.getBorderShrinkTime(), cfg.getBorderDamagePhase2());
                        cancel();
                    }
                }
            };
            borderTask.runTaskTimer(plugin, 20L, 20L);
            borderTasks.add(borderTask);
        }

        // Phase 5: Final border (15 min)
        if (!phaseTriggered[4] && seconds >= cfg.getBorderFinalTime()) {
            phaseTriggered[4] = true;
            currentPhase = 5;
            shrinkBorder(cfg.getBorderFinal(), cfg.getBorderShrinkTime(), cfg.getBorderDamageFinal());
            plugin.getMessageUtil().broadcast("최종 자기장이 축소됩니다!");
        }
    }

    private void shrinkBorder(int targetRadius, int shrinkTime, double damage) {
        var center = plugin.getGameManager().getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        WorldBorder border = center.getWorld().getWorldBorder();
        border.setSize(targetRadius * 2, shrinkTime);
        border.setDamageAmount(damage);
        border.setDamageBuffer(0);
        border.setWarningDistance(10);
        border.setWarningTime(15);

        plugin.getMessageUtil().broadcast("자기장 축소 중 - " + shrinkTime + "초 후 완료");
    }

    public void skip() {
        // Find next untriggered phase and trigger it
        var cfg = plugin.getConfigManager();
        for (int i = 0; i < phaseTriggered.length; i++) {
            if (!phaseTriggered[i]) {
                phaseTriggered[i] = true;
                currentPhase = i + 1;
                switch (i) {
                    case 0 -> plugin.getFreezeManager().startAugmentSelection("SILVER", 1);
                    case 1 -> plugin.getFreezeManager().startAugmentSelection("GOLD", 2);
                    case 2 -> {
                        plugin.getFreezeManager().startAugmentSelection("GOLD", 3);
                        shrinkBorder(cfg.getBorderPhase1(), 10, cfg.getBorderDamagePhase1());
                    }
                    case 3 -> {
                        plugin.getFreezeManager().startAugmentSelection("PRISM", 4);
                        shrinkBorder(cfg.getBorderPhase2(), 10, cfg.getBorderDamagePhase2());
                    }
                    case 4 -> shrinkBorder(cfg.getBorderFinal(), 10, cfg.getBorderDamageFinal());
                }
                break;
            }
        }
    }

    public int getNextAugmentSeconds() {
        var cfg = plugin.getConfigManager();
        int seconds = plugin.getGameManager().getGameTimeSeconds();
        if (!phaseTriggered[0]) return cfg.getAugment1st() - seconds;
        if (!phaseTriggered[1]) return cfg.getAugment2nd() - seconds;
        if (!phaseTriggered[2]) return cfg.getAugment3rd() - seconds;
        if (!phaseTriggered[3]) return cfg.getAugment4th() - seconds;
        if (!phaseTriggered[4]) return cfg.getBorderFinalTime() - seconds;
        return -1;
    }

    public void stop() {
        if (phaseTask != null) phaseTask.cancel();
        for (BukkitRunnable task : borderTasks) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        borderTasks.clear();
    }
}
