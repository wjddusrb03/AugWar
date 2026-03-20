package com.augwar.augment;

import com.augwar.AugWar;
import com.augwar.augment.silver.SilverAugments;
import com.augwar.augment.gold.GoldAugments;
import com.augwar.augment.prism.PrismAugments;

import java.util.*;
import java.util.stream.Collectors;

public class AugmentRegistry {

    private final AugWar plugin;
    private final Map<String, Augment> augments = new LinkedHashMap<>();

    public AugmentRegistry(AugWar plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerAll() {
        // Silver
        for (int i = 1; i <= 12; i++) {
            String id = String.format("S%02d", i);
            Augment aug = SilverAugments.create(id);
            if (aug != null && plugin.getConfigManager().isAugmentEnabled(id)) {
                augments.put(id, aug);
            }
        }
        // Gold
        for (int i = 1; i <= 16; i++) {
            String id = String.format("G%02d", i);
            Augment aug = GoldAugments.create(id);
            if (aug != null && plugin.getConfigManager().isAugmentEnabled(id)) {
                augments.put(id, aug);
            }
        }
        // Prism
        for (int i = 1; i <= 12; i++) {
            String id = String.format("P%02d", i);
            Augment aug = PrismAugments.create(id);
            if (aug != null && plugin.getConfigManager().isAugmentEnabled(id)) {
                augments.put(id, aug);
            }
        }

        // Remove disabled
        List<String> disabled = plugin.getConfigManager().getDisabledAugments();
        for (String id : disabled) {
            augments.remove(id);
        }

        plugin.getLogger().info("증강 " + augments.size() + "개 등록 완료.");
    }

    public Augment get(String id) { return augments.get(id); }

    public Augment createFresh(String id) {
        if (id.startsWith("S")) return SilverAugments.create(id);
        if (id.startsWith("G")) return GoldAugments.create(id);
        if (id.startsWith("P")) return PrismAugments.create(id);
        return null;
    }

    public List<Augment> getByTier(AugmentTier tier) {
        return augments.values().stream()
                .filter(a -> a.getTier() == tier)
                .collect(Collectors.toList());
    }

    public List<Augment> getRandomCandidates(AugmentTier tier, int count, Set<String> excludeIds) {
        List<Augment> pool = augments.values().stream()
                .filter(a -> a.getTier() == tier)
                .filter(a -> !excludeIds.contains(a.getId()))
                .collect(Collectors.toList());

        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public Collection<Augment> getAll() { return augments.values(); }

    public Augment findByName(String name) {
        return augments.values().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public void reload() {
        augments.clear();
        registerAll();
    }
}
