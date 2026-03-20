package com.augwar.listeners;

import com.augwar.AugWar;
import com.augwar.game.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;

public class FreezeListener implements Listener {

    private final AugWar plugin;

    public FreezeListener(AugWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (plugin.getGameManager().getAugPlayer(e.getPlayer()) == null) return;

        if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ() || e.getFrom().getY() != e.getTo().getY()) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageFrozen(EntityDamageEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (plugin.getGameManager().getAugPlayer(e.getPlayer()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemConsume(PlayerItemConsumeEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (plugin.getGameManager().getAugPlayer(e.getPlayer()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (plugin.getGameManager().getAugPlayer(e.getPlayer()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBowShoot(EntityShootBowEvent e) {
        if (plugin.getGameManager().getState() != GameState.FROZEN) return;
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }
}
