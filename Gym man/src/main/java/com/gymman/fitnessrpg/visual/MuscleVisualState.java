package com.gymman.fitnessrpg.visual;

import com.gymman.fitnessrpg.model.MuscleGroup;

public record MuscleVisualState(
        MuscleGroup group,
        int dataLevel,
        double visibleLevel,
        double rawGrowth01,
        double balancedGrowth01,
        double bulkMorphWeight,
        double definitionMorphWeight,
        Scale3 localScale,
        MaterialVisualState material,
        boolean visuallyCapped
) {
}
