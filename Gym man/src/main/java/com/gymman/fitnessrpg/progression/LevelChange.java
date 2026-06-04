package com.gymman.fitnessrpg.progression;

import com.gymman.fitnessrpg.model.MuscleGroup;

public record LevelChange(MuscleGroup group, int beforeLevel, int afterLevel) {
    public boolean leveledUp() {
        return afterLevel > beforeLevel;
    }
}
