package com.augwar.augment.silver;

import com.augwar.augment.Augment;
import com.augwar.augment.AugmentTier;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

// S01: 광부의 축복 - 채굴 속도 증가 + 채굴 시 회복
class S01 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a1000001-0000-0000-0000-000000000001");
    public S01() { super("S01", "광부의 축복", AugmentTier.SILVER, "채굴 속도 30% 증가 + 광물 채굴 시 체력 1하트 회복"); }
    @Override public void onApply(Player p) { p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, false, false, true)); }
    @Override public void onRemove(Player p) { p.removePotionEffect(PotionEffectType.HASTE); }
}

// S02: 두꺼운 가죽 - 방어력 +2
class S02 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a1000002-0000-0000-0000-000000000002");
    public S02() { super("S02", "두꺼운 가죽", AugmentTier.SILVER, "방어력 +2 상시 적용"); }
    @Override public void onApply(Player p) {
        var attr = p.getAttribute(Attribute.ARMOR);
        if (attr != null) attr.addModifier(new AttributeModifier(MOD_UUID, "augwar_s02", 2, AttributeModifier.Operation.ADD_NUMBER));
    }
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(Attribute.ARMOR);
        if (attr != null) attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
    }
}

// S03: 민첩한 발걸음 - 이동속도 10% 증가
class S03 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a1000003-0000-0000-0000-000000000003");
    public S03() { super("S03", "민첩한 발걸음", AugmentTier.SILVER, "이동속도 10% 증가"); }
    @Override public void onApply(Player p) {
        var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.addModifier(new AttributeModifier(MOD_UUID, "augwar_s03", 0.1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
    }
}

// S04: 양날의 검 - 공격력 +2, 받는 데미지 3% 증가
class S04 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a1000004-0000-0000-0000-000000000004");
    public S04() { super("S04", "양날의 검", AugmentTier.SILVER, "공격력 +2, 받는 데미지 3% 증가"); }
    @Override public void onApply(Player p) {
        var attr = p.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr != null) attr.addModifier(new AttributeModifier(MOD_UUID, "augwar_s04", 2.0, AttributeModifier.Operation.ADD_NUMBER));
    }
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr != null) attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
    }
}

// S05: 자연 치유 - 5초마다 하트 1 회복
class S05 extends Augment {
    public S05() { super("S05", "자연 치유", AugmentTier.SILVER, "5초마다 1하트 자연 회복"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
    @Override public void onSecond(Player p) {
        if (!isOnCooldown("S05_heal", 5)) {
            setCooldown("S05_heal");
            var attr = p.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = attr != null ? attr.getValue() : 20.0;
            if (p.getHealth() < maxHp) {
                p.setHealth(Math.min(maxHp, p.getHealth() + 2.0));
            }
        }
    }
}

// S06: 행운의 손 - 행운 1 효과 (handled in listener)
class S06 extends Augment {
    public S06() { super("S06", "행운의 손", AugmentTier.SILVER, "광물 드롭량 2배"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// S07: 긴급 식량 - 허기 감소 30% 감소
class S07 extends Augment {
    public S07() { super("S07", "긴급 식량", AugmentTier.SILVER, "허기 게이지 감소 속도 30% 감소"); }
    @Override public void onApply(Player p) {
        // Use low-level saturation to slow hunger instead of making it infinite
        p.setSaturation(20f);
    }
    @Override public void onRemove(Player p) {}
    @Override public void onSecond(Player p) {
        // Periodically restore some saturation to simulate slower hunger drain
        if (p.getSaturation() < 5f) {
            p.setSaturation(Math.min(20f, p.getSaturation() + 2f));
        }
    }
}

// S08: 야간 투시 - 밤에 자동 야간 투시
class S08 extends Augment {
    public S08() { super("S08", "야간 투시", AugmentTier.SILVER, "밤에 야간 투시 효과 자동 적용"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) { p.removePotionEffect(PotionEffectType.NIGHT_VISION); }
    @Override public void onSecond(Player p) {
        long time = p.getWorld().getTime();
        if (time >= 13000 && time <= 23000) {
            if (!p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, true));
            }
        }
    }
}

// S09: 가벼운 주머니 - 핫바 아이템 들면 이속 5%
class S09 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a1000009-0000-0000-0000-000000000009");
    public S09() { super("S09", "가벼운 주머니", AugmentTier.SILVER, "인벤토리 최상단 줄 아이템을 들고 있으면 이속 5% 추가"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
    }
    @Override public void onTick(Player p) {
        var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        boolean hasItem = p.getInventory().getItemInMainHand().getType() != Material.AIR;
        boolean hasMod = attr.getModifiers().stream().anyMatch(m -> m.getUniqueId().equals(MOD_UUID));
        if (hasItem && !hasMod) {
            attr.addModifier(new AttributeModifier(MOD_UUID, "augwar_s09", 0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        } else if (!hasItem && hasMod) {
            attr.getModifiers().stream().filter(m -> m.getUniqueId().equals(MOD_UUID)).findFirst().ifPresent(attr::removeModifier);
        }
    }
}

// S10: 연막탄 - 웅크리기 3초 시 주변 실명 (handled in listener)
class S10 extends Augment {
    public S10() { super("S10", "연막탄", AugmentTier.SILVER, "웅크리기 3초 유지 시 주변 3블록 실명 연막 (쿨타임 60초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// S11: 화살 절약 - 30% 확률 화살 미소모 (handled in listener)
class S11 extends Augment {
    public S11() { super("S11", "화살 절약", AugmentTier.SILVER, "화살이 30% 확률로 소모되지 않음"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// S12: 불사의 토템 파편 - 1회 생존 (handled in GameManager)
class S12 extends Augment {
    public S12() { super("S12", "불사의 토템 파편", AugmentTier.SILVER, "치명적 피해 시 1회 한정으로 하트 2개로 생존"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// Factory class for creating silver augments
public class SilverAugments {
    public static Augment create(String id) {
        return switch (id) {
            case "S01" -> new S01();
            case "S02" -> new S02();
            case "S03" -> new S03();
            case "S04" -> new S04();
            case "S05" -> new S05();
            case "S06" -> new S06();
            case "S07" -> new S07();
            case "S08" -> new S08();
            case "S09" -> new S09();
            case "S10" -> new S10();
            case "S11" -> new S11();
            case "S12" -> new S12();
            default -> null;
        };
    }
}
