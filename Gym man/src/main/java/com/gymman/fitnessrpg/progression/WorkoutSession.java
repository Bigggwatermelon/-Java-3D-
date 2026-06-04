package com.gymman.fitnessrpg.progression;

import com.gymman.fitnessrpg.model.MuscleGroup;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class WorkoutSession {
    private final EnumMap<MuscleGroup, Long> xpByGroup;

    private WorkoutSession(EnumMap<MuscleGroup, Long> xpByGroup) {
        this.xpByGroup = new EnumMap<>(xpByGroup);
    }

    public static WorkoutSession single(MuscleGroup group, long xp) {
        return builder().addXp(group, xp).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<MuscleGroup, Long> xpByGroup() {
        return Collections.unmodifiableMap(xpByGroup);
    }

    public static final class Builder {
        private final EnumMap<MuscleGroup, Long> xpByGroup = new EnumMap<>(MuscleGroup.class);

        private Builder() {
            for (MuscleGroup group : MuscleGroup.values()) {
                xpByGroup.put(group, 0L);
            }
        }

        public Builder addXp(MuscleGroup group, long xp) {
            Objects.requireNonNull(group);
            if (xp < 0) {
                throw new IllegalArgumentException("xp must be >= 0");
            }
            xpByGroup.merge(group, xp, Math::addExact);
            return this;
        }

        public Builder addExerciseVolume(MuscleGroup group,
                                         int sets,
                                         int repsPerSet,
                                         double loadKg,
                                         double effort01) {
            Objects.requireNonNull(group);
            if (sets < 0 || repsPerSet < 0 || loadKg < 0.0) {
                throw new IllegalArgumentException("sets, reps and load must be >= 0");
            }
            if (effort01 < 0.0 || effort01 > 1.0) {
                throw new IllegalArgumentException("effort01 must be within [0, 1]");
            }

            double safeLoad = Math.max(20.0, loadKg);
            long xp = Math.round(sets * repsPerSet * safeLoad * (0.35 + effort01));
            return addXp(group, xp);
        }

        public WorkoutSession build() {
            return new WorkoutSession(xpByGroup);
        }
    }
}
