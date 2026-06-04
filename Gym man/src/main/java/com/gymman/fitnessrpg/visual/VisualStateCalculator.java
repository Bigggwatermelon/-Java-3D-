package com.gymman.fitnessrpg.visual;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.BodyPartProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.progression.XpCurve;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class VisualStateCalculator {
    private final XpCurve xpCurve;
    private final VisualRules rules;
    private final Map<MuscleGroup, MuscleVisualProfile> profiles;

    public VisualStateCalculator(XpCurve xpCurve,
                                 VisualRules rules,
                                 Map<MuscleGroup, MuscleVisualProfile> profiles) {
        this.xpCurve = Objects.requireNonNull(xpCurve);
        this.rules = Objects.requireNonNull(rules);
        this.profiles = Map.copyOf(Objects.requireNonNull(profiles));
    }

    public static VisualStateCalculator defaults(XpCurve xpCurve) {
        return new VisualStateCalculator(xpCurve, VisualRules.defaults(), MuscleVisualProfile.defaults());
    }

    public AvatarVisualState calculate(AvatarProgress progress) {
        Objects.requireNonNull(progress);

        Map<MuscleGroup, BodyPartProgress> snapshot = progress.snapshot(xpCurve);
        EnumMap<MuscleGroup, MuscleVisualState> visualParts = new EnumMap<>(MuscleGroup.class);

        for (MuscleGroup group : MuscleGroup.values()) {
            BodyPartProgress partProgress = snapshot.get(group);
            MuscleVisualProfile profile = profiles.get(group);
            double visibleLevel = partProgress.level();
            double rawGrowth = levelToGrowth(partProgress.level());
            double balancedGrowth = rawGrowth;
            double definition = definitionAmount(profile, balancedGrowth);
            double pump = progress.pump01(group);

            double bulkMorph = profile.maxBulkMorph() * balancedGrowth;
            double definitionMorph = profile.maxDefinitionMorph() * definition;
            Scale3 scale = Scale3.fromDelta(
                    profile.maxScaleXDelta(),
                    profile.maxScaleYDelta(),
                    profile.maxScaleZDelta(),
                    balancedGrowth
            );

            MaterialVisualState material = new MaterialVisualState(
                    clamp01(rules.minimumNormalIntensity() + definition * (1.0 - rules.minimumNormalIntensity())),
                    lerp(0.72, 0.38, definition) - 0.06 * pump,
                    clamp01(0.18 + 0.45 * definition + 0.18 * pump),
                    clamp01(definition * 0.75),
                    pump
            );

            visualParts.put(group, new MuscleVisualState(
                    group,
                    partProgress.level(),
                    visibleLevel,
                    rawGrowth,
                    balancedGrowth,
                    bulkMorph,
                    definitionMorph,
                    scale,
                    material,
                    false
            ));
        }

        return new AvatarVisualState(
                visualParts,
                1.0,
                upperLowerRatio(snapshot)
        );
    }

    private double levelToGrowth(double level) {
        double normalized = Math.max(0.0, level - 1.0) / Math.max(1.0, rules.growthLevelDivisor());
        if (normalized == 0.0) {
            return 0.0;
        }
        return Math.pow(normalized, rules.growthExponent());
    }

    private static double definitionAmount(MuscleVisualProfile profile, double growth) {
        double normalized = (growth - profile.definitionStart01()) / Math.max(0.001, 1.0 - profile.definitionStart01());
        return smoothstep(clamp01(normalized));
    }

    private static double upperLowerRatio(Map<MuscleGroup, BodyPartProgress> snapshot) {
        double upper = (
                snapshot.get(MuscleGroup.CHEST).level()
                        + snapshot.get(MuscleGroup.ARMS).level()
                        + snapshot.get(MuscleGroup.BACK).level()
        ) / 3.0;
        double lower = Math.max(1.0, snapshot.get(MuscleGroup.LEGS).level());
        return upper / lower;
    }

    private static double smoothstep(double value) {
        double x = clamp01(value);
        return x * x * (3.0 - 2.0 * x);
    }

    private static double lerp(double from, double to, double amount01) {
        double amount = clamp01(amount01);
        return from + (to - from) * amount;
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
