package com.augwar.augment;

public enum AugmentTier {
    SILVER("§7", "실버", "IRON_INGOT"),
    GOLD("§6", "골드", "GOLD_INGOT"),
    PRISM("§d§l", "프리즘", "DIAMOND");

    private final String colorCode;
    private final String displayName;
    private final String iconMaterial;

    AugmentTier(String colorCode, String displayName, String iconMaterial) {
        this.colorCode = colorCode;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
    }

    public String getColorCode() { return colorCode; }
    public String getDisplayName() { return displayName; }
    public String getIconMaterial() { return iconMaterial; }

    public AugmentTier upgrade() {
        return switch (this) {
            case SILVER -> GOLD;
            case GOLD -> PRISM;
            case PRISM -> PRISM;
        };
    }
}
