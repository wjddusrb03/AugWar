package com.augwar.gui;

import com.augwar.AugWar;
import com.augwar.augment.Augment;
import com.augwar.augment.AugmentTier;
import com.augwar.player.AugPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AugmentSelectGUI {

    private final AugWar plugin;
    public static final String GUI_TITLE = "증강 선택";
    private final Map<UUID, List<Augment>> pendingSelections = new HashMap<>();
    private final Set<UUID> selectedPlayers = new HashSet<>();
    private final Random sharedRandom = new Random();

    public AugmentSelectGUI(AugWar plugin) {
        this.plugin = plugin;
    }

    public void openSelection(Player player, String tierName, int phaseIndex) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null || !ap.isAlive()) return;

        AugmentTier tier;
        try {
            tier = AugmentTier.valueOf(tierName);
        } catch (Exception e) {
            tier = AugmentTier.SILVER;
        }

        // Apply kill upgrade
        if (ap.hasNextTierUpgrade()) {
            tier = tier.upgrade();
            ap.setNextTierUpgrade(false);
        }

        // Get candidates excluding already owned
        Set<String> owned = new HashSet<>();
        for (Augment a : ap.getAugments()) owned.add(a.getId());
        int candidateCount = plugin.getConfigManager().getCandidates();
        List<Augment> candidates = plugin.getAugmentRegistry().getRandomCandidates(tier, candidateCount, owned);

        if (candidates.isEmpty()) return;

        pendingSelections.put(player.getUniqueId(), candidates);
        selectedPlayers.remove(player.getUniqueId());

        Inventory gui = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE));

        // Fill border with glass
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        // Place candidates at slots 11, 13, 15
        int[] slots = {11, 13, 15};
        for (int i = 0; i < candidates.size() && i < 3; i++) {
            Augment aug = candidates.get(i);
            gui.setItem(slots[i], createAugmentItem(aug));
        }

        player.openInventory(gui);
    }

    public void reopenSelection(Player player) {
        List<Augment> pending = pendingSelections.get(player.getUniqueId());
        if (pending == null || selectedPlayers.contains(player.getUniqueId())) {
            plugin.getMessageUtil().send(player, "현재 선택 가능한 증강이 없습니다.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE));
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        int[] slots = {11, 13, 15};
        for (int i = 0; i < pending.size() && i < 3; i++) {
            gui.setItem(slots[i], createAugmentItem(pending.get(i)));
        }

        player.openInventory(gui);
    }

    private ItemStack createAugmentItem(Augment aug) {
        Material mat = switch (aug.getTier()) {
            case SILVER -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case PRISM -> Material.DIAMOND;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor color = switch (aug.getTier()) {
            case SILVER -> NamedTextColor.GRAY;
            case GOLD -> NamedTextColor.GOLD;
            case PRISM -> NamedTextColor.LIGHT_PURPLE;
        };

        meta.displayName(Component.text("[" + aug.getTier().getDisplayName() + "] " + aug.getName())
                .color(color).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("효과:").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(aug.getDescription()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("클릭하여 선택").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public boolean handleClick(Player player, int slot) {
        List<Augment> candidates = pendingSelections.get(player.getUniqueId());
        if (candidates == null) return false;
        if (selectedPlayers.contains(player.getUniqueId())) return false;

        int index = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };

        if (index < 0 || index >= candidates.size()) return false;

        Augment chosen = candidates.get(index);
        selectedPlayers.add(player.getUniqueId());
        player.closeInventory();

        plugin.getAugmentManager().applyAugment(player, chosen.getId());
        return true;
    }

    public void autoSelectAll() {
        for (Map.Entry<UUID, List<Augment>> entry : pendingSelections.entrySet()) {
            if (selectedPlayers.contains(entry.getKey())) continue;

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            List<Augment> candidates = entry.getValue();
            if (candidates.isEmpty()) continue;

            Augment chosen = candidates.get(sharedRandom.nextInt(candidates.size()));
            selectedPlayers.add(entry.getKey());
            p.closeInventory();

            plugin.getAugmentManager().applyAugment(p, chosen.getId());
            plugin.getMessageUtil().send(p, "시간 초과 - 자동 선택: " + chosen.getDisplayName());
        }
        pendingSelections.clear();
        selectedPlayers.clear();
    }

    public void forceAutoSelect(UUID uuid) {
        if (selectedPlayers.contains(uuid)) return;
        List<Augment> candidates = pendingSelections.get(uuid);
        if (candidates == null || candidates.isEmpty()) return;

        Player p = Bukkit.getPlayer(uuid);
        Augment chosen = candidates.get(sharedRandom.nextInt(candidates.size()));
        selectedPlayers.add(uuid);

        if (p != null && p.isOnline()) {
            p.closeInventory();
            plugin.getAugmentManager().applyAugment(p, chosen.getId());
            plugin.getMessageUtil().send(p, "자동 선택: " + chosen.getDisplayName());
        }
    }

    public boolean allSelected() {
        for (UUID uuid : pendingSelections.keySet()) {
            AugPlayer ap = plugin.getGameManager().getAugPlayer(uuid);
            if (ap != null && ap.isAlive() && !selectedPlayers.contains(uuid)) return false;
        }
        return true;
    }

    public boolean hasPending(UUID uuid) {
        return pendingSelections.containsKey(uuid) && !selectedPlayers.contains(uuid);
    }
}
