package com.gymman.fitnessrpg.visual;

public record VisualRules(
        double growthLevelDivisor,
        double growthExponent,
        double minimumNormalIntensity,
        double pumpVisualScaleBoost,
        double pumpHalfLifeHours
) {
    public static VisualRules defaults() {
        return new VisualRules(
                24.0,
                1.35,
                0.16,
                0.14,
                6.0
        );
    }
}
