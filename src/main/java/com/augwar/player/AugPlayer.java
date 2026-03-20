package com.augwar.player;

import com.augwar.augment.Augment;
import com.augwar.augment.AugmentTier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AugPlayer {

    private final UUID uuid;
    private final String name;
    private final List<Augment> augments = new ArrayList<>();
    private final Set<String> synergies = new HashSet<>();

    private boolean alive = true;
    private boolean spectating = false;
    private int kills = 0;
    private double damageDealt = 0;
    private double damageTaken = 0;
    private long joinTime;
    private long lastCombatTime = 0;
    private boolean locationExposed = false;

    // Kill upgrade: next augment tier boost
    private boolean nextTierUpgrade = false;

    // Disconnect tracking
    private boolean disconnected = false;
    private long disconnectTime = 0;

    // Totem fragment (S12) used
    private boolean totemUsed = false;

    // Immortal warrior (P02) used
    private boolean reviveUsed = false;

    // Soul collection (G12) bonus health
    private double bonusHealth = 0;

    // Pocket inventory (G13)
    private ItemStack[] pocketInventory;

    // Time warp (P05) position snapshots
    private final LinkedList<double[]> positionHistory = new LinkedList<>();

    public AugPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.joinTime = System.currentTimeMillis();
        this.lastCombatTime = System.currentTimeMillis();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public List<Augment> getAugments() { return new ArrayList<>(augments); }
    public Set<String> getSynergies() { return synergies; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isSpectating() { return spectating; }
    public void setSpectating(boolean spectating) { this.spectating = spectating; }

    public int getKills() { return kills; }
    public void addKill() { this.kills++; }

    public double getDamageDealt() { return damageDealt; }
    public void addDamageDealt(double d) { this.damageDealt += d; }

    public double getDamageTaken() { return damageTaken; }
    public void addDamageTaken(double d) { this.damageTaken += d; }

    public long getJoinTime() { return joinTime; }

    public long getLastCombatTime() { return lastCombatTime; }
    public void updateCombatTime() { this.lastCombatTime = System.currentTimeMillis(); }

    public boolean isLocationExposed() { return locationExposed; }
    public void setLocationExposed(boolean exposed) { this.locationExposed = exposed; }

    public boolean hasNextTierUpgrade() { return nextTierUpgrade; }
    public void setNextTierUpgrade(boolean upgrade) { this.nextTierUpgrade = upgrade; }

    public boolean isDisconnected() { return disconnected; }
    public void setDisconnected(boolean dc) { this.disconnected = dc; }
    public long getDisconnectTime() { return disconnectTime; }
    public void setDisconnectTime(long t) { this.disconnectTime = t; }

    public boolean isTotemUsed() { return totemUsed; }
    public void setTotemUsed(boolean used) { this.totemUsed = used; }

    public boolean isReviveUsed() { return reviveUsed; }
    public void setReviveUsed(boolean used) { this.reviveUsed = used; }

    public double getBonusHealth() { return bonusHealth; }
    public void addBonusHealth(double amount) { this.bonusHealth += amount; }

    public ItemStack[] getPocketInventory() { return pocketInventory; }
    public void setPocketInventory(ItemStack[] inv) { this.pocketInventory = inv; }

    public LinkedList<double[]> getPositionHistory() { return positionHistory; }

    public void addAugment(Augment augment) {
        augments.add(augment);
    }

    public boolean hasAugment(String id) {
        return augments.stream().anyMatch(a -> a.getId().equalsIgnoreCase(id));
    }

    public Augment getAugment(String id) {
        return augments.stream().filter(a -> a.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public void removeAugment(String id) {
        augments.removeIf(a -> a.getId().equalsIgnoreCase(id));
    }

    public String getAugmentNames() {
        if (augments.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < augments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(augments.get(i).getName());
        }
        return sb.toString();
    }

    public String getAugmentDisplayForTab() {
        if (augments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < augments.size(); i++) {
            if (i > 0) sb.append(" | ");
            Augment a = augments.get(i);
            String color = switch (a.getTier()) {
                case SILVER -> "§7";
                case GOLD -> "§6";
                case PRISM -> "§d";
            };
            sb.append(color).append(a.getName());
        }
        return sb.toString();
    }

    public int getSurvivalSeconds() {
        return (int) ((System.currentTimeMillis() - joinTime) / 1000);
    }

    public void reset() {
        augments.clear();
        synergies.clear();
        alive = true;
        spectating = false;
        kills = 0;
        damageDealt = 0;
        damageTaken = 0;
        lastCombatTime = System.currentTimeMillis();
        locationExposed = false;
        nextTierUpgrade = false;
        disconnected = false;
        totemUsed = false;
        reviveUsed = false;
        bonusHealth = 0;
        pocketInventory = null;
        positionHistory.clear();
    }
}
