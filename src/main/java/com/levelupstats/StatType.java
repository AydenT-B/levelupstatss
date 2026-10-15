package com.levelupstats;

import net.minecraft.resources.ResourceLocation;

public enum StatType {
    SPEED("speed", 0xFF55FFFF),
    JUMP("jump", 0xFF55FF55),
    STRENGTH("strength", 0xFFFF5555),
    LUCK("luck", 0xFFFFAA00),
    CONSTITUTION("constitution", 0xFFFF55FF),
    INTELLIGENCE("intelligence", 0xFF55AAFF);

    private final String id;
    private final int color;

    StatType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public String nameKey() {
        return "stat." + LevelUpStats.MODID + "." + id;
    }

    public String descKey() {
        return "stat." + LevelUpStats.MODID + "." + id + ".desc";
    }

    /** Id used for the attribute modifiers this stat applies. */
    public ResourceLocation modifierId() {
        return ResourceLocation.fromNamespaceAndPath(LevelUpStats.MODID, id);
    }
}
