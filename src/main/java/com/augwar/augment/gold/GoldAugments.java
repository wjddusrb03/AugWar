package com.augwar.augment.gold;

import com.augwar.augment.Augment;
import com.augwar.augment.AugmentTier;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

// G01: 광전사 - 체력 30% 이하 시 공격력 50% 증가 (handled in listener)
class G01 extends Augment {
    public G01() { super("G01", "광전사", AugmentTier.GOLD, "체력이 30% 이하일 때 공격력 50% 증가, 힘 I 적용"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) { p.removePotionEffect(PotionEffectType.STRENGTH); }
}

// G02: 철벽 - 웅크리기 시 데미지 40% 감소 (handled in listener)
class G02 extends Augment {
    public G02() { super("G02", "철벽", AugmentTier.GOLD, "웅크리기 시 받는 데미지 40% 감소 (이동속도 50% 감소)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G03: 뱀파이어 - 흡혈 20% (handled in listener)
class G03 extends Augment {
    public G03() { super("G03", "뱀파이어", AugmentTier.GOLD, "근접 공격 시 입힌 데미지의 20%만큼 체력 회복"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G04: 스톰 러너 - 달리기 중 주변 전기 데미지 (handled in listener)
class G04 extends Augment {
    public G04() { super("G04", "스톰 러너", AugmentTier.GOLD, "달리기 중 주변 3블록 적에게 초당 0.5하트 전기 데미지"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G05: 폭발물 전문가 - 자신 폭발 데미지 감소 (handled in listener)
class G05 extends Augment {
    public G05() { super("G05", "폭발물 전문가", AugmentTier.GOLD, "TNT 설치 쿨타임 없음, 자신은 폭발 데미지 50% 감소"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G06: 은신 달인 - 웅크리기 10초 후 투명화 (handled in listener)
class G06 extends Augment {
    public G06() { super("G06", "은신 달인", AugmentTier.GOLD, "웅크리기 시 10초 후 투명화, 공격하면 해제 (쿨타임 30초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) { p.removePotionEffect(PotionEffectType.INVISIBILITY); }
}

// G07: 화염 마법사 - 공격 시 발화 (handled in listener)
class G07 extends Augment {
    public G07() { super("G07", "화염 마법사", AugmentTier.GOLD, "칼로 공격 시 30% 확률로 발화, 활 공격은 항상 화염 화살"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G08: 서리 보행자 - 물 얼림 + 둔화 (handled in listener)
class G08 extends Augment {
    public G08() { super("G08", "서리 보행자", AugmentTier.GOLD, "걸을 때 주변 물이 얼음으로 변환, 근접 공격에 둔화 II 2초"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G09: 역장 생성 - 피격 시 넉백 (handled in listener)
class G09 extends Augment {
    public G09() { super("G09", "역장 생성", AugmentTier.GOLD, "피격 시 20% 확률로 주변 4블록 넉백 폭발 (쿨타임 15초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G10: 정밀 사격 - 활 데미지 증가 (handled in listener)
class G10 extends Augment {
    public G10() { super("G10", "정밀 사격", AugmentTier.GOLD, "활 완전 차지 시 데미지 40% 증가, 관통 I 효과"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G11: 지진술사 - 착지 시 광역 데미지 (handled in listener)
class G11 extends Augment {
    public G11() { super("G11", "지진술사", AugmentTier.GOLD, "웅크리기 2초 유지 시 반경 5블록 충격파: 둔화 + 2하트 (쿨타임 20초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G12: 영혼 수집 - 킬 시 최대 체력 증가 (handled in GameManager)
class G12 extends Augment {
    private static final UUID MOD_UUID = UUID.fromString("a2000012-0000-0000-0000-000000000012");
    public G12() { super("G12", "영혼 수집", AugmentTier.GOLD, "적 처치 시 영구적으로 최대 체력 +1하트 (최대 5하트 추가)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(MOD_UUID))
                .findFirst()
                .ifPresent(attr::removeModifier);
        }
    }
}

// G13: 차원 주머니 - 별도 인벤토리 (handled in command)
class G13 extends Augment {
    public G13() { super("G13", "차원 주머니", AugmentTier.GOLD, "/aw pocket 명령으로 별도 9칸 인벤토리 (장비류 보관 불가)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G14: 전투 도약 - 돌진 공격 (handled in listener)
class G14 extends Augment {
    public G14() { super("G14", "전투 도약", AugmentTier.GOLD, "스프린트 중 점프 시 전방 5블록 돌진+넉백+3하트 (쿨타임 12초)"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

// G15: 방패 생성기 - 15초마다 흡수 하트 4개
class G15 extends Augment {
    public G15() { super("G15", "방패 생성기", AugmentTier.GOLD, "15초마다 자동으로 흡수(노란 하트) 4개 생성"); }
    @Override public void onApply(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 1, false, false, true));
        setCooldown("G15_regen");
    }
    @Override public void onRemove(Player p) { p.removePotionEffect(PotionEffectType.ABSORPTION); }
    @Override public void onSecond(Player p) {
        if (!p.hasPotionEffect(PotionEffectType.ABSORPTION) && !isOnCooldown("G15_regen", 15)) {
            setCooldown("G15_regen");
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 1, false, false, true));
        }
    }
}

// G16: 약탈자 - 킬 시 아이템 획득 (handled in GameManager)
class G16 extends Augment {
    public G16() { super("G16", "약탈자", AugmentTier.GOLD, "적 처치 시 상대 인벤토리에서 랜덤 아이템 3개 자동 획득"); }
    @Override public void onApply(Player p) {}
    @Override public void onRemove(Player p) {}
}

public class GoldAugments {
    public static Augment create(String id) {
        return switch (id) {
            case "G01" -> new G01();
            case "G02" -> new G02();
            case "G03" -> new G03();
            case "G04" -> new G04();
            case "G05" -> new G05();
            case "G06" -> new G06();
            case "G07" -> new G07();
            case "G08" -> new G08();
            case "G09" -> new G09();
            case "G10" -> new G10();
            case "G11" -> new G11();
            case "G12" -> new G12();
            case "G13" -> new G13();
            case "G14" -> new G14();
            case "G15" -> new G15();
            case "G16" -> new G16();
            default -> null;
        };
    }
}
