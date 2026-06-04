package com.gymman.fitnessrpg.visual;

import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.render.AvatarVisualSink;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record AvatarVisualState(
        Map<MuscleGroup, MuscleVisualState> parts,
        double harmonyScore01,
        double upperLowerVisualRatio
) {
    public AvatarVisualState {
        parts = Collections.unmodifiableMap(new EnumMap<>(parts));
        harmonyScore01 = clamp01(harmonyScore01);
    }

    public MuscleVisualState part(MuscleGroup group) {
        return parts.get(Objects.requireNonNull(group));
    }

    public void applyTo(AvatarVisualSink sink) {
        Objects.requireNonNull(sink);
        for (MuscleVisualState state : parts.values()) {
            MuscleGroup group = state.group();
            sink.setMorphWeight(group.bulkMorphTarget(), state.bulkMorphWeight());
            sink.setMorphWeight(group.definitionMorphTarget(), state.definitionMorphWeight());
            for (String boneName : group.boneNames()) {
                sink.setBoneScale(boneName, state.localScale());
            }
            sink.setMaterialState(group.materialSlot(), state.material());
        }
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
