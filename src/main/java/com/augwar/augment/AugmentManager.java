package com.augwar.augment;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AugmentManager {

    private final AugWar plugin;
    private BukkitRunnable tickTask;

    public AugmentManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void applyAugment(Player player, String augmentId) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null) return;

        Augment aug = plugin.getAugmentRegistry().createFresh(augmentId);
        if (aug == null) return;

        ap.addAugment(aug);
        aug.onApply(player);

        plugin.getMessageUtil().send(player, "증강 획득: " + aug.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Check synergies
        plugin.getSynergyManager().checkSynergies(player);

        // Update tab
        plugin.getTabListManager().updatePlayer(player);
    }

    public void openBonusSelection(Player player) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null) return;

        // Determine current tier based on phase
        int phase = plugin.getPhaseManager().getCurrentPhase();
        AugmentTier tier = switch (phase) {
            case 0, 1 -> AugmentTier.SILVER;
            case 2, 3 -> AugmentTier.GOLD;
            default -> AugmentTier.GOLD;
        };

        // Apply kill upgrade if available
        if (ap.hasNextTierUpgrade()) {
            tier = tier.upgrade();
            ap.setNextTierUpgrade(false);
        }

        plugin.getAugmentSelectGUI().openSelection(player, tier.name(), plugin.getPhaseManager().getCurrentPhase());
    }

    public void startTickTask() {
        if (tickTask != null) tickTask.cancel();
        tickTask = new BukkitRunnable() {
            int tickCount = 0;
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != com.augwar.game.GameState.PLAYING) return;

                for (var entry : plugin.getGameManager().getPlayerMap().entrySet()) {
                    AugPlayer ap = entry.getValue();
                    if (!ap.isAlive() || ap.isDisconnected()) continue;

                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) continue;

                    for (Augment aug : ap.getAugments()) {
                        aug.onTick(p);
                        if (tickCount % 20 == 0) {
                            aug.onSecond(p);
                        }
                    }

                    // P05 Time warp - record position every tick
                    if (ap.hasAugment("P05")) {
                        var history = ap.getPositionHistory();
                        history.addLast(new double[]{p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), p.getHealth()});
                        // Keep 3 seconds of history (60 ticks)
                        while (history.size() > 60) history.removeFirst();
                    }
                }
                tickCount++;
            }
        };
        tickTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
