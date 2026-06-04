package com.gymman.fitnessrpg.visual;

import com.gymman.fitnessrpg.model.MuscleGroup;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record MuscleVisualProfile(
        MuscleGroup group,
        double maxScaleXDelta,
        double maxScaleYDelta,
        double maxScaleZDelta,
        double maxBulkMorph,
        double maxDefinitionMorph,
        double definitionStart01
) {
    public static Map<MuscleGroup, MuscleVisualProfile> defaults() {
        EnumMap<MuscleGroup, MuscleVisualProfile> profiles = new EnumMap<>(MuscleGroup.class);
        profiles.put(MuscleGroup.CHEST, new MuscleVisualProfile(MuscleGroup.CHEST, 1.85, 0.42, 2.65, 8.00, 1.00, 0.05));
        profiles.put(MuscleGroup.ABS, new MuscleVisualProfile(MuscleGroup.ABS, 1.25, 0.18, 1.60, 5.50, 1.00, 0.03));
        profiles.put(MuscleGroup.ARMS, new MuscleVisualProfile(MuscleGroup.ARMS, 2.75, 0.34, 2.75, 9.00, 1.00, 0.04));
        profiles.put(MuscleGroup.BACK, new MuscleVisualProfile(MuscleGroup.BACK, 2.25, 0.46, 2.10, 8.50, 1.00, 0.05));
        profiles.put(MuscleGroup.LEGS, new MuscleVisualProfile(MuscleGroup.LEGS, 2.45, 0.36, 2.45, 9.00, 1.00, 0.04));
        return Collections.unmodifiableMap(profiles);
    }
}
