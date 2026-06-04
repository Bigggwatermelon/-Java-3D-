package com.gymman.fitnessrpg;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.progression.AvatarProgressionService;
import com.gymman.fitnessrpg.progression.WorkoutSession;
import com.gymman.fitnessrpg.visual.AvatarVisualState;

public final class FitnessRpgCoreTest {
    private FitnessRpgCoreTest() {
    }

    public static void main(String[] args) {
        independentXpByMuscleGroup();
        overtrainedUpperBodyIsNotVisuallyCapped();
        extremeTrainingProducesHugeLocalScale();
        pumpDecaysOverTime();
        System.out.println("All core tests passed.");
    }

    private static void independentXpByMuscleGroup() {
        AvatarProgress avatar = new AvatarProgress();
        AvatarProgressionService service = AvatarProgressionService.defaults();

        service.logWorkout(avatar, WorkoutSession.single(MuscleGroup.CHEST, 20_000));

        int chestLevel = avatar.progressOf(MuscleGroup.CHEST, service.xpCurve()).level();
        int legLevel = avatar.progressOf(MuscleGroup.LEGS, service.xpCurve()).level();

        require(chestLevel > legLevel, "chest should level independently from legs");
        require(legLevel == 1, "untrained legs should stay at level 1");
    }

    private static void overtrainedUpperBodyIsNotVisuallyCapped() {
        AvatarProgress avatar = new AvatarProgress();
        AvatarProgressionService service = AvatarProgressionService.defaults();

        service.logWorkout(avatar, WorkoutSession.builder()
                .addXp(MuscleGroup.CHEST, 1_000_000)
                .addXp(MuscleGroup.ARMS, 800_000)
                .addXp(MuscleGroup.BACK, 100_000)
                .addXp(MuscleGroup.LEGS, 1_000)
                .build());

        AvatarVisualState visual = service.calculateVisualState(avatar);

        require(!visual.part(MuscleGroup.CHEST).visuallyCapped(), "chest should not be visually capped");
        require(!visual.part(MuscleGroup.ARMS).visuallyCapped(), "arms should not be visually capped");
        require(visual.part(MuscleGroup.CHEST).visibleLevel() == visual.part(MuscleGroup.CHEST).dataLevel(),
                "visible chest level should equal data level");
    }

    private static void extremeTrainingProducesHugeLocalScale() {
        AvatarProgress avatar = new AvatarProgress();
        AvatarProgressionService service = AvatarProgressionService.defaults();

        service.logWorkout(avatar, WorkoutSession.single(MuscleGroup.CHEST, 5_000_000));

        AvatarVisualState visual = service.calculateVisualState(avatar);

        require(visual.part(MuscleGroup.CHEST).localScale().z() >= 5.0,
                "extreme chest training should produce at least 5x depth scale");
        require(visual.part(MuscleGroup.LEGS).localScale().z() == 1.0,
                "untrained legs should stay at base visual scale");
    }

    private static void pumpDecaysOverTime() {
        AvatarProgress avatar = new AvatarProgress();
        AvatarProgressionService service = AvatarProgressionService.defaults();

        service.logWorkout(avatar, WorkoutSession.single(MuscleGroup.ARMS, 6_000));
        double before = avatar.pump01(MuscleGroup.ARMS);
        avatar.decayPump(6.0, 6.0);
        double after = avatar.pump01(MuscleGroup.ARMS);

        require(after < before, "pump should decay over time");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
