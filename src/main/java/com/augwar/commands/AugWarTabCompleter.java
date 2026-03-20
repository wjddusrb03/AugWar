package com.augwar.commands;

import com.augwar.AugWar;
import com.augwar.augment.Augment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AugWarTabCompleter implements TabCompleter {

    private final AugWar plugin;

    public AugWarTabCompleter(AugWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "leave", "check", "select", "info", "stats", "top", "pocket", "spectate"));
            if (sender.hasPermission("augwar.admin")) {
                subs.addAll(List.of("create", "setlobby", "setspawn", "start", "stop", "forcestart", "forcestop", "reload", "give", "skip", "config", "team"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give" -> { return filter(getOnlineNames(), args[1]); }
                case "team" -> { return filter(List.of("create", "add", "random", "list"), args[1]); }
                case "setspawn" -> { return filter(List.of("add"), args[1]); }
                case "info" -> {
                    return filter(plugin.getAugmentRegistry().getAll().stream().map(Augment::getName).collect(Collectors.toList()), args[1]);
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return filter(plugin.getAugmentRegistry().getAll().stream().map(Augment::getId).collect(Collectors.toList()), args[2]);
            }
            if (args[0].equalsIgnoreCase("team")) {
                if (args[1].equalsIgnoreCase("add")) {
                    return filter(new ArrayList<>(plugin.getTeamManager().getTeams().keySet()), args[2]);
                }
                // team create [팀명] - no completion for team name
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("team")) {
                if (args[1].equalsIgnoreCase("add")) {
                    return filter(getOnlineNames(), args[3]);
                }
                if (args[1].equalsIgnoreCase("create")) {
                    // team create [팀명] [색상]
                    return filter(List.of("red", "blue", "green", "yellow"), args[3]);
                }
            }
        }

        return completions;
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
