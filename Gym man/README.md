# Gym Man Fitness RPG Core

This project contains the engine-independent Java core for a PC fitness RPG avatar.

## Recommended 3D Stack

- Runtime engine: jMonkeyEngine 3 on top of LWJGL.
- Asset format: glTF 2.0 / GLB exported from Blender.
- Model authoring contract: one skinned humanoid mesh with named morph targets per muscle group:
  `CHEST_BULK`, `CHEST_DEFINITION`, `ABS_BULK`, `ABS_DEFINITION`, `ARMS_BULK`,
  `ARMS_DEFINITION`, `BACK_BULK`, `BACK_DEFINITION`, `LEGS_BULK`, `LEGS_DEFINITION`.
- Material contract: separate material slots for `Mat_Chest`, `Mat_Abs`, `Mat_Arms`,
  `Mat_Back`, and `Mat_Legs`, so each body part can blend normal maps, roughness,
  vascularity, and pump highlights independently.

## Core Idea

The domain layer stores true XP and levels independently for each muscle group.
The visual layer converts those levels into an uncapped `AvatarVisualState`.
Every muscle group can grow independently and extremely. The procedural sandbox solves
attachment points each frame so inflated torso parts push shoulders, hips, arms, and legs
instead of leaving limbs floating or buried.

The renderer should implement `AvatarVisualSink` and map:

- morph weights to glTF morph targets / Blender shape keys,
- bone scales to rig bones when safe,
- material state to PBR shader uniforms or material variants.

## Local Compile

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java/*.java)
java -cp out com.gymman.fitnessrpg.FitnessRpgDemo
```

## Visual Test App

This project also includes a no-asset Swing visualizer. It renders a procedural pseudo-3D
avatar from Java2D primitives, so it works before you have a `.glb` or `.obj` model.
The surface uses a CPU-side procedural normal-map style shader: each body part is rasterized
with dynamic height fields, specular light, fiber grooves, and vein color blending.

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java/*.java) (Get-ChildItem -Recurse src/test/java/*.java)
java -cp out com.gymman.fitnessrpg.ui.FitnessRpgVisualApp
```

Useful checks:

```powershell
java -cp out com.gymman.fitnessrpg.FitnessRpgCoreTest
java -cp out com.gymman.fitnessrpg.ui.FitnessRpgVisualSmokeTest
```

The embeddable component is `com.gymman.fitnessrpg.ui.FitnessRpgVisualizerPanel`.
Another Java UI can instantiate it as a normal `JPanel`, call `addXp(...)` or
`applyWorkout(...)`, and dispose it when the host window closes.
