package com.levelupstats;

import net.minecraft.network.chat.Component;

/**
 * All balance numbers live here so they are easy to tweak.
 */
public final class StatEffects {
    /** Fraction of base walking speed added per Speed level (0.04 = +4%). */
    public static final double SPEED_PER_LEVEL = 0.04;
    /** Fraction of base jump strength added per Jump level (0.05 = +5%). */
    public static final double JUMP_PER_LEVEL = 0.05;
    /** Extra blocks you can fall without damage per Jump level, so high jumps don't hurt. */
    public static final double SAFE_FALL_PER_LEVEL = 0.3;
    /** Melee damage added per Strength level (1.0 = half a heart). */
    public static final double STRENGTH_PER_LEVEL = 0.5;
    /** Max health added per Constitution level (2.0 = one heart). */
    public static final double HEALTH_PER_LEVEL = 2.0;
    /** Value added to the Luck attribute per Luck level (fishing, loot tables using luck). */
    public static final double LUCK_ATTRIBUTE_PER_LEVEL = 0.5;
    /** XP orb multiplier added per Intelligence level (0.5 -> level 2 = x2.0). */
    public static final double XP_MULTIPLIER_PER_LEVEL = 0.5;

    private StatEffects() {
    }

    /** Extra Fortune / Looting levels granted by a Luck level: +1 for every 2 levels (rounded up). */
    public static int fortuneLootingBonus(int luckLevel) {
        return (luckLevel + 1) / 2;
    }

    public static double xpMultiplier(int intelligenceLevel) {
        return 1.0 + XP_MULTIPLIER_PER_LEVEL * intelligenceLevel;
    }

    /** Human readable description of what a stat currently gives at the given level. */
    public static Component describe(StatType type, int level) {
        return switch (type) {
            case SPEED -> Component.translatable(type.descKey(), fmt(level * SPEED_PER_LEVEL * 100));
            case JUMP -> Component.translatable(type.descKey(), fmt(level * JUMP_PER_LEVEL * 100));
            case STRENGTH -> Component.translatable(type.descKey(), fmt(level * STRENGTH_PER_LEVEL));
            case LUCK -> Component.translatable(type.descKey(), fortuneLootingBonus(level), fmt(level * LUCK_ATTRIBUTE_PER_LEVEL));
            case CONSTITUTION -> Component.translatable(type.descKey(), fmt(level * HEALTH_PER_LEVEL / 2.0));
            case INTELLIGENCE -> Component.translatable(type.descKey(), fmt(xpMultiplier(level)));
        };
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
