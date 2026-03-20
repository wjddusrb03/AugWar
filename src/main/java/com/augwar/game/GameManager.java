package com.augwar.game;

import com.augwar.AugWar;
import com.augwar.player.AugPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final AugWar plugin;
    private GameState state = GameState.WAITING;
    private final Map<UUID, AugPlayer> players = new ConcurrentHashMap<>();
    private final List<Location> spawnPoints = new ArrayList<>();
    private Location lobbyLocation;
    private Location centerLocation;
    private int gameTimeTicks = 0;
    private BukkitRunnable gameTimer;
    // Track scheduled tasks for cleanup on forceStop
    private final List<BukkitRunnable> scheduledTasks = new ArrayList<>();

    public GameManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public Map<UUID, AugPlayer> getPlayerMap() { return players; }

    public AugPlayer getAugPlayer(UUID uuid) { return players.get(uuid); }
    public AugPlayer getAugPlayer(Player player) { return players.get(player.getUniqueId()); }

    public Collection<Player> getAllPlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) list.add(p);
        }
        return list;
    }

    public Collection<Player> getAlivePlayers() {
        List<Player> list = new ArrayList<>();
        for (Map.Entry<UUID, AugPlayer> entry : players.entrySet()) {
            if (entry.getValue().isAlive()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) list.add(p);
            }
        }
        return list;
    }

    public int getAliveCount() {
        int count = 0;
        for (Map.Entry<UUID, AugPlayer> entry : players.entrySet()) {
            AugPlayer ap = entry.getValue();
            if (ap.isAlive() && !ap.isDisconnected()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) count++;
            }
        }
        return count;
    }

    /** Count including disconnected but alive players (for total alive tracking) */
    public int getTotalAliveCount() {
        return (int) players.values().stream().filter(AugPlayer::isAlive).count();
    }

    public int getTotalCount() { return players.size(); }

    public int getGameTimeSeconds() { return gameTimeTicks / 20; }

    public Location getLobbyLocation() { return lobbyLocation; }
    public void setLobbyLocation(Location loc) { this.lobbyLocation = loc; }

    public Location getCenterLocation() { return centerLocation; }
    public void setCenterLocation(Location loc) { this.centerLocation = loc; }

    public List<Location> getSpawnPoints() { return spawnPoints; }
    public void addSpawnPoint(Location loc) { spawnPoints.add(loc); }

    // === Game Flow ===

    public boolean startGame(boolean force) {
        if (state != GameState.WAITING) return false;

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (!force && online.size() < plugin.getConfigManager().getMinPlayers()) return false;

        state = GameState.STARTING;
        players.clear();
        gameTimeTicks = 0;

        for (Player p : online) {
            if (players.size() >= plugin.getConfigManager().getMaxPlayers()) break;
            AugPlayer ap = new AugPlayer(p);
            players.put(p.getUniqueId(), ap);
        }

        // Countdown
        int countdown = plugin.getConfigManager().getCountdown();
        new BukkitRunnable() {
            int count = countdown;
            @Override
            public void run() {
                if (state != GameState.STARTING) { cancel(); return; }
                if (count <= 0) {
                    cancel();
                    beginGame();
                    return;
                }
                if (count <= 5 || count % 10 == 0) {
                    plugin.getMessageUtil().broadcast("게임 시작까지 " + count + "초");
                    for (Player p : getAllPlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                }
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void beginGame() {
        state = GameState.PLAYING;

        // Teleport players to spawn points
        List<Player> playerList = new ArrayList<>(getAllPlayers());
        Collections.shuffle(playerList);
        for (int i = 0; i < playerList.size(); i++) {
            Player p = playerList.get(i);
            if (!spawnPoints.isEmpty()) {
                Location spawn = spawnPoints.get(i % spawnPoints.size());
                p.teleport(spawn);
            } else if (centerLocation != null) {
                // Random spread around center
                double angle = Math.random() * 2 * Math.PI;
                double dist = 50 + Math.random() * 100;
                Location spawn = centerLocation.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                spawn.setY(spawn.getWorld().getHighestBlockYAt(spawn) + 1);
                p.teleport(spawn);
            }

            // Reset player state
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setSaturation(5f);
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setLevel(0);
            p.setExp(0);
        }

        // Set world border
        if (centerLocation != null) {
            WorldBorder border = centerLocation.getWorld().getWorldBorder();
            border.setCenter(centerLocation);
            border.setSize(plugin.getConfigManager().getBorderInitial() * 2);
            border.setDamageAmount(0);
            border.setDamageBuffer(0);
        }

        // Show credits
        plugin.getMessageUtil().sendCredits();

        // Start game timer
        startGameTimer();

        // Start phase manager
        plugin.getPhaseManager().start();

        // Start augment tick task (for onTick/onSecond augments)
        plugin.getAugmentManager().startTickTask();

        // Start scoreboard updates
        plugin.getScoreboardMgr().startUpdating();
        plugin.getBossBarManager().startUpdating();
        plugin.getTabListManager().startUpdating();
    }

    private void startGameTimer() {
        gameTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING && state != GameState.FROZEN) {
                    cancel();
                    return;
                }
                if (state == GameState.PLAYING) {
                    gameTimeTicks++;
                    // Anti-camp check every second
                    if (gameTimeTicks % 20 == 0) {
                        checkAntiCamp();
                    }
                }
            }
        };
        gameTimer.runTaskTimer(plugin, 0L, 1L);
    }

    private void checkAntiCamp() {
        if (!plugin.getConfigManager().isAntiCampEnabled()) return;
        int seconds = getGameTimeSeconds();
        int startAfter = plugin.getConfigManager().getAntiCampStartAfter();
        int idleTime = plugin.getConfigManager().getAntiCampIdleTime();

        if (seconds < startAfter) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, AugPlayer> entry : players.entrySet()) {
            AugPlayer ap = entry.getValue();
            if (!ap.isAlive() || ap.isDisconnected()) continue;
            long idleMs = now - ap.getLastCombatTime();
            if (idleMs >= idleTime * 1000L && !ap.isLocationExposed()) {
                ap.setLocationExposed(true);
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    plugin.getMessageUtil().broadcast(p.getName() + "님의 위치가 공개되었습니다.");
                    // Glow effect
                    p.setGlowing(true);
                }
            }
        }
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        AugPlayer ap = getAugPlayer(victim);
        if (ap == null) return;

        // Check S12 totem fragment
        if (ap.hasAugment("S12") && !ap.isTotemUsed()) {
            ap.setTotemUsed(true);
            victim.setHealth(4.0); // 2 hearts
            plugin.getMessageUtil().send(victim, "불사의 토템 파편이 발동되었습니다!");
            victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            return;
        }

        // Check P02 revive
        if (ap.hasAugment("P02") && !ap.isReviveUsed()) {
            ap.setReviveUsed(true);
            BukkitRunnable reviveTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isOnline() || state == GameState.ENDING || state == GameState.WAITING) return;
                    victim.spigot().respawn();
                    victim.teleport(victim.getLocation());
                    victim.setHealth(20);
                    victim.setFoodLevel(20);
                    // 10 second invincibility
                    victim.setInvulnerable(true);
                    plugin.getMessageUtil().send(victim, "불멸의 전사가 발동되었습니다! 10초간 무적.");
                    plugin.getMessageUtil().broadcast(victim.getName() + "님이 부활했습니다!");
                    BukkitRunnable invincTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (victim.isOnline()) victim.setInvulnerable(false);
                        }
                    };
                    invincTask.runTaskLater(plugin, 200L);
                    scheduledTasks.add(invincTask);
                }
            };
            reviveTask.runTaskLater(plugin, 1L);
            scheduledTasks.add(reviveTask);
            return;
        }

        // Actual death
        ap.setAlive(false);
        ap.setSpectating(true);
        victim.setGameMode(GameMode.SPECTATOR);

        // Death message
        String augmentList = ap.getAugmentNames();
        if (killer != null) {
            plugin.getMessageUtil().broadcast(killer.getName() + " -> " + victim.getName() + " (" + augmentList + ")");
            handleKill(killer, victim);
        } else {
            plugin.getMessageUtil().broadcast(victim.getName() + "님이 사망했습니다. (" + augmentList + ")");
        }

        // Remove augments
        for (var aug : ap.getAugments()) {
            aug.onRemove(victim);
        }

        // Check win condition
        checkWinCondition();
    }

    private void handleKill(Player killer, Player victim) {
        AugPlayer kp = getAugPlayer(killer);
        AugPlayer vp = getAugPlayer(victim);
        if (kp == null) return;

        kp.addKill();
        kp.updateCombatTime();

        // Remove glow if location was exposed
        if (kp.isLocationExposed()) {
            kp.setLocationExposed(false);
            killer.setGlowing(false);
        }

        // Kill upgrade: tier boost every 2 kills (not every kill)
        if (plugin.getConfigManager().isKillUpgrade() && kp.getKills() % 2 == 0) {
            kp.setNextTierUpgrade(true);
            plugin.getMessageUtil().send(killer, "2킬 달성! 다음 증강 등급이 상승합니다!");
        }

        // P04 Doppelganger / P10 Augment steal (only one per kill)
        boolean augmentCopied = false;
        if (kp.hasAugment("P04") && vp != null && !vp.getAugments().isEmpty()) {
            var victimAugs = vp.getAugments();
            var random = victimAugs.get(new Random().nextInt(victimAugs.size()));
            if (!kp.hasAugment(random.getId())) {
                plugin.getAugmentManager().applyAugment(killer, random.getId());
                plugin.getMessageUtil().send(killer, "도플갱어: " + random.getName() + " 복사 완료!");
                augmentCopied = true;
            }
        }

        if (!augmentCopied && kp.hasAugment("P10") && vp != null && !vp.getAugments().isEmpty()) {
            var victimAugs = vp.getAugments();
            var randomAug = victimAugs.get(new Random().nextInt(victimAugs.size()));
            if (!kp.hasAugment(randomAug.getId())) {
                int minTarget = plugin.getConfigManager().getAugmentInt("P10", "min-target-augments", 2);
                if (victimAugs.size() >= minTarget) {
                    plugin.getMessageUtil().send(killer, "증강 강탈: " + randomAug.getName() + " 획득!");
                } else {
                    plugin.getMessageUtil().send(killer, "증강 복사: " + randomAug.getName() + " 획득!");
                }
                plugin.getAugmentManager().applyAugment(killer, randomAug.getId());
            }
        }

        // G12 Soul collection - use AttributeModifier
        if (kp.hasAugment("G12")) {
            double max = plugin.getConfigManager().getAugmentDouble("G12", "max-bonus", 10.0);
            double per = plugin.getConfigManager().getAugmentDouble("G12", "health-per-kill", 2.0);
            if (kp.getBonusHealth() < max) {
                kp.addBonusHealth(per);
                UUID modUuid = UUID.fromString("a2000012-0000-0000-0000-000000000012");
                var attr = killer.getAttribute(Attribute.MAX_HEALTH);
                if (attr != null) {
                    // Remove old modifier and add updated one
                    attr.getModifiers().stream()
                        .filter(m -> m.getUniqueId().equals(modUuid))
                        .findFirst()
                        .ifPresent(attr::removeModifier);
                    attr.addModifier(new AttributeModifier(modUuid, "augwar_g12", kp.getBonusHealth(), AttributeModifier.Operation.ADD_NUMBER));
                }
                plugin.getMessageUtil().send(killer, "영혼 수집: 최대 체력 +" + (int)(per/2) + " 하트");
            }
        }

        // G16 Looter - give killer 3 random items from victim's inventory
        if (kp.hasAugment("G16") && vp != null) {
            List<ItemStack> victimItems = new ArrayList<>();
            for (ItemStack item : victim.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) victimItems.add(item.clone());
            }
            Collections.shuffle(victimItems);
            int lootCount = Math.min(3, victimItems.size());
            for (int i = 0; i < lootCount; i++) {
                HashMap<Integer, ItemStack> overflow = killer.getInventory().addItem(victimItems.get(i));
                // Drop items that don't fit
                for (ItemStack drop : overflow.values()) {
                    killer.getWorld().dropItemNaturally(killer.getLocation(), drop);
                }
            }
            if (lootCount > 0) {
                plugin.getMessageUtil().send(killer, "약탈자: 적 인벤토리에서 " + lootCount + "개 아이템 획득!");
            }
        }

        // Kill streak bonuses
        if (plugin.getConfigManager().isKillStreakBonus()) {
            if (kp.getKills() == 3) {
                plugin.getMessageUtil().send(killer, "3킬 보너스! 실버 등급 증강 지급!");
                // 3킬 보너스는 Silver 등급으로 제한 (스노우볼 방지)
                plugin.getAugmentSelectGUI().openSelection(killer, "SILVER", plugin.getPhaseManager().getCurrentPhase());
            }
            if (kp.getKills() == 5) {
                plugin.getMessageUtil().broadcast(killer.getName() + "님이 5연속 처치! 학살자!");
                // 5킬 보너스: 체력 회복만 (이동속도 삭제)
                killer.setHealth(Math.min(getMaxHealth(killer), killer.getHealth() + 6.0));
                plugin.getMessageUtil().send(killer, "5킬 보너스: 체력 3하트 회복!");
            }
        }

        // Check synergies
        plugin.getSynergyManager().checkSynergies(killer);
    }

    public void checkWinCondition() {
        if (state != GameState.PLAYING && state != GameState.FROZEN) return;

        int aliveCount = getAliveCount();
        if (aliveCount <= 1) {
            // If frozen, unfreeze first
            if (state == GameState.FROZEN) {
                plugin.getFreezeManager().unfreezeWorld();
            }
            endGame();
        }
    }

    public void endGame() {
        state = GameState.ENDING;

        if (gameTimer != null) gameTimer.cancel();
        plugin.getPhaseManager().stop();
        plugin.getAugmentManager().stopTickTask();
        plugin.getScoreboardMgr().stopUpdating();
        plugin.getBossBarManager().stopUpdating();
        plugin.getTabListManager().stopUpdating();

        // Find winner
        Player winner = null;
        AugPlayer winnerAp = null;
        for (Map.Entry<UUID, AugPlayer> entry : players.entrySet()) {
            if (entry.getValue().isAlive()) {
                winner = Bukkit.getPlayer(entry.getKey());
                winnerAp = entry.getValue();
                break;
            }
        }

        if (winner != null && winnerAp != null) {
            plugin.getTitleManager().showWin(winner.getName());
            plugin.getMessageUtil().sendWinMessage(
                    winner.getName(),
                    winnerAp.getKills(),
                    winnerAp.getSurvivalSeconds(),
                    winnerAp.getAugmentNames()
            );

            // Fireworks
            Player w = winner;
            BukkitRunnable fireworkTask = new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count >= 5 || !w.isOnline() || state == GameState.WAITING) { cancel(); return; }
                    Firework fw = w.getWorld().spawn(w.getLocation().add(0, 1, 0), Firework.class);
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(Color.PURPLE, Color.YELLOW)
                            .withFade(Color.WHITE)
                            .trail(true)
                            .flicker(true)
                            .build());
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                    count++;
                }
            };
            fireworkTask.runTaskTimer(plugin, 0L, 20L);
            scheduledTasks.add(fireworkTask);

            // Save stats
            plugin.getStatsManager().recordWin(winnerAp);
        }

        // Save all stats
        for (AugPlayer ap : players.values()) {
            plugin.getStatsManager().recordGame(ap);
        }

        // Return to lobby after 10 seconds
        BukkitRunnable lobbyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.ENDING) return;
                returnToLobby();
            }
        };
        lobbyTask.runTaskLater(plugin, 200L);
        scheduledTasks.add(lobbyTask);
    }

    private void returnToLobby() {
        for (Player p : getAllPlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(20);
            p.setFoodLevel(20);
            // Reset max health via attribute (remove all augwar modifiers)
            var maxHpAttr = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHpAttr != null) {
                new ArrayList<>(maxHpAttr.getModifiers()).stream()
                    .filter(m -> m.getName().startsWith("augwar"))
                    .forEach(maxHpAttr::removeModifier);
            }
            p.setWalkSpeed(0.2f);
            p.setGlowing(false);
            p.setInvulnerable(false);

            if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }

            // Remove augments
            AugPlayer ap = getAugPlayer(p);
            if (ap != null) {
                for (var aug : ap.getAugments()) {
                    aug.onRemove(p);
                }
                ap.reset();
            }
        }

        // Reset world border
        if (centerLocation != null) {
            WorldBorder border = centerLocation.getWorld().getWorldBorder();
            border.setSize(60000000);
        }

        players.clear();
        scheduledTasks.clear();
        state = GameState.WAITING;
        plugin.getScoreboardMgr().removeAll();
        plugin.getBossBarManager().removeAll();
    }

    private void cancelScheduledTasks() {
        for (BukkitRunnable task : scheduledTasks) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        scheduledTasks.clear();
    }

    public void forceStop() {
        if (gameTimer != null) gameTimer.cancel();
        cancelScheduledTasks();
        plugin.getPhaseManager().stop();
        plugin.getScoreboardMgr().stopUpdating();
        plugin.getBossBarManager().stopUpdating();
        plugin.getTabListManager().stopUpdating();
        plugin.getDisconnectManager().cancelAll();
        plugin.getAugmentManager().stopTickTask();
        // Unfreeze if frozen
        if (plugin.getFreezeManager().isFrozen()) {
            plugin.getFreezeManager().unfreezeWorld();
        }
        plugin.getTeamManager().clear();
        returnToLobby();
    }

    private double getMaxHealth(Player p) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    public void addPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new AugPlayer(player));
        }
    }

    public void removePlayer(Player player) {
        AugPlayer ap = players.get(player.getUniqueId());
        if (ap != null) {
            ap.setAlive(false);
            for (var aug : ap.getAugments()) {
                aug.onRemove(player);
            }
        }
    }
}
