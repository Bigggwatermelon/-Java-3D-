package com.gymman.fitnessrpg.model;

public record BodyPartProgress(
        MuscleGroup group,
        long totalXp,
        int level,
        long xpIntoLevel,
        long xpForNextLevel
) {
    public double levelProgress01() {
        if (xpForNextLevel <= 0) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) xpIntoLevel / xpForNextLevel));
    }
}
