package com.augwar.listeners;

import com.augwar.AugWar;
import com.augwar.gui.AugmentCheckGUI;
import com.augwar.gui.AugmentSelectGUI;
import com.augwar.player.AugPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {

    private final AugWar plugin;
    private static final String POCKET_TITLE = "차원 주머니";

    public GUIListener(AugWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());

        if (title.equals(AugmentSelectGUI.GUI_TITLE)) {
            e.setCancelled(true);
            plugin.getAugmentSelectGUI().handleClick(player, e.getRawSlot());
            return;
        }

        if (title.equals(AugmentCheckGUI.GUI_TITLE)) {
            e.setCancelled(true);
            return;
        }

        // Pocket inventory - allow interaction within the 9 slots, block shift-click from player inv
        if (title.equals(POCKET_TITLE)) {
            // Block shift-click from player inventory area into pocket
            if (e.isShiftClick() && e.getRawSlot() >= 9) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());

        // Block all drag in selection and check GUIs
        if (title.equals(AugmentSelectGUI.GUI_TITLE) || title.equals(AugmentCheckGUI.GUI_TITLE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());

        // Save pocket inventory on close
        if (title.equals(POCKET_TITLE)) {
            AugPlayer ap = plugin.getGameManager().getAugPlayer(player);
            if (ap != null) {
                ap.setPocketInventory(e.getInventory().getContents());
            }
        }
    }
}
