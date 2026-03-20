package com.augwar.commands;

import com.augwar.AugWar;
import com.augwar.augment.Augment;
import com.augwar.game.GameState;
import com.augwar.player.AugPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AugWarCommand implements CommandExecutor {

    private final AugWar plugin;

    public AugWarCommand(AugWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            // === Admin commands ===
            case "create" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player p)) { sender.sendMessage("[AugWar] 게임 내에서 사용하세요."); return true; }
                plugin.getGameManager().setCenterLocation(p.getLocation());
                msg(sender, "맵 중심이 현재 위치로 설정되었습니다.");
            }
            case "setlobby" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player p)) return true;
                plugin.getGameManager().setLobbyLocation(p.getLocation());
                msg(sender, "로비 위치가 설정되었습니다.");
            }
            case "setspawn" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player p)) return true;
                if (args.length > 1 && args[1].equalsIgnoreCase("add")) {
                    plugin.getGameManager().addSpawnPoint(p.getLocation());
                    msg(sender, "스폰 포인트 추가 완료. (총 " + plugin.getGameManager().getSpawnPoints().size() + "개)");
                }
            }
            case "start" -> {
                if (!checkAdmin(sender)) return true;
                if (plugin.getGameManager().getCenterLocation() == null) {
                    msg(sender, "먼저 /aw create 로 맵 중심을 설정하세요.");
                    return true;
                }
                boolean ok = plugin.getGameManager().startGame(false);
                if (!ok) msg(sender, "게임을 시작할 수 없습니다. (이미 진행 중이거나 인원 부족)");
            }
            case "forcestart" -> {
                if (!checkAdmin(sender)) return true;
                if (plugin.getGameManager().getCenterLocation() == null) {
                    msg(sender, "먼저 /aw create 로 맵 중심을 설정하세요.");
                    return true;
                }
                boolean ok = plugin.getGameManager().startGame(true);
                if (!ok) msg(sender, "게임을 시작할 수 없습니다.");
                else msg(sender, "강제 시작!");
            }
            case "stop" -> {
                if (!checkAdmin(sender)) return true;
                if (plugin.getGameManager().getState() == GameState.WAITING) {
                    msg(sender, "진행 중인 게임이 없습니다.");
                    return true;
                }
                plugin.getGameManager().endGame();
                msg(sender, "게임을 종료합니다.");
            }
            case "forcestop" -> {
                if (!checkAdmin(sender)) return true;
                plugin.getGameManager().forceStop();
                msg(sender, "게임을 강제 중단하고 모든 데이터를 초기화했습니다.");
            }
            case "reload" -> {
                if (!checkAdmin(sender)) return true;
                plugin.getConfigManager().reload();
                plugin.getAugmentRegistry().reload();
                msg(sender, "설정이 다시 로드되었습니다.");
            }
            case "give" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) { msg(sender, "사용법: /aw give [플레이어] [증강ID]"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { msg(sender, "플레이어를 찾을 수 없습니다."); return true; }
                Augment aug = plugin.getAugmentRegistry().get(args[2].toUpperCase());
                if (aug == null) { msg(sender, "증강을 찾을 수 없습니다: " + args[2]); return true; }
                plugin.getAugmentManager().applyAugment(target, args[2].toUpperCase());
                msg(sender, target.getName() + "에게 " + aug.getName() + " 지급 완료.");
            }
            case "skip" -> {
                if (!checkAdmin(sender)) return true;
                if (plugin.getGameManager().getState() != GameState.PLAYING) {
                    msg(sender, "게임 중이 아닙니다.");
                    return true;
                }
                plugin.getPhaseManager().skip();
                msg(sender, "다음 페이즈로 건너뜁니다.");
            }
            case "config" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) { msg(sender, "사용법: /aw config [key] [value]"); return true; }
                plugin.getConfig().set(args[1], tryParse(args[2]));
                plugin.saveConfig();
                msg(sender, args[1] + " = " + args[2] + " 설정 완료.");
            }

            // === Team commands ===
            case "team" -> {
                if (args.length < 2) { msg(sender, "사용법: /aw team [create|add|random|list]"); return true; }
                handleTeam(sender, args);
            }

            // === Player commands ===
            case "join" -> {
                if (!(sender instanceof Player p)) return true;
                if (plugin.getGameManager().getState() != GameState.WAITING) {
                    msg(sender, "현재 참가할 수 없습니다.");
                    return true;
                }
                plugin.getGameManager().addPlayer(p);
                msg(sender, "게임에 참가했습니다.");
            }
            case "leave" -> {
                if (!(sender instanceof Player p)) return true;
                if (plugin.getGameManager().getState() != GameState.WAITING) {
                    msg(sender, "게임 중에는 나갈 수 없습니다.");
                    return true;
                }
                plugin.getGameManager().removePlayer(p);
                msg(sender, "게임에서 나왔습니다.");
            }
            case "check" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.getAugmentCheckGUI().open(p);
            }
            case "select" -> {
                if (!(sender instanceof Player p)) return true;
                if (plugin.getAugmentSelectGUI().hasPending(p.getUniqueId())) {
                    plugin.getAugmentSelectGUI().reopenSelection(p);
                } else {
                    msg(sender, "현재 선택 가능한 증강이 없습니다.");
                }
            }
            case "info" -> {
                if (args.length < 2) { msg(sender, "사용법: /aw info [증강이름]"); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                Augment aug = plugin.getAugmentRegistry().findByName(name);
                if (aug == null) { msg(sender, "증강을 찾을 수 없습니다: " + name); return true; }
                msg(sender, aug.getDisplayName() + " - " + aug.getDescription());
            }
            case "stats" -> {
                if (!(sender instanceof Player p)) return true;
                msg(sender, plugin.getStatsManager().getStatsDisplay(p.getUniqueId().toString()));
            }
            case "top" -> {
                msg(sender, "랭킹 기능은 준비 중입니다.");
            }
            case "pocket" -> {
                if (!(sender instanceof Player p)) return true;
                AugPlayer ap = plugin.getGameManager().getAugPlayer(p);
                if (ap == null || !ap.hasAugment("G13")) {
                    msg(sender, "차원 주머니 증강이 없습니다.");
                    return true;
                }
                openPocket(p, ap);
            }
            case "spectate" -> {
                if (!(sender instanceof Player p)) return true;
                if (plugin.getGameManager().getState() != GameState.PLAYING && plugin.getGameManager().getState() != GameState.FROZEN) {
                    msg(sender, "게임 중이 아닙니다.");
                    return true;
                }
                AugPlayer spectAp = plugin.getGameManager().getAugPlayer(p);
                if (spectAp != null && spectAp.isAlive()) {
                    msg(sender, "생존 중인 플레이어는 관전 모드로 전환할 수 없습니다.");
                    return true;
                }
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
                msg(sender, "관전 모드로 전환했습니다.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleTeam(CommandSender sender, String[] args) {
        String sub2 = args[1].toLowerCase();
        switch (sub2) {
            case "create" -> {
                if (!checkAdmin(sender)) return;
                if (args.length < 4) { msg(sender, "사용법: /aw team create [팀명] [색상]"); return; }
                plugin.getTeamManager().createTeam(args[2], args[3]);
                msg(sender, "팀 '" + args[2] + "' 생성 완료.");
            }
            case "add" -> {
                if (!checkAdmin(sender)) return;
                if (args.length < 4) { msg(sender, "사용법: /aw team add [팀명] [플레이어]"); return; }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) { msg(sender, "플레이어를 찾을 수 없습니다."); return; }
                boolean ok = plugin.getTeamManager().addToTeam(args[2], target.getUniqueId());
                if (ok) msg(sender, target.getName() + "님을 " + args[2] + " 팀에 추가했습니다.");
                else msg(sender, "팀을 찾을 수 없습니다.");
            }
            case "random" -> {
                if (!checkAdmin(sender)) return;
                int count = 2;
                if (args.length > 2) {
                    try { count = Integer.parseInt(args[2]); }
                    catch (NumberFormatException ex) { msg(sender, "올바른 숫자를 입력하세요."); return; }
                }
                if (count < 1 || count > 4) { msg(sender, "팀 수는 1~4 사이여야 합니다."); return; }
                plugin.getTeamManager().randomizeTeams(count);
                msg(sender, count + "개 팀으로 랜덤 배정 완료.");
            }
            case "list" -> {
                var teams = plugin.getTeamManager().getTeams();
                if (teams.isEmpty()) { msg(sender, "등록된 팀이 없습니다."); return; }
                for (var entry : teams.entrySet()) {
                    StringBuilder sb = new StringBuilder(entry.getKey() + ": ");
                    for (var uuid : entry.getValue()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) sb.append(p.getName()).append(" ");
                    }
                    msg(sender, sb.toString());
                }
            }
        }
    }

    private void openPocket(Player p, AugPlayer ap) {
        org.bukkit.inventory.Inventory pocket = Bukkit.createInventory(null, 9,
                net.kyori.adventure.text.Component.text("차원 주머니"));
        if (ap.getPocketInventory() != null) {
            pocket.setContents(ap.getPocketInventory());
        }
        p.openInventory(pocket);
        // Save on close handled separately
    }

    private boolean checkAdmin(CommandSender sender) {
        if (!sender.hasPermission("augwar.admin")) {
            msg(sender, "권한이 없습니다.");
            return false;
        }
        return true;
    }

    private void msg(CommandSender sender, String message) {
        sender.sendMessage("[AugWar] " + message);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("=============================");
        sender.sendMessage("  AugWar - 증강전쟁 명령어");
        sender.sendMessage("=============================");
        if (sender.hasPermission("augwar.admin")) {
            sender.sendMessage("/aw create - 맵 중심 설정");
            sender.sendMessage("/aw setlobby - 로비 위치 설정");
            sender.sendMessage("/aw setspawn add - 스폰 포인트 추가");
            sender.sendMessage("/aw start - 게임 시작");
            sender.sendMessage("/aw stop - 게임 종료");
            sender.sendMessage("/aw forcestart - 강제 시작 (테스트)");
            sender.sendMessage("/aw forcestop - 강제 중단 (테스트)");
            sender.sendMessage("/aw give [플레이어] [증강ID] - 증강 지급 (테스트)");
            sender.sendMessage("/aw skip - 다음 페이즈 건너뛰기 (테스트)");
            sender.sendMessage("/aw reload - 설정 다시 로드");
        }
        sender.sendMessage("/aw join - 게임 참가");
        sender.sendMessage("/aw leave - 게임 나가기");
        sender.sendMessage("/aw check - 내 증강 확인");
        sender.sendMessage("/aw select - 증강 선택 GUI 재오픈");
        sender.sendMessage("/aw info [이름] - 증강 정보");
        sender.sendMessage("/aw stats - 전적 확인");
        sender.sendMessage("/aw pocket - 차원 주머니");
        sender.sendMessage("/aw spectate - 관전 모드");
    }

    private Object tryParse(String value) {
        try { return Integer.parseInt(value); } catch (Exception ignored) {}
        try { return Double.parseDouble(value); } catch (Exception ignored) {}
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return Boolean.parseBoolean(value);
        return value;
    }
}
