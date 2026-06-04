package com.gymman.fitnessrpg.model;

import java.util.List;

public enum MuscleGroup {
    CHEST("Chest", "CHEST_BULK", "CHEST_DEFINITION", "Mat_Chest",
            List.of("Chest", "UpperChest.L", "UpperChest.R")),
    ABS("Abs", "ABS_BULK", "ABS_DEFINITION", "Mat_Abs",
            List.of("Spine", "Abs.Upper", "Abs.Lower")),
    ARMS("Arms", "ARMS_BULK", "ARMS_DEFINITION", "Mat_Arms",
            List.of("UpperArm.L", "UpperArm.R", "Forearm.L", "Forearm.R")),
    BACK("Back", "BACK_BULK", "BACK_DEFINITION", "Mat_Back",
            List.of("UpperBack", "Lat.L", "Lat.R")),
    LEGS("Legs", "LEGS_BULK", "LEGS_DEFINITION", "Mat_Legs",
            List.of("Thigh.L", "Thigh.R", "Calf.L", "Calf.R"));

    private final String displayName;
    private final String bulkMorphTarget;
    private final String definitionMorphTarget;
    private final String materialSlot;
    private final List<String> boneNames;

    MuscleGroup(String displayName,
                String bulkMorphTarget,
                String definitionMorphTarget,
                String materialSlot,
                List<String> boneNames) {
        this.displayName = displayName;
        this.bulkMorphTarget = bulkMorphTarget;
        this.definitionMorphTarget = definitionMorphTarget;
        this.materialSlot = materialSlot;
        this.boneNames = boneNames;
    }

    public String displayName() {
        return displayName;
    }

    public String bulkMorphTarget() {
        return bulkMorphTarget;
    }

    public String definitionMorphTarget() {
        return definitionMorphTarget;
    }

    public String materialSlot() {
        return materialSlot;
    }

    public List<String> boneNames() {
        return boneNames;
    }

    public boolean isUpperBody() {
        return this == CHEST || this == ARMS || this == BACK;
    }
}
