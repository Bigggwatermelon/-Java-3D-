package com.gymman.fitnessrpg.progression;

public interface XpCurve {
    int maxLevel();

    int levelForTotalXp(long totalXp);

    long totalXpForLevel(int level);

    long xpToAdvanceFromLevel(int level);
}
