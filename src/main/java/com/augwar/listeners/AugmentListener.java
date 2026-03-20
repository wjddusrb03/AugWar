package com.augwar.listeners;

import com.augwar.AugWar;
import com.augwar.augment.Augment;
import com.augwar.game.GameState;
import com.augwar.player.AugPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AugmentListener implements Listener {

    private final AugWar plugin;
    private final Map<UUID, Long> sneakTimers = new HashMap<>();
    private final Map<UUID, Long> sneakInvisTimers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> sneakTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> invisTasks = new HashMap<>();

    public AugmentListener(AugWar plugin) {
        this.plugin = plugin;
    }

    // Cleanup on player quit
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        sneakTimers.remove(uuid);
        sneakInvisTimers.remove(uuid);
        cancelTask(sneakTasks, uuid);
        cancelTask(invisTasks, uuid);
        cancelTask(g11Tasks, uuid);
    }

    private void cancelTask(Map<UUID, BukkitRunnable> tasks, UUID uuid) {
        BukkitRunnable task = tasks.remove(uuid);
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
    }

    // G04 스톰 러너 - sprint damage aura
    @EventHandler
    public void onMoveStorm(PlayerMoveEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("G04") || !p.isSprinting()) return;

        // Only every 20 ticks worth of movement
        var aug = ap.getAugment("G04");
        if (aug.isOnCooldown("G04_tick", 1)) return;
        aug.setCooldown("G04_tick");

        for (Entity nearby : p.getNearbyEntities(3, 3, 3)) {
            if (nearby instanceof Player target && !target.equals(p)) {
                if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                if (tap != null && tap.isAlive()) {
                    target.damage(1.0, p); // 0.5 heart
                    // Synergy: 빙뢰 - add freeze
                    if (plugin.getSynergyManager().hasSynergy(p, "빙뢰")) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, false, true));
                    }
                }
            }
        }
    }

    // G08 서리 보행자 - freeze water
    @EventHandler
    public void onMoveFrost(PlayerMoveEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("G08")) return;

        Location loc = p.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = loc.clone().add(x, -1, z).getBlock();
                if (block.getType() == Material.WATER) {
                    block.setType(Material.FROSTED_ICE);
                }
            }
        }
    }

    // S10 연막탄 + G06 은신 달인 - sneak tracking
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null) return;

        if (e.isSneaking()) {
            sneakTimers.put(p.getUniqueId(), System.currentTimeMillis());

            // S10 연막탄 check - cancel previous task first
            if (ap.hasAugment("S10")) {
                cancelTask(sneakTasks, p.getUniqueId());
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!p.isOnline() || !p.isSneaking()) { cancel(); sneakTasks.remove(p.getUniqueId()); return; }
                        Long start = sneakTimers.get(p.getUniqueId());
                        if (start != null && System.currentTimeMillis() - start >= 3000) {
                            var aug = ap.getAugment("S10");
                            if (aug != null && !aug.isOnCooldown("S10", 60)) {
                                aug.setCooldown("S10");
                                for (Entity nearby : p.getNearbyEntities(3, 3, 3)) {
                                    if (nearby instanceof Player target && !target.equals(p)) {
                                        // Team check
                                        if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, true));
                                    }
                                }
                                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 0.5f);
                            }
                            cancel();
                            sneakTasks.remove(p.getUniqueId());
                        }
                    }
                };
                task.runTaskTimer(plugin, 20L, 20L);
                sneakTasks.put(p.getUniqueId(), task);
            }

            // G06 은신 달인 check - cancel previous task first
            if (ap.hasAugment("G06")) {
                cancelTask(invisTasks, p.getUniqueId());
                sneakInvisTimers.put(p.getUniqueId(), System.currentTimeMillis());
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!p.isOnline() || !p.isSneaking()) { cancel(); invisTasks.remove(p.getUniqueId()); return; }
                        Long start = sneakInvisTimers.get(p.getUniqueId());
                        if (start != null && System.currentTimeMillis() - start >= 10000) {
                            var aug = ap.getAugment("G06");
                            if (aug != null && !aug.isOnCooldown("G06", 30)) {
                                aug.setCooldown("G06"); // Set cooldown when activating
                                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                                plugin.getMessageUtil().send(p, "은신 활성화! 공격 시 해제됩니다.");
                            }
                            cancel();
                            invisTasks.remove(p.getUniqueId());
                        }
                    }
                };
                task.runTaskTimer(plugin, 20L, 20L);
                invisTasks.put(p.getUniqueId(), task);
            }
        } else {
            sneakTimers.remove(p.getUniqueId());
            sneakInvisTimers.remove(p.getUniqueId());
            cancelTask(sneakTasks, p.getUniqueId());
            cancelTask(invisTasks, p.getUniqueId());

            // G06 - remove invis on unsneak
            if (ap.hasAugment("G06") && p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    // S01 광부의 축복 - heal on ore break + S06 행운의 손 - double ore drops
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null) return;

        Material type = e.getBlock().getType();
        if (!isOre(type)) return;

        // S01 광부의 축복 - 광물 채굴 시 체력 1하트 회복
        if (ap.hasAugment("S01")) {
            var attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maxHp = attr != null ? attr.getValue() : 20.0;
            p.setHealth(Math.min(maxHp, p.getHealth() + 2.0));
        }

        // S06 행운의 손 - double drops
        if (ap.hasAugment("S06")) {
            e.setExpToDrop(e.getExpToDrop() * 2);
            for (var drop : e.getBlock().getDrops(p.getInventory().getItemInMainHand())) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
            }
        }
    }

    private boolean isOre(Material m) {
        return m.name().contains("ORE") || m == Material.ANCIENT_DEBRIS;
    }

    // P06 엔더 지배자 - infinite ender pearl with cooldown
    @EventHandler
    public void onEnderPearlThrow(ProjectileLaunchEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (!(e.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player p)) return;

        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("P06")) return;

        Augment aug = ap.getAugment("P06");
        if (aug.isOnCooldown("P06_pearl", 15)) {
            e.setCancelled(true);
            plugin.getMessageUtil().send(p, "엔더펄 쿨타임 중입니다.");
            return;
        }
        aug.setCooldown("P06_pearl");

        // Refund the ender pearl
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    // Ensure they always have an ender pearl
                    boolean hasEP = false;
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.ENDER_PEARL) { hasEP = true; break; }
                    }
                    if (!hasEP) p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    // P06 엔더 지배자 - AoE damage on ender pearl teleport
    @EventHandler
    public void onEnderTeleport(PlayerTeleportEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("P06")) return;

        // Cancel ender pearl self-damage
        e.getPlayer().setNoDamageTicks(1);

        // AoE damage at destination
        Location dest = e.getTo();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity nearby : p.getNearbyEntities(5, 5, 5)) {
                    if (nearby instanceof Player target && !target.equals(p)) {
                        if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                        AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                        if (tap != null && tap.isAlive()) {
                            target.damage(4.0, p); // 2 hearts
                        }
                    }
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
            }
        }.runTaskLater(plugin, 1L);
    }

    // P06 - give ender pearl on apply
    // Handled via onSecond to ensure player always has one
    // (see PrismAugments P06 onSecond)

    // G11 지진술사 - sneak 2s → shockwave AoE (20s cooldown)
    private final Map<UUID, BukkitRunnable> g11Tasks = new HashMap<>();

    @EventHandler
    public void onSneakG11(PlayerToggleSneakEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("G11")) return;

        if (e.isSneaking()) {
            Augment aug = ap.getAugment("G11");
            if (aug.isOnCooldown("G11_quake", 20)) return;

            cancelTask(g11Tasks, p.getUniqueId());
            BukkitRunnable task = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!p.isOnline() || !p.isSneaking()) { cancel(); g11Tasks.remove(p.getUniqueId()); return; }
                    ticks++;
                    if (ticks >= 40) { // 2 seconds
                        cancel();
                        g11Tasks.remove(p.getUniqueId());
                        aug.setCooldown("G11_quake");
                        // AoE shockwave: 5 block radius - slowness + 2 hearts damage
                        for (Entity nearby : p.getNearbyEntities(5, 3, 5)) {
                            if (nearby instanceof Player target && !target.equals(p)) {
                                if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                                AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                                if (tap != null && tap.isAlive()) {
                                    target.damage(4.0, p);
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, true));
                                }
                            }
                        }
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                        plugin.getMessageUtil().send(p, "지진술사 발동!");
                    }
                }
            };
            task.runTaskTimer(plugin, 0L, 1L);
            g11Tasks.put(p.getUniqueId(), task);
        } else {
            cancelTask(g11Tasks, p.getUniqueId());
        }
    }

    // G14 전투 도약 - sprint + jump -> forward dash + knockback + 3 hearts
    private final Set<UUID> g14Jumping = new HashSet<>();

    @EventHandler
    public void onMoveG14(PlayerMoveEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player p = e.getPlayer();
        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("G14")) return;

        Augment aug = ap.getAugment("G14");

        // Detect sprinting + going up (jump)
        if (p.isSprinting() && !p.isSneaking() && e.getTo() != null && e.getTo().getY() > e.getFrom().getY() && !g14Jumping.contains(p.getUniqueId())) {
            if (aug.isOnCooldown("G14_dash", 12)) return;
            g14Jumping.add(p.getUniqueId());
            aug.setCooldown("G14_dash");

            // Dash forward
            Vector dir = p.getLocation().getDirection().normalize().multiply(1.5).setY(0.4);
            p.setVelocity(dir);

            // Delayed: damage + knockback to nearby enemies on arrival
            new BukkitRunnable() {
                @Override
                public void run() {
                    g14Jumping.remove(p.getUniqueId());
                    for (Entity nearby : p.getNearbyEntities(3, 2, 3)) {
                        if (nearby instanceof Player target && !target.equals(p)) {
                            if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                            AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                            if (tap != null && tap.isAlive()) {
                                target.damage(6.0, p); // 3 hearts
                                Vector kb = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(0.4);
                                target.setVelocity(kb);
                            }
                        }
                    }
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
                    plugin.getMessageUtil().send(p, "전투 도약 발동!");
                }
            }.runTaskLater(plugin, 10L); // 0.5 sec delay for dash to complete
        }

        // Clean up landing detection
        if (g14Jumping.contains(p.getUniqueId()) && p.isOnGround()) {
            g14Jumping.remove(p.getUniqueId());
        }
    }

    // P11 영역 전개 - sneak + attack while sneaking -> AoE field (8s, 90s cooldown)
    @EventHandler
    public void onInteractP11(PlayerInteractEvent e) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (e.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_AIR && e.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
        if (ap == null || !ap.hasAugment("P11")) return;

        Augment aug = ap.getAugment("P11");
        if (aug.isOnCooldown("P11_field", 90)) return;
        aug.setCooldown("P11_field");

        plugin.getMessageUtil().send(p, "영역 전개! 8초간 결계가 유지됩니다.");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        // 8 seconds of AoE: slowness II + 1 heart/sec to enemies in radius 10
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 160 || !p.isOnline() || plugin.getGameManager().getState() != GameState.PLAYING) {
                    cancel();
                    return;
                }
                // Every 20 ticks (1 second), apply effects
                if (ticks % 20 == 0) {
                    for (Entity nearby : p.getNearbyEntities(10, 10, 10)) {
                        if (nearby instanceof Player target && !target.equals(p)) {
                            if (plugin.getTeamManager().isSameTeam(p.getUniqueId(), target.getUniqueId())) continue;
                            AugPlayer tap = plugin.getGameManager().getAugPlayer(target);
                            if (tap != null && tap.isAlive()) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false, true));
                                target.damage(2.0, p); // 1 heart
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // P06 - ensure ender pearl supply
    // P08, P09 - handled in Augment.onSecond() via AugmentManager tick task
}
