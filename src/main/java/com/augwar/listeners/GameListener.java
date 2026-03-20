package com.augwar.listeners;

import com.augwar.AugWar;
import com.augwar.game.GameState;
import com.augwar.player.AugPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameListener implements Listener {

    private final AugWar plugin;

    public GameListener(AugWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        GameState gs = plugin.getGameManager().getState();
        if (gs == GameState.PLAYING || gs == GameState.FROZEN) {
            AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
            if (ap != null && ap.isDisconnected()) {
                plugin.getDisconnectManager().handleReconnect(p);
            } else if (ap != null && !ap.isAlive()) {
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
            } else if (ap == null) {
                // New player joining during game - force spectator
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
                p.sendMessage("[AugWar] 게임이 진행 중입니다. 관전 모드로 전환됩니다.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.getGameManager().getState() == GameState.PLAYING || plugin.getGameManager().getState() == GameState.FROZEN) {
            plugin.getDisconnectManager().handleDisconnect(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;

        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        e.setKeepInventory(false);
        e.setKeepLevel(false);

        plugin.getGameManager().handlePlayerDeath(victim, killer);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING && plugin.getGameManager().getState() != GameState.ENDING) return;
        AugPlayer ap = plugin.getGameManager().getAugPlayer(e.getPlayer());
        if (ap != null && !ap.isAlive()) {
            e.getPlayer().setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }
}
