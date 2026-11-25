# Minefade Mod 0.3.0 — Minecraft Java 1.12.2

Minefade combines conservative client-side performance tools, a shared visual redesign for Minecraft interfaces, and original gameplay content. The existing voxelcore mod ID is intentionally retained so worlds, registry names and the configuration file from the 0.2.0 source line remain compatible.

## Interface redesign

The client GUI theme uses Forge 1.12.2 GUI events to apply a shared visual identity across vanilla screens and compatible mod screens. It re-skins ordinary buttons while preserving their original IDs, hitboxes and parent-screen action handlers. Sliders, image buttons and specialized controls are left intact to avoid breaking interactions. A small Minefade identity strip is drawn in the screen margin.

Minefade entry points are added to the main menu, pause menu and Options screen. The O key opens the control center when no screen is active. The responsive control center contains three tabs:

- **Diagnóstico:** global theme, performance overlay, memory, chunks, entities and tile entities.
- **Rendimiento:** optional distant-creature visual culling, cutoff percentage and minimal-particle mode.
- **Contenido:** toggles and generation values for Resonite ore in newly generated Overworld chunks.

The theme can be disabled from the Diagnostics tab. Settings are persisted in config/voxelcore.cfg.

## Performance and content

- Frame-time/FPS, Java heap, client chunk, entity and tile-entity diagnostics.
- Optional rendering cutoff for distant non-player living entities; disabled by default.
- Optional native minimal-particle setting with restoration of the previous value.
- Resonite ore, crystal, ingot, block, lamp, tools, full armor set and crafting recipes.
- Configurable ore generation in newly generated Overworld chunks.

## Controls

| Key | Action |
|---|---|
| O | Open the Minefade control center when no screen is open. |
| F8 | Toggle the performance monitor. |
| F7 | Toggle distant-creature visual culling. |
| F6 | Toggle minimal particles. |

Keys can be rebound in Options → Controls → Minefade.

## Build and install

Target: Minecraft 1.12.2, Forge 14.23.5.2860, JDK 8, Gradle 4.10.3 and ForgeGradle 2.3.

The repository root contains the GitHub Actions workflow that builds the JAR on pushes and pull requests. It validates resources, compiles the mod and uploads minefade-0.3.0.jar as an Actions artifact. For local builds, run build.sh or build.bat from this project folder. The local output is build/libs/minefade-0.3.0.jar.

Source archives are not installable mods. Copy only the compiled JAR from a successful build into .minecraft/mods, alongside Forge 1.12.2.

## Technical limits

This release does not claim to replace Minecraft's chunk renderer, add parallel chunk generation, provide custom shaders, or rewrite every container's internal layout. It applies a consistent theme to the GUI event surface while leaving specialized controls intact for compatibility. Deeper renderer work will be added only after dedicated Forge runtime testing.
