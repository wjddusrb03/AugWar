package com.augwar.game;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DisconnectManager {

    private final AugWar plugin;
    private final Map<UUID, BukkitRunnable> disconnectTimers = new HashMap<>();

    public DisconnectManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void handleDisconnect(Player player) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null || !ap.isAlive()) return;

        ap.setDisconnected(true);
        ap.setDisconnectTime(System.currentTimeMillis());

        int timeout = plugin.getConfigManager().getDisconnectTimeout();
        plugin.getMessageUtil().broadcast(player.getName() + "님의 접속이 끊어졌습니다. " + timeout + "초 대기.");

        // If in augment selection, auto-select
        plugin.getAugmentSelectGUI().forceAutoSelect(player.getUniqueId());

        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                AugPlayer ap2 = plugin.getGameManager().getAugPlayer(player.getUniqueId());
                if (ap2 == null) return;

                if (ap2.isDisconnected()) {
                    // Timed out - eliminate
                    ap2.setAlive(false);
                    plugin.getMessageUtil().broadcast(player.getName() + "님이 탈락 처리되었습니다. (접속 시간 초과)");

                    if (plugin.getConfigManager().isDropItemsOnTimeout()) {
                        // Items will be dropped when inventory is accessed
                    }

                    plugin.getGameManager().checkWinCondition();
                }
                disconnectTimers.remove(player.getUniqueId());
            }
        };
        timer.runTaskLater(plugin, timeout * 20L);
        disconnectTimers.put(player.getUniqueId(), timer);
    }

    public void handleReconnect(Player player) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null) return;

        if (ap.isDisconnected()) {
            ap.setDisconnected(false);
            plugin.getMessageUtil().broadcast(player.getName() + "님이 복귀했습니다.");

            BukkitRunnable timer = disconnectTimers.remove(player.getUniqueId());
            if (timer != null) timer.cancel();

            // Re-apply augment effects
            for (var aug : ap.getAugments()) {
                aug.onApply(player);
            }
        }
    }

    public void cancelAll() {
        for (BukkitRunnable timer : disconnectTimers.values()) {
            timer.cancel();
        }
        disconnectTimers.clear();
    }
}
