package com.gymman.fitnessrpg.model;

import com.gymman.fitnessrpg.progression.XpCurve;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class AvatarProgress {
    private final EnumMap<MuscleGroup, Long> totalXpByGroup = new EnumMap<>(MuscleGroup.class);
    private final EnumMap<MuscleGroup, Double> pumpByGroup = new EnumMap<>(MuscleGroup.class);

    public AvatarProgress() {
        for (MuscleGroup group : MuscleGroup.values()) {
            totalXpByGroup.put(group, 0L);
            pumpByGroup.put(group, 0.0);
        }
    }

    public long totalXp(MuscleGroup group) {
        return totalXpByGroup.get(Objects.requireNonNull(group));
    }

    public double pump01(MuscleGroup group) {
        return pumpByGroup.get(Objects.requireNonNull(group));
    }

    public BodyPartProgress progressOf(MuscleGroup group, XpCurve xpCurve) {
        Objects.requireNonNull(group);
        Objects.requireNonNull(xpCurve);

        long totalXp = totalXp(group);
        int level = xpCurve.levelForTotalXp(totalXp);
        long levelStartXp = xpCurve.totalXpForLevel(level);
        long nextLevelXp = xpCurve.totalXpForLevel(Math.min(xpCurve.maxLevel(), level + 1));
        long xpForNext = level >= xpCurve.maxLevel() ? 0L : nextLevelXp - levelStartXp;
        long xpIntoLevel = level >= xpCurve.maxLevel() ? 0L : totalXp - levelStartXp;

        return new BodyPartProgress(group, totalXp, level, xpIntoLevel, xpForNext);
    }

    public Map<MuscleGroup, BodyPartProgress> snapshot(XpCurve xpCurve) {
        EnumMap<MuscleGroup, BodyPartProgress> snapshot = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup group : MuscleGroup.values()) {
            snapshot.put(group, progressOf(group, xpCurve));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public Map<MuscleGroup, Long> xpSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(totalXpByGroup));
    }

    public void addXp(MuscleGroup group, long xp) {
        Objects.requireNonNull(group);
        if (xp < 0) {
            throw new IllegalArgumentException("xp must be >= 0");
        }
        totalXpByGroup.merge(group, xp, Math::addExact);
    }

    public void addPump(MuscleGroup group, double amount01) {
        Objects.requireNonNull(group);
        pumpByGroup.merge(group, amount01, (oldValue, amount) -> clamp01(oldValue + amount));
    }

    public void decayPump(double hours, double halfLifeHours) {
        if (hours < 0.0) {
            throw new IllegalArgumentException("hours must be >= 0");
        }
        if (halfLifeHours <= 0.0) {
            throw new IllegalArgumentException("halfLifeHours must be > 0");
        }

        double factor = Math.pow(0.5, hours / halfLifeHours);
        for (MuscleGroup group : MuscleGroup.values()) {
            pumpByGroup.put(group, clamp01(pumpByGroup.get(group) * factor));
        }
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
