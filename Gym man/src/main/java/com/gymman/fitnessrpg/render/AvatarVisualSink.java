package com.gymman.fitnessrpg.render;

import com.gymman.fitnessrpg.visual.MaterialVisualState;
import com.gymman.fitnessrpg.visual.Scale3;

public interface AvatarVisualSink {
    void setMorphWeight(String morphTargetName, double weight01);

    void setBoneScale(String boneName, Scale3 scale);

    void setMaterialState(String materialSlotName, MaterialVisualState materialState);
}
