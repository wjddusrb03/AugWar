package com.augwar.game;

import com.augwar.AugWar;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class FreezeManager {

    private final AugWar plugin;
    private boolean frozen = false;
    private final Map<UUID, Vector> savedVelocities = new HashMap<>();
    private final Set<UUID> frozenEntities = new HashSet<>();

    public FreezeManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public boolean isFrozen() { return frozen; }

    public void freezeWorld() {
        if (frozen) return;
        frozen = true;
        plugin.getGameManager().setState(GameState.FROZEN);

        var center = plugin.getGameManager().getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        // Freeze all entities in the game world
        for (Entity entity : center.getWorld().getEntities()) {
            if (entity instanceof Player) continue; // Players handled by FreezeListener

            frozenEntities.add(entity.getUniqueId());

            if (entity instanceof LivingEntity living) {
                living.setAI(false);
                living.setGravity(false);
            }

            if (entity instanceof Projectile || entity instanceof Item || entity instanceof FallingBlock) {
                savedVelocities.put(entity.getUniqueId(), entity.getVelocity().clone());
                entity.setVelocity(new Vector(0, 0, 0));
                entity.setGravity(false);
            }

            if (entity instanceof TNTPrimed tnt) {
                savedVelocities.put(entity.getUniqueId(), new Vector(tnt.getFuseTicks(), 0, 0));
            }
        }
    }

    public void unfreezeWorld() {
        if (!frozen) return;
        frozen = false;
        // Only restore to PLAYING if game is still in FROZEN state (not ENDING/WAITING)
        if (plugin.getGameManager().getState() == GameState.FROZEN) {
            plugin.getGameManager().setState(GameState.PLAYING);
        }

        var center = plugin.getGameManager().getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        for (Entity entity : center.getWorld().getEntities()) {
            if (entity instanceof Player) continue;
            if (!frozenEntities.contains(entity.getUniqueId())) continue;

            if (entity instanceof LivingEntity living) {
                if (!(living instanceof Player)) {
                    living.setAI(true);
                    living.setGravity(true);
                }
            }

            Vector saved = savedVelocities.get(entity.getUniqueId());
            if (saved != null) {
                if (entity instanceof TNTPrimed tnt) {
                    tnt.setFuseTicks((int) saved.getX());
                } else {
                    entity.setGravity(true);
                    entity.setVelocity(saved);
                }
            }
        }

        savedVelocities.clear();
        frozenEntities.clear();
    }

    /**
     * Freeze, open GUI for all, wait for selections, unfreeze.
     */
    public void startAugmentSelection(String tierId, int tierIndex) {
        freezeWorld();

        int timeout = plugin.getConfigManager().getGuiTimeout();

        // Show title and open GUI for alive players
        for (Player p : plugin.getGameManager().getAlivePlayers()) {
            plugin.getMessageUtil().sendTitle(p, "증강 선택", timeout + "초 안에 선택하세요", 5, 40, 10);
            plugin.getAugmentSelectGUI().openSelection(p, tierId, tierIndex);
        }

        // Countdown timer
        new BukkitRunnable() {
            int remaining = timeout;
            @Override
            public void run() {
                if (!frozen) { cancel(); return; }
                GameState gs = plugin.getGameManager().getState();
                if (gs == GameState.ENDING || gs == GameState.WAITING) { cancel(); return; }

                if (remaining <= 0) {
                    // Auto-select for players who haven't chosen
                    plugin.getAugmentSelectGUI().autoSelectAll();
                    cancel();

                    // Small delay then unfreeze
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            GameState s = plugin.getGameManager().getState();
                            if (s == GameState.ENDING || s == GameState.WAITING) return;
                            unfreezeWorld();
                            plugin.getMessageUtil().broadcast("전원 선택 완료. 게임 재개.");
                        }
                    }.runTaskLater(plugin, 10L);
                    return;
                }

                // Show remaining time in action bar
                for (Player p : plugin.getGameManager().getAlivePlayers()) {
                    plugin.getMessageUtil().sendActionBar(p, "[AugWar] 선택 시간: " + remaining + "초");
                }

                // Check if all selected
                if (plugin.getAugmentSelectGUI().allSelected()) {
                    cancel();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            GameState s = plugin.getGameManager().getState();
                            if (s == GameState.ENDING || s == GameState.WAITING) return;
                            unfreezeWorld();
                            plugin.getMessageUtil().broadcast("전원 선택 완료. 게임 재개.");
                        }
                    }.runTaskLater(plugin, 10L);
                    return;
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
