package com.augwar.augment.prism;

import com.augwar.augment.Augment;
import com.augwar.augment.AugmentTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

// P01: 사신의 낫 - 체력 20% 이하 즉사 (handled in listener)
class P01 extends Augment {
    public P01() { super("P01", "사신의 낫", AugmentTier.PRISM, "적 체력 10% 이하 시 강력한 처형 일격 (15데미지)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P02: 불멸의 전사 - 1회 부활 (handled in GameManager)
class P02 extends Augment {
    public P02() { super("P02", "불멸의 전사", AugmentTier.PRISM, "사망 시 1회 부활 (풀 체력, 10초 무적)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P03: 천둥의 분노 - 공격 시 번개 (handled in listener)
class P03 extends Augment {
    public P03() { super("P03", "천둥의 분노", AugmentTier.PRISM, "적을 공격할 때마다 20% 확률로 번개 소환 (6하트)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P04: 도플갱어 - 킬 시 적 증강 복사 (handled in GameManager)
class P04 extends Augment {
    public P04() { super("P04", "도플갱어", AugmentTier.PRISM, "마지막으로 처치한 적의 증강 1개를 복사하여 획득"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P05: 시간 왜곡 - 피격 시 되돌림 (handled in listener)
class P05 extends Augment {
    public P05() { super("P05", "시간 왜곡", AugmentTier.PRISM, "피격 시 30% 확률로 3초 전 위치+체력으로 되돌림 (쿨타임 45초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P06: 엔더 지배자 - 엔더펄 무한 (listener handles throw/teleport)
class P06 extends Augment {
    public P06() { super("P06", "엔더 지배자", AugmentTier.PRISM, "엔더펄 무한 사용 (쿨타임 15초), 순간이동 직후 주변 적 2하트"); }
    @Override public void onApply(Player p) {
        // Give initial ender pearl
        boolean hasEP = false;
        for (var item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL) { hasEP = true; break; }
        }
        if (!hasEP) p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ENDER_PEARL, 1));
    }
    @Override public void onRemove(Player p) {}
    @Override public void onSecond(Player p) {
        // Ensure player always has an ender pearl
        boolean hasEP = false;
        for (var item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL) { hasEP = true; break; }
        }
        if (!hasEP) p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ENDER_PEARL, 1));
    }
}

// P07: 생명력 폭발 - 회복 시 광역 데미지 (handled in listener)
class P07 extends Augment {
    public P07() { super("P07", "생명력 폭발", AugmentTier.PRISM, "체력이 회복될 때마다 주변 5블록 적에게 회복량만큼 데미지 (쿨타임 2초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P08: 전장의 안개 - 주기적 실명 안개
class P08 extends Augment {
    public P08() { super("P08", "전장의 안개", AugmentTier.PRISM, "60초마다 주변 8블록에 실명 안개 5초 (자신은 면역)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
    @Override public void onSecond(Player p) {
        if (!isOnCooldown("P08_fog", 60)) {
            setCooldown("P08_fog");
            for (Entity nearby : p.getNearbyEntities(8, 8, 8)) {
                if (nearby instanceof Player target && !target.equals(p)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, true));
                }
            }
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
        }
    }
}

// P09: 강화 갑옷 - 방어력 +4 상시 + 갑옷 자동 수리
class P09 extends Augment {
    private static final java.util.UUID MOD_UUID = java.util.UUID.fromString("a3000009-0000-0000-0000-000000000009");
    public P09() { super("P09", "강화 갑옷", AugmentTier.PRISM, "방어력 +4 상시 적용 + 갑옷 내구도 자동 회복"); }
    @Override public void onApply(Player p) {
        var attr = p.getAttribute(org.bukkit.attribute.Attribute.ARMOR);
        if (attr != null) attr.addModifier(new org.bukkit.attribute.AttributeModifier(MOD_UUID, "augwar_p09", 4, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER));
    }
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(org.bukkit.attribute.Attribute.ARMOR);
        if (attr != null) attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
    }
    @Override public void onSecond(Player p) {
        for (var item : p.getInventory().getArmorContents()) {
            if (item != null && item.getType().getMaxDurability() > 0) {
                var meta = item.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                    if (dmg.getDamage() > 0) {
                        dmg.setDamage(Math.max(0, dmg.getDamage() - 1));
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }
}

// P10: 증강 강탈 - 킬 시 증강 빼앗기 (handled in GameManager)
class P10 extends Augment {
    public P10() { super("P10", "증강 강탈", AugmentTier.PRISM, "적 처치 시 상대의 증강 중 1개를 빼앗아 자신에게 추가"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P11: 영역 전개 - 액티브 결계 (handled in AugmentListener via sneak+left-click)
class P11 extends Augment {
    public P11() { super("P11", "영역 전개", AugmentTier.PRISM, "웅크리기+공격: 반경 10블록 결계, 적 둔화 II + 초당 1하트 (8초, 쿨타임 90초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// P12: 최후의 저항 - 저체력 시 증강 2배 (handled in listener)
class P12 extends Augment {
    public P12() { super("P12", "최후의 저항", AugmentTier.PRISM, "체력 30% 이하에서 공격 데미지 30% 증가"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

public class PrismAugments {
    public static Augment create(String id) {
        return switch (id) {
            case "P01" -> new P01();
            case "P02" -> new P02();
            case "P03" -> new P03();
            case "P04" -> new P04();
            case "P05" -> new P05();
            case "P06" -> new P06();
            case "P07" -> new P07();
            case "P08" -> new P08();
            case "P09" -> new P09();
            case "P10" -> new P10();
            case "P11" -> new P11();
            case "P12" -> new P12();
            default -> null;
        };
    }
}
