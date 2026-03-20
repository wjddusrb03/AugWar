package com.augwar.augment;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public abstract class Augment {

    protected final String id;
    protected final String name;
    protected final AugmentTier tier;
    protected final String description;
    protected final Map<String, Long> cooldowns = new HashMap<>();

    public Augment(String id, String name, AugmentTier tier, String description) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public AugmentTier getTier() { return tier; }
    public String getDescription() { return description; }

    public String getDisplayName() {
        return tier.getColorCode() + "[" + tier.getDisplayName() + "] " + name;
    }

    /** Called when augment is applied to the player */
    public abstract void onApply(Player player);

    /** Called when augment is removed from the player */
    public abstract void onRemove(Player player);

    /** Called every tick for continuous effects (override if needed) */
    public void onTick(Player player) {}

    /** Called every second for periodic effects (override if needed) */
    public void onSecond(Player player) {}

    /** Check cooldown */
    public boolean isOnCooldown(String key, int cooldownSeconds) {
        Long lastUse = cooldowns.get(key);
        if (lastUse == null) return false;
        return System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L;
    }

    /** Set cooldown */
    public void setCooldown(String key) {
        cooldowns.put(key, System.currentTimeMillis());
    }
}
