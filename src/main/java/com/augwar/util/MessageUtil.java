package com.augwar.util;

import com.augwar.AugWar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.Collection;

public class MessageUtil {

    private final AugWar plugin;

    public MessageUtil(AugWar plugin) {
        this.plugin = plugin;
    }

    private String getPrefix() {
        return plugin.getConfigManager().getPrefix();
    }

    public void send(Player player, String message) {
        player.sendMessage(Component.text(getPrefix() + " " + message));
    }

    public void broadcast(String message) {
        for (Player p : getGamePlayers()) {
            send(p, message);
        }
    }

    public void broadcastAll(String message) {
        Bukkit.broadcast(Component.text(getPrefix() + " " + message));
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                Component.text(title).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
                Component.text(subtitle).color(NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player p : getGamePlayers()) {
            sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void sendCredits() {
        String line = "=============================";
        String credit1 = plugin.getConfig().getString("messages.credit-line1", "AugWar - 증강전쟁");
        String credit2 = plugin.getConfig().getString("messages.credit-line2", "Made by JG");
        String credit3 = plugin.getConfig().getString("messages.credit-line3", "당신의 전략이 승리를 결정합니다.");

        for (Player p : getGamePlayers()) {
            p.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
            p.sendMessage(Component.text("  " + credit1).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
            p.sendMessage(Component.text("  " + credit2).color(NamedTextColor.GOLD));
            p.sendMessage(Component.text("  " + credit3).color(NamedTextColor.WHITE));
            p.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
        }

        broadcastTitle("AugWar", "Made by JG", 10, 60, 20);
    }

    public void sendWinMessage(String winner, int kills, int survivalSeconds, String augments) {
        String line = "=============================";
        int min = survivalSeconds / 60;
        int sec = survivalSeconds % 60;

        for (Player p : getGamePlayers()) {
            p.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
            p.sendMessage(Component.text("  AugWar - 게임 종료").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
            p.sendMessage(Component.text("  우승: " + winner).color(NamedTextColor.GOLD));
            p.sendMessage(Component.text("  처치: " + kills + "킬 | 생존: " + min + "분 " + sec + "초").color(NamedTextColor.WHITE));
            p.sendMessage(Component.text("  보유 증강: " + augments).color(NamedTextColor.WHITE));
            p.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
        }
    }

    public String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private Collection<? extends Player> getGamePlayers() {
        if (plugin.getGameManager() != null) {
            return plugin.getGameManager().getAllPlayers();
        }
        return Bukkit.getOnlinePlayers();
    }
}
