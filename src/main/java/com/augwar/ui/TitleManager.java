package com.augwar.ui;

import com.augwar.AugWar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleManager {

    private final AugWar plugin;

    public TitleManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void showWin(String winnerName) {
        Title title = Title.title(
                Component.text("게임 종료").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("우승: " + winnerName).color(NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );
        for (Player p : plugin.getGameManager().getAllPlayers()) {
            p.showTitle(title);
        }
    }

    public void showAugmentSelect(int timeout) {
        Title title = Title.title(
                Component.text("증강 선택").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
                Component.text(timeout + "초 안에 선택하세요").color(NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))
        );
        for (Player p : plugin.getGameManager().getAlivePlayers()) {
            p.showTitle(title);
        }
    }
}
