package com.augwar.ui;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TabListManager {

    private final AugWar plugin;
    private BukkitRunnable updateTask;

    public TabListManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAll();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopUpdating() {
        if (updateTask != null) updateTask.cancel();
        // Reset tab list for all players
        for (Player p : plugin.getGameManager().getAllPlayers()) {
            p.sendPlayerListHeaderAndFooter(
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.text.Component.empty()
            );
            p.playerListName(net.kyori.adventure.text.Component.text(p.getName()));
        }
    }

    private void updateAll() {
        int alive = plugin.getGameManager().getAliveCount();
        int total = plugin.getGameManager().getTotalCount();
        int seconds = plugin.getGameManager().getGameTimeSeconds();
        String time = plugin.getMessageUtil().formatTime(seconds);

        Component header = Component.text("---------- AugWar - 증강전쟁 ----------").color(NamedTextColor.LIGHT_PURPLE);
        Component footer = Component.text("생존: " + alive + "명 / " + total + "명          경과 시간: " + time).color(NamedTextColor.GRAY);

        for (Player p : plugin.getGameManager().getAllPlayers()) {
            p.sendPlayerListHeaderAndFooter(header, footer);
            updatePlayer(p);
        }
    }

    public void updatePlayer(Player p) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null) return;

        String suffix;
        if (!ap.isAlive()) {
            suffix = " §8[사망]";
        } else {
            String augDisplay = ap.getAugmentDisplayForTab();
            if (augDisplay.isEmpty()) {
                suffix = " §a[생존]";
            } else {
                suffix = " §a[생존] §7" + augDisplay;
            }
        }

        p.playerListName(Component.text(p.getName() + suffix));
    }
}
