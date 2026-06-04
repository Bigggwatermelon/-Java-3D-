package com.gymman.fitnessrpg.visual;

public record MaterialVisualState(
        double normalBlend01,
        double roughness,
        double specular01,
        double vascularity01,
        double pump01
) {
    public MaterialVisualState {
        normalBlend01 = clamp01(normalBlend01);
        roughness = Math.min(1.0, Math.max(0.05, roughness));
        specular01 = clamp01(specular01);
        vascularity01 = clamp01(vascularity01);
        pump01 = clamp01(pump01);
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
