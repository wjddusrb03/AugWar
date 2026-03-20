package com.augwar.listeners;

import com.augwar.AugWar;
import com.augwar.game.GameState;
import com.augwar.player.AugPlayer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class CombatListener implements Listener {

    private final AugWar plugin;
    private final Random random = new Random();
    private static final String LIGHTNING_META = "augwar_p03_lightning";

    public CombatListener(AugWar plugin) {
        this.plugin = plugin;
    }

    private double getMaxHealth(Player p) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (e.isCancelled()) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

        if (attacker == null) return;

        AugPlayer attackerAp = plugin.getGameManager().getAugPlayer(attacker);
        AugPlayer victimAp = plugin.getGameManager().getAugPlayer(victim);
        if (attackerAp == null || victimAp == null) return;

        // Team check
        if (plugin.getTeamManager().isSameTeam(attacker.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        // Update combat time
        attackerAp.updateCombatTime();
        victimAp.updateCombatTime();
        if (attackerAp.isLocationExposed()) { attackerAp.setLocationExposed(false); attacker.setGlowing(false); }
        if (victimAp.isLocationExposed()) { victimAp.setLocationExposed(false); victim.setGlowing(false); }

        double damage = e.getDamage();

        // S04 양날의 검 - victim takes 3% more
        if (victimAp.hasAugment("S04")) {
            damage *= 1.03;
        }

        // G01 광전사 - attacker below 30% hp, +50% damage
        if (attackerAp.hasAugment("G01")) {
            double threshold = plugin.getConfigManager().getAugmentDouble("G01", "health-threshold", 0.3);
            if (attacker.getHealth() / getMaxHealth(attacker) <= threshold) {
                damage *= 1.5;
                if (!attacker.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false, true));
                }
            }
        }

        // G02 철벽 - victim sneaking, -40% damage
        if (victimAp.hasAugment("G02") && victim.isSneaking()) {
            damage *= 0.6;
        }

        // G05 폭발물 전문가 - self explosion damage -50% (both entity and block explosions)
        if (victimAp.hasAugment("G05") && (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            damage *= 0.5;
        }

        // G07 화염 마법사 - melee 30% fire
        if (attackerAp.hasAugment("G07") && e.getDamager() instanceof Player) {
            if (random.nextDouble() < 0.3) {
                victim.setFireTicks(60);
            }
        }

        // G08 서리 보행자 - melee slowness
        if (attackerAp.hasAugment("G08") && e.getDamager() instanceof Player) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, true));
        }

        // G09 역장 생성 - 20% knockback explosion on hit (team check added)
        if (victimAp.hasAugment("G09") && random.nextDouble() < 0.2) {
            if (!victimAp.getAugment("G09").isOnCooldown("G09", 15)) {
                victimAp.getAugment("G09").setCooldown("G09");
                for (Entity nearby : victim.getNearbyEntities(4, 4, 4)) {
                    if (nearby instanceof Player p && !p.equals(victim)) {
                        if (plugin.getTeamManager().isSameTeam(victim.getUniqueId(), p.getUniqueId())) continue;
                        p.setVelocity(p.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(1.5).setY(0.5));
                    }
                }
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            }
        }

        // G10 정밀 사격 - bow full charge +40%
        if (attackerAp.hasAugment("G10") && e.getDamager() instanceof Arrow arrow) {
            if (arrow.isCritical()) damage *= 1.4;
        }

        // P01 사신의 낫 - execute below threshold (고정 15데미지, 즉사 아님)
        if (attackerAp.hasAugment("P01")) {
            double threshold = plugin.getSynergyManager().getExecuteThreshold(attacker);
            if (victim.getHealth() / getMaxHealth(victim) <= threshold) {
                damage = 15.0; // 고정 15데미지 (7.5하트) - 즉사가 아닌 강력한 추가 데미지
                plugin.getMessageUtil().broadcast(attacker.getName() + "님이 " + victim.getName() + "님에게 처형 일격!");
            }
        }

        // P03 천둥의 분노 - 20% lightning (prevent recursion)
        if (attackerAp.hasAugment("P03") && random.nextDouble() < 0.2) {
            if (!attacker.hasMetadata(LIGHTNING_META)) {
                attacker.setMetadata(LIGHTNING_META, new FixedMetadataValue(plugin, true));
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.damage(12.0, attacker);
                attacker.removeMetadata(LIGHTNING_META, plugin);
            }
        }

        // P05 시간 왜곡 - 30% rewind
        if (victimAp.hasAugment("P05") && random.nextDouble() < 0.3) {
            var aug = victimAp.getAugment("P05");
            if (aug != null && !aug.isOnCooldown("P05", 45)) {
                aug.setCooldown("P05");
                var history = victimAp.getPositionHistory();
                if (!history.isEmpty()) {
                    double[] pos = history.getFirst();
                    victim.teleport(new Location(victim.getWorld(), pos[0], pos[1], pos[2]));
                    victim.setHealth(Math.min(getMaxHealth(victim), pos[3]));
                    plugin.getMessageUtil().send(victim, "시간 왜곡 발동!");
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // P12 최후의 저항 - below 30%, augment damage boost (1.3x)
        if (attackerAp.hasAugment("P12")) {
            if (attacker.getHealth() / getMaxHealth(attacker) <= 0.3) {
                damage *= 1.3;
            }
        }

        // Assassin synergy - first attack from invis x1.5
        if (plugin.getSynergyManager().isAssassinActive(attacker) && attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            damage *= 1.5;
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        e.setDamage(damage);

        // G03 뱀파이어 - lifesteal (use final damage for accuracy)
        if (attackerAp.hasAugment("G03") && e.getDamager() instanceof Player) {
            double rate = plugin.getSynergyManager().getLifestealRate(attacker);
            double finalDmg = e.getFinalDamage();
            double heal = finalDmg * rate;
            double newHp = Math.min(getMaxHealth(attacker), attacker.getHealth() + heal);
            attacker.setHealth(newHp);
        }

        // Track damage (use final damage for accuracy)
        double finalDamage = e.getFinalDamage();
        attackerAp.addDamageDealt(finalDamage);
        victimAp.addDamageTaken(finalDamage);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (!(e.getEntity() instanceof Player shooter)) return;
        AugPlayer ap = plugin.getGameManager().getAugPlayer(shooter);
        if (ap == null) return;

        // G07 화염 마법사 - all arrows are fire
        if (ap.hasAugment("G07") && e.getProjectile() instanceof Arrow arrow) {
            arrow.setFireTicks(200);
        }

        // S11 화살 절약
        if (ap.hasAugment("S11") && random.nextDouble() < 0.3) {
            e.setConsumeArrow(false);
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;

        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null || !ap.hasAugment("P07")) return;

        var aug = ap.getAugment("P07");
        if (aug != null && !aug.isOnCooldown("P07", 2)) {
            aug.setCooldown("P07");
            double healAmount = e.getAmount();
            for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
                if (nearby instanceof Player target && !target.equals(player)) {
                    AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                    if (tap != null && tap.isAlive()) {
                        if (!plugin.getTeamManager().isSameTeam(player.getUniqueId(), target.getUniqueId())) {
                            target.damage(healAmount, player);
                        }
                    }
                }
            }
        }
    }
}
