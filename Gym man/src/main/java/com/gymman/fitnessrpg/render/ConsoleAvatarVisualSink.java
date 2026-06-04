package com.gymman.fitnessrpg.render;

import com.gymman.fitnessrpg.visual.MaterialVisualState;
import com.gymman.fitnessrpg.visual.Scale3;

import java.util.Locale;

public final class ConsoleAvatarVisualSink implements AvatarVisualSink {
    @Override
    public void setMorphWeight(String morphTargetName, double weight01) {
        System.out.printf(Locale.US, "morph %-18s -> %.3f%n", morphTargetName, weight01);
    }

    @Override
    public void setBoneScale(String boneName, Scale3 scale) {
        System.out.printf(Locale.US, "bone  %-18s -> scale(%.3f, %.3f, %.3f)%n",
                boneName, scale.x(), scale.y(), scale.z());
    }

    @Override
    public void setMaterialState(String materialSlotName, MaterialVisualState materialState) {
        System.out.printf(Locale.US,
                "mat   %-18s -> normal=%.3f rough=%.3f spec=%.3f veins=%.3f pump=%.3f%n",
                materialSlotName,
                materialState.normalBlend01(),
                materialState.roughness(),
                materialState.specular01(),
                materialState.vascularity01(),
                materialState.pump01());
    }
}
