package com.augwar.game;

import com.augwar.AugWar;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    private final AugWar plugin;
    private final Map<String, List<UUID>> teams = new LinkedHashMap<>();
    private final Map<String, ChatColor> teamColors = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();

    public TeamManager(AugWar plugin) {
        this.plugin = plugin;
    }

    public void createTeam(String name, String color) {
        teams.put(name, new ArrayList<>());
        try {
            teamColors.put(name, ChatColor.valueOf(color.toUpperCase()));
        } catch (Exception e) {
            teamColors.put(name, ChatColor.WHITE);
        }
    }

    public boolean addToTeam(String teamName, UUID uuid) {
        if (!teams.containsKey(teamName)) return false;
        teams.get(teamName).add(uuid);
        playerTeams.put(uuid, teamName);
        return true;
    }

    public String getTeam(UUID uuid) { return playerTeams.get(uuid); }

    public boolean isSameTeam(UUID a, UUID b) {
        String ta = playerTeams.get(a);
        String tb = playerTeams.get(b);
        return ta != null && ta.equals(tb);
    }

    public List<UUID> getTeamMembers(String team) {
        return teams.getOrDefault(team, new ArrayList<>());
    }

    public Map<String, List<UUID>> getTeams() { return teams; }
    public ChatColor getTeamColor(String team) { return teamColors.getOrDefault(team, ChatColor.WHITE); }

    public void randomizeTeams(int teamCount) {
        teams.clear();
        playerTeams.clear();
        String[] names = {"red", "blue", "green", "yellow"};

        int actualCount = Math.min(Math.max(teamCount, 1), 4);
        for (int i = 0; i < actualCount; i++) {
            createTeam(names[i], names[i]);
        }

        List<UUID> allPlayers = new ArrayList<>(plugin.getGameManager().getPlayerMap().keySet());
        Collections.shuffle(allPlayers);

        for (int i = 0; i < allPlayers.size(); i++) {
            String team = names[i % actualCount];
            addToTeam(team, allPlayers.get(i));
        }
    }

    public void clear() {
        teams.clear();
        playerTeams.clear();
        teamColors.clear();
    }
}
