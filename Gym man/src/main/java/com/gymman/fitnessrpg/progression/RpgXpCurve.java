package com.gymman.fitnessrpg.progression;

public final class RpgXpCurve implements XpCurve {
    private final int maxLevel;
    private final int baseXp;
    private final double exponent;
    private final long[] totalXpForLevel;

    public RpgXpCurve(int maxLevel, int baseXp, double exponent) {
        if (maxLevel < 2) {
            throw new IllegalArgumentException("maxLevel must be >= 2");
        }
        if (baseXp <= 0) {
            throw new IllegalArgumentException("baseXp must be > 0");
        }
        if (exponent < 1.0) {
            throw new IllegalArgumentException("exponent must be >= 1.0");
        }

        this.maxLevel = maxLevel;
        this.baseXp = baseXp;
        this.exponent = exponent;
        this.totalXpForLevel = buildTotalXpTable(maxLevel, baseXp, exponent);
    }

    public static RpgXpCurve defaultCurve() {
        return new RpgXpCurve(10_000, 100, 1.45);
    }

    @Override
    public int maxLevel() {
        return maxLevel;
    }

    @Override
    public int levelForTotalXp(long totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("totalXp must be >= 0");
        }

        int level = 1;
        while (level < maxLevel && totalXp >= totalXpForLevel[level + 1]) {
            level++;
        }
        return level;
    }

    @Override
    public long totalXpForLevel(int level) {
        if (level < 1 || level > maxLevel) {
            throw new IllegalArgumentException("level must be between 1 and " + maxLevel);
        }
        return totalXpForLevel[level];
    }

    @Override
    public long xpToAdvanceFromLevel(int level) {
        if (level < 1 || level >= maxLevel) {
            return 0L;
        }
        return totalXpForLevel[level + 1] - totalXpForLevel[level];
    }

    private static long[] buildTotalXpTable(int maxLevel, int baseXp, double exponent) {
        long[] table = new long[maxLevel + 1];
        table[1] = 0L;
        for (int level = 1; level < maxLevel; level++) {
            long step = Math.max(1L, Math.round(baseXp * Math.pow(level, exponent)));
            table[level + 1] = Math.addExact(table[level], step);
        }
        return table;
    }
}
