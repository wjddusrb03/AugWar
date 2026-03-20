package com.augwar.gui;

import com.augwar.AugWar;
import com.augwar.augment.Augment;
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

import java.util.ArrayList;
import java.util.List;

public class AugmentCheckGUI {

    public static final String GUI_TITLE = "내 증강 확인";
    private final AugWar plugin;

    public AugmentCheckGUI(AugWar plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
        if (ap == null) {
            plugin.getMessageUtil().send(player, "게임 중이 아닙니다.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        List<Augment> augments = ap.getAugments();
        for (int i = 0; i < augments.size() && i < 9; i++) {
            Augment aug = augments.get(i);
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
            lore.add(Component.text("효과: " + aug.getDescription()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
            gui.setItem(9 + i, item); // Middle row
        }

        // Synergies info
        if (!ap.getSynergies().isEmpty()) {
            ItemStack synergyItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta sm = synergyItem.getItemMeta();
            sm.displayName(Component.text("활성 시너지").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String s : ap.getSynergies()) {
                lore.add(Component.text("- " + s).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            sm.lore(lore);
            synergyItem.setItemMeta(sm);
            gui.setItem(22, synergyItem);
        }

        player.openInventory(gui);
    }
}
