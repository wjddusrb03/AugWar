package com.augwar.augment.synergy;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class SynergyManager {

    private final AugWar plugin;

    private record Synergy(String name, String aug1, String aug2, String description) {}

    private final List<Synergy> synergies = List.of(
            new Synergy("피의 군주", "G03", "G01", "흡혈량 20% -> 30%로 증가"),
            new Synergy("빙뢰", "G08", "G04", "전기 데미지에 빙결(이속 80% 감소 1초) 추가"),
            new Synergy("암살자", "G06", "G10", "투명 상태 첫 공격 데미지 1.5배")
    );

    public SynergyManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void checkSynergies(Player player) {
        if (!plugin.getConfigManager().isSynergyEnabled()) return;

        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null) return;

        for (Synergy syn : synergies) {
            boolean hasBoth = ap.hasAugment(syn.aug1) && ap.hasAugment(syn.aug2);
            if (hasBoth && !ap.getSynergies().contains(syn.name)) {
                ap.getSynergies().add(syn.name);
                plugin.getMessageUtil().send(player, "시너지 발동! <" + syn.name + "> - " + syn.description);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            } else if (!hasBoth && ap.getSynergies().contains(syn.name)) {
                // Remove synergy if augments no longer present
                ap.getSynergies().remove(syn.name);
                plugin.getMessageUtil().send(player, "시너지 해제: <" + syn.name + ">");
            }
        }
    }

    public boolean hasSynergy(Player player, String synergyName) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        return ap != null && ap.getSynergies().contains(synergyName);
    }

    // Helper for combat listener to get modified values
    public double getLifestealRate(Player player) {
        double base = plugin.getConfigManager().getAugmentDouble("G03", "lifesteal", 0.2);
        if (hasSynergy(player, "피의 군주")) return 0.30;
        return base;
    }

    public double getExecuteThreshold(Player player) {
        // 저승사자 시너지 삭제 — 기본 threshold만 사용
        return plugin.getConfigManager().getAugmentDouble("P01", "execute-threshold", 0.1);
    }

    public boolean isAssassinActive(Player player) {
        return hasSynergy(player, "암살자");
    }
}
