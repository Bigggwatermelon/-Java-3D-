package com.gymman.fitnessrpg.ui;

import com.gymman.fitnessrpg.model.MuscleGroup;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class FitnessRpgVisualSmokeTest {
    private FitnessRpgVisualSmokeTest() {
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        FitnessRpgVisualizerPanel panel = new FitnessRpgVisualizerPanel();
        try {
            panel.setSize(1180, 760);
            panel.doLayout();
            panel.addXp(MuscleGroup.CHEST, 250_000);
            panel.addXp(MuscleGroup.ARMS, 180_000);
            panel.addXp(MuscleGroup.LEGS, 5_000);

            BufferedImage image = new BufferedImage(1180, 760, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                panel.paint(graphics);
            } finally {
                graphics.dispose();
            }

            require(hasPaintedPixels(image), "visual panel did not paint meaningful pixels");
            System.out.println("Visual smoke test passed.");
        } finally {
            panel.dispose();
        }
    }

    private static boolean hasPaintedPixels(BufferedImage image) {
        int changedPixels = 0;
        for (int y = 0; y < image.getHeight(); y += 8) {
            for (int x = 0; x < image.getWidth(); x += 8) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    changedPixels++;
                }
            }
        }
        return changedPixels > 1_000;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
