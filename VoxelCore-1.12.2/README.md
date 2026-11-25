# Minefade Mod 0.5.0 — Minecraft Java 1.12.2

Minefade is a Forge 1.12.2 mod combining an interface redesign, performance diagnostics, original Resonite content and a client-side water/atmosphere presentation layer.

## Realistic water and atmosphere

Version 0.5.0 improves the original client-side water visual system:

- Expanding surface rings when swimmers and boats move through nearby water, now batched into one draw call per frame.
- Water-entry splash particles and subtle wakes.
- Small rain rings on exposed nearby water surfaces.
- Light mist particles near nearby waterfall impacts.
- Biome-aware underwater colour.
- In-game controls for the whole system, wave strength, effect radius and automatic quality adaptation.
- Cached surface scans, less frequent waterfall checks and a lower-cost fallback when FPS remains low.
- Press **K** to toggle Minefade's water effects.

These effects render locally on the client. The mod does not replace vanilla water blocks, alter the server's fluid states, or change saved oceans. The effect radius is capped at 64 blocks and active ripple count is capped to keep the renderer lightweight.

### About the water mod in Rubius's video

The video **"Instalé AGUA REALISTA en Minecraft"** was published by Rubius Z on November 13, 2026. The mod called Realistic Water by sskaffy is built for Fabric 26.3 and requires Vulkan. It cannot be installed directly in this Forge 1.12.2 mod; Minefade recreates selected visual ideas with original Forge-compatible code, not a port or redistributed copy.

Minefade does not claim FLIP particle simulation, fully finite water, custom liquid blocks or real GPU fluid physics. Realistic Finite Fluids is a separate community mod and is not bundled or required by Minefade; combined use has not been runtime-tested.

## Interface and settings

The control centre is available from the main menu, pause menu and Options screen, or with **O** when no screen is open. Tabs:

- **INFO:** global interface theme and diagnostic counters.
- **REND.:** optional distant-creature culling and particle quality.
- **AGUA:** water effects, ripples, splashes, rain, waterfall mist, underwater tint, wave strength, radius and adaptive quality.
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

## Build, verify and install

**Forge build passed:** [GitHub Actions run #40](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45701481080).

- JAR: `minefade-0.5.0.jar`
- Size: 70,999 bytes
- SHA-256: `890ea6deff74db2a791fe2d6fd9a0506543a138f7b016537513f0d2b0225d2a3`

Target: Minecraft 1.12.2, Forge 14.23.5.2847, JDK 8, Gradle 4.10.3 and ForgeGradle 2.3.

The downloaded JAR passed ZIP/CRC integrity and metadata checks. Copy only `minefade-0.5.0.jar` into `.minecraft/mods`, alongside Forge 1.12.2.

## Technical limits

This release does not replace Minecraft's chunk renderer or provide Vulkan/FLIP water simulation. Its water enhancement is a client-side presentation layer; vanilla liquid simulation is preserved for compatibility. A real Minecraft client launch and in-game performance/compatibility test have not been performed in this environment.
