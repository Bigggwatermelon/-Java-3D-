package com.gymman.fitnessrpg;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.progression.AvatarProgressionService;
import com.gymman.fitnessrpg.progression.WorkoutSession;
import com.gymman.fitnessrpg.render.ConsoleAvatarVisualSink;
import com.gymman.fitnessrpg.visual.AvatarVisualState;
import com.gymman.fitnessrpg.visual.MuscleVisualState;

import java.util.Locale;

public final class FitnessRpgDemo {
    private FitnessRpgDemo() {
    }

    public static void main(String[] args) {
        AvatarProgress progress = new AvatarProgress();
        AvatarProgressionService service = AvatarProgressionService.defaults();

        service.logWorkout(progress, WorkoutSession.builder()
                .addXp(MuscleGroup.CHEST, 240_000)
                .addXp(MuscleGroup.ARMS, 210_000)
                .addXp(MuscleGroup.BACK, 95_000)
                .addXp(MuscleGroup.ABS, 40_000)
                .addXp(MuscleGroup.LEGS, 3_000)
                .build());

        AvatarVisualState visualState = service.calculateVisualState(progress);
        printSummary("Extreme upper-body user", visualState);
        visualState.applyTo(new ConsoleAvatarVisualSink());

        service.logWorkout(progress, WorkoutSession.builder()
                .addXp(MuscleGroup.LEGS, 180_000)
                .addXp(MuscleGroup.BACK, 70_000)
                .build());

        AvatarVisualState rebalanced = service.calculateVisualState(progress);
        printSummary("After extreme legs/back workout", rebalanced);
    }

    private static void printSummary(String title, AvatarVisualState visualState) {
        System.out.println();
        System.out.println("== " + title + " ==");
        System.out.printf(Locale.US, "No cap mode, upper/lower ratio %.3f%n",
                visualState.upperLowerVisualRatio());
        for (MuscleGroup group : MuscleGroup.values()) {
            MuscleVisualState part = visualState.part(group);
            System.out.printf(Locale.US,
                    "%-5s dataLv=%3d visibleLv=%6.2f scale=(%.2f, %.2f, %.2f) bulk=%.3f def=%.3f%n",
                    group.displayName(),
                    part.dataLevel(),
                    part.visibleLevel(),
                    part.localScale().x(),
                    part.localScale().y(),
                    part.localScale().z(),
                    part.bulkMorphWeight(),
                    part.definitionMorphWeight());
        }
    }
}
