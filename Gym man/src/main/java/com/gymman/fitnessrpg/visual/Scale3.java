package com.gymman.fitnessrpg.visual;

public record Scale3(double x, double y, double z) {
    public static Scale3 identity() {
        return new Scale3(1.0, 1.0, 1.0);
    }

    public static Scale3 fromDelta(double dx, double dy, double dz, double amount01) {
        double amount = Math.max(0.0, amount01);
        return new Scale3(1.0 + dx * amount, 1.0 + dy * amount, 1.0 + dz * amount);
    }

    public Scale3 multiply(double factor) {
        return new Scale3(x * factor, y * factor, z * factor);
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
