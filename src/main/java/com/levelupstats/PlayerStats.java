package com.levelupstats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-player stat levels plus prestige. Stored as a data attachment on the player.
 */
public class PlayerStats {
    /** Starting maximum level for every stat. */
    public static final int BASE_CAP = 10;
    /** How much the cap rises per prestige. */
    public static final int CAP_PER_PRESTIGE = 5;

    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().optionalFieldOf("levels", List.of()).forGetter(PlayerStats::levelList),
            Codec.INT.optionalFieldOf("prestige", 0).forGetter(PlayerStats::getPrestige)
    ).apply(instance, PlayerStats::new));

    private final int[] levels = new int[StatType.values().length];
    private int prestige;

    public PlayerStats() {
    }

    private PlayerStats(List<Integer> savedLevels, int prestige) {
        for (int i = 0; i < levels.length && i < savedLevels.size(); i++) {
            levels[i] = Math.max(0, savedLevels.get(i));
        }
        this.prestige = Math.max(0, prestige);
    }

    private List<Integer> levelList() {
        List<Integer> list = new ArrayList<>(levels.length);
        for (int level : levels) {
            list.add(level);
        }
        return list;
    }

    public int get(StatType type) {
        return levels[type.ordinal()];
    }

    public void set(StatType type, int level) {
        levels[type.ordinal()] = level;
    }

    public int getPrestige() {
        return prestige;
    }

    /** Current maximum level for each stat. */
    public int cap() {
        return BASE_CAP + prestige * CAP_PER_PRESTIGE;
    }

    /** XP levels needed to raise this stat by one: the Nth point costs N levels. */
    public int costFor(StatType type) {
        return get(type) + 1;
    }

    public boolean isMaxed(StatType type) {
        return get(type) >= cap();
    }

    public boolean allMaxed() {
        for (StatType type : StatType.values()) {
            if (!isMaxed(type)) {
                return false;
            }
        }
        return true;
    }

    public void prestige() {
        prestige++;
    }

    public PlayerStats copy() {
        PlayerStats copy = new PlayerStats();
        System.arraycopy(levels, 0, copy.levels, 0, levels.length);
        copy.prestige = prestige;
        return copy;
    }

    public void write(ByteBuf buf) {
        buf.writeInt(prestige);
        for (int level : levels) {
            buf.writeInt(level);
        }
    }

    public static PlayerStats read(ByteBuf buf) {
        PlayerStats stats = new PlayerStats();
        stats.prestige = buf.readInt();
        for (int i = 0; i < stats.levels.length; i++) {
            stats.levels[i] = buf.readInt();
        }
        return stats;
    }
}
