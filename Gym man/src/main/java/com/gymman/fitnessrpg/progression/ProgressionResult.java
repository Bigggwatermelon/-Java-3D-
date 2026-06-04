package com.gymman.fitnessrpg.progression;

import com.gymman.fitnessrpg.visual.AvatarVisualState;

import java.util.List;

public record ProgressionResult(
        List<LevelChange> levelChanges,
        AvatarVisualState visualState
) {
    public boolean hasLevelUps() {
        return levelChanges.stream().anyMatch(LevelChange::leveledUp);
    }
}
