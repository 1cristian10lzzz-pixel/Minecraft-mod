# Minefade Mod 0.4.0 — Minecraft Java 1.12.2

Minefade is a Forge 1.12.2 mod combining an interface redesign, performance diagnostics, original Resonite content and a new water/atmosphere presentation layer.

## Realistic water and atmosphere

Version 0.4.0 adds an original client-side water visual system:

- Expanding surface rings when swimmers and boats move through nearby water.
- Water-entry splash particles and subtle wakes.
- Small rain rings on exposed nearby water surfaces.
- Light mist particles near nearby waterfall impacts.
- Biome-aware underwater colour.
- In-game controls for the whole system, wave strength and effect radius.
- Press **K** to toggle Minefade's water effects.

These effects render locally on the client. The mod does not replace vanilla water blocks, alter the server's fluid states, or change saved oceans. The effect radius is capped at 64 blocks and active ripple count is capped to keep the renderer lightweight.

### About the water mod in Rubius's video

The video **"Instalé AGUA REALISTA en Minecraft"** was published by Rubius Z on November 13, 2026. The mod called Realistic Water by sskaffy is built for Fabric 26.3 and requires Vulkan. Its current source has a separate renderer/simulation stack and a custom all-rights-reserved license. It cannot be dropped into Forge 1.12.2 or redistributed as Minefade code.

Minefade 0.4.0 therefore recreates selected visual ideas with original Forge-compatible code. It is not a port of the Fabric/Vulkan mod and does not yet claim FLIP particle simulation, fully finite water, custom liquid blocks or real GPU fluid physics.

For actual finite-fluid mechanics on Forge 1.12.2, a separate community mod, Realistic Finite Fluids, exists; it is not bundled or required by Minefade, and combined use has not been runtime-tested here.

## Interface and settings

The control centre is available from the main menu, pause menu and Options screen, or with **O** when no screen is open. Tabs:

- **INFO:** global interface theme and diagnostic counters.
- **REND.:** optional distant-creature culling and particle quality.
- **AGUA:** water effects, ripples, splashes, rain, waterfall mist, underwater tint, wave strength and radius.
- **MUNDO:** Resonite world generation settings.

## Performance controls

| Key | Action |
|---|---|
| O | Open the Minefade control centre. |
| K | Toggle Minefade water effects. |
| F8 | Toggle the performance monitor. |
| F7 | Toggle distant-creature visual culling. |
| F6 | Toggle minimal particles. |

Keys can be rebound in Options → Controls → Minefade.

## Performance and content

- Frame-time/FPS, Java heap, client chunk, entity and tile-entity diagnostics.
- Optional rendering cutoff for distant non-player living entities; disabled by default.
- Optional native minimal-particle setting with restoration of the previous value.
- Resonite ore, crystal, ingot, block, lamp, tools, full armor set and crafting recipes.
- Configurable ore generation in newly generated Overworld chunks.

## Build and install

Target: Minecraft 1.12.2, Forge 14.23.5.2847, JDK 8, Gradle 4.10.3 and ForgeGradle 2.3.

The repository root contains the GitHub Actions workflow that validates assets, compiles the mod and uploads `minefade-0.4.0.jar` as an Actions artifact. For local builds, run `build.sh` or `build.bat` from this project folder. The local output is `build/libs/minefade-0.4.0.jar`.

Source archives are not installable mods. Copy only the compiled JAR from a successful build into `.minecraft/mods`, alongside Forge 1.12.2.

## Technical limits

This release does not claim to replace Minecraft's chunk renderer, provide Vulkan/FLIP water simulation, or rewrite the full terrain renderer. Its water enhancement is a client-side presentation layer; the vanilla liquid simulation is preserved for compatibility. Deeper renderer and fluid-simulation work requires separate compatibility and in-game performance testing.
