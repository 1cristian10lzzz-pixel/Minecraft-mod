# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.4.0** · Forge 14.23.5.2847

Minefade is a Forge 1.12.2 mod combining a shared interface theme, performance diagnostics, original Resonite content and new realistic water presentation effects.

## Minefade Mod 0.4.0 — compiled JAR

**Build successful:** [GitHub Actions run #36](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45698663718).

The run compiled with JDK 8, Gradle 4.10.3 and ForgeGradle 2.3, validated the assets, and uploaded the installable JAR as artifact **minefade-mod-minecraft-1.12.2-v0.4.0**. Download it from the run's **Artifacts** section. The artifact is retained for 30 days.

- JAR: `minefade-0.4.0.jar`
- Size: 69,472 bytes
- SHA-256: `2b4bf20a48e253b4615fbaa05c5771ea5912a1ad5825ab3c5126f524d124dbc8`

The JAR passed archive-integrity and metadata checks. A launch inside a real Minecraft client has not yet been performed in the build environment.

## 0.4.0 — Water and environmental realism

- New client-side water surface rings, wakes, splashes and rain impacts.
- Light mist at nearby waterfall impacts.
- Biome-aware underwater tint.
- In-game Agua tab with on/off switches, intensity and effect radius.
- Press **K** to toggle water visuals.
- Original Forge 1.12.2 code; vanilla/server water states are not replaced.

The Rubius video "Instalé AGUA REALISTA en Minecraft" features a modern Fabric/Vulkan project. That project cannot be installed in this Forge 1.12.2 mod. Minefade implements selected visual concepts independently; it does not include or redistribute the original project's code/assets.

## Latest source

- [Open the complete source project](./VoxelCore-1.12.2/)
- [Realism and water system notes](./VoxelCore-1.12.2/README.md)
- [Change log](./VoxelCore-1.12.2/CHANGELOG.md)
- [Build and integrity record](./VoxelCore-1.12.2/BUILD_STATUS.txt)

## Build and install

The repository includes a GitHub Actions build using Temurin JDK 8 and Gradle 4.10.3. It validates resources, builds against Forge 14.23.5.2847 and uploads the compiled JAR as an Actions artifact.

Install only the compiled JAR from a successful run into `.minecraft/mods`; source archives are not installable mods.

## Compatibility

The internal mod ID remains `voxelcore` to preserve existing registry names and configuration from the 0.2.x/0.3.x source line.

Minecraft 1.12.2 · Forge 14.23.5.2847 · Java 8 · ForgeGradle 2.3.
