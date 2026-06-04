package com.gymman.fitnessrpg.progression;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.BodyPartProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.visual.AvatarVisualState;
import com.gymman.fitnessrpg.visual.VisualStateCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AvatarProgressionService {
    private final XpCurve xpCurve;
    private final VisualStateCalculator visualStateCalculator;

    public AvatarProgressionService(XpCurve xpCurve, VisualStateCalculator visualStateCalculator) {
        this.xpCurve = Objects.requireNonNull(xpCurve);
        this.visualStateCalculator = Objects.requireNonNull(visualStateCalculator);
    }

    public static AvatarProgressionService defaults() {
        XpCurve xpCurve = RpgXpCurve.defaultCurve();
        return new AvatarProgressionService(xpCurve, VisualStateCalculator.defaults(xpCurve));
    }

    public ProgressionResult logWorkout(AvatarProgress progress, WorkoutSession workoutSession) {
        Objects.requireNonNull(progress);
        Objects.requireNonNull(workoutSession);

        Map<MuscleGroup, BodyPartProgress> before = progress.snapshot(xpCurve);
        for (Map.Entry<MuscleGroup, Long> entry : workoutSession.xpByGroup().entrySet()) {
            long xp = entry.getValue();
            if (xp > 0) {
                progress.addXp(entry.getKey(), xp);
                progress.addPump(entry.getKey(), pumpFromXp(xp));
            }
        }

        Map<MuscleGroup, BodyPartProgress> after = progress.snapshot(xpCurve);
        List<LevelChange> changes = new ArrayList<>();
        for (MuscleGroup group : MuscleGroup.values()) {
            changes.add(new LevelChange(group, before.get(group).level(), after.get(group).level()));
        }

        AvatarVisualState visualState = visualStateCalculator.calculate(progress);
        return new ProgressionResult(List.copyOf(changes), visualState);
    }

    public AvatarVisualState calculateVisualState(AvatarProgress progress) {
        return visualStateCalculator.calculate(progress);
    }

    public XpCurve xpCurve() {
        return xpCurve;
    }

    private static double pumpFromXp(long xp) {
        return Math.min(0.45, xp / 6000.0);
    }
}
