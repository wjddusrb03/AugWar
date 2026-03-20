package com.augwar.ui;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final AugWar plugin;
    private BukkitRunnable updateTask;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public ScoreboardManager(AugWar plugin) {
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
    }

    private void updateAll() {
        for (Player p : plugin.getGameManager().getAllPlayers()) {
            updatePlayer(p);
        }
    }

    private void updatePlayer(Player p) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null) return;

        // Reuse existing scoreboard or create new one
        Scoreboard board = playerBoards.get(p.getUniqueId());
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            playerBoards.put(p.getUniqueId(), board);
        }

        // Clear old objective and create new (entries can't be reliably updated in-place)
        Objective old = board.getObjective("augwar");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective("augwar", Criteria.DUMMY, Component.text("AugWar").color(NamedTextColor.LIGHT_PURPLE));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int seconds = plugin.getGameManager().getGameTimeSeconds();
        String time = plugin.getMessageUtil().formatTime(seconds);
        int alive = plugin.getGameManager().getAliveCount();

        int line = 10;
        setScore(obj, "§7-----------", line--);
        setScore(obj, "§f경과: §a" + time, line--);
        setScore(obj, "§f생존: §c" + alive + "명", line--);
        setScore(obj, "§7-----------", line--);

        int nextAug = plugin.getPhaseManager().getNextAugmentSeconds();
        if (nextAug > 0) {
            setScore(obj, "§f다음 증강: §e" + plugin.getMessageUtil().formatTime(nextAug), line--);
        } else {
            int finalTime = plugin.getConfigManager().getBorderFinalTime() - seconds;
            if (finalTime > 0) {
                setScore(obj, "§f최종 자기장: §e" + plugin.getMessageUtil().formatTime(finalTime), line--);
            } else {
                setScore(obj, "§c최후의 결전!", line--);
            }
        }

        setScore(obj, "§7-----------  ", line--);
        setScore(obj, "§f처치: §c" + ap.getKills() + "킬", line--);
        setScore(obj, "§f데미지: §e" + (int) ap.getDamageDealt(), line--);
        setScore(obj, "§7----------- ", line--);

        p.setScoreboard(board);
    }

    private void setScore(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        playerBoards.clear();
    }
}
