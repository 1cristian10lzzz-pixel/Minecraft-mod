# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.5.0** · Forge 14.23.5.2847

Minefade is a Forge 1.12.2 mod with a shared interface theme, performance diagnostics, Resonite content and an enhanced client-side water presentation system.

## Minefade Mod 0.5.0 — improved rendering

- All visible ripple rings are batched into one render buffer and one GL draw call per frame.
- Nearby water-surface searches are cached instead of repeated on every client tick.
- Rain and waterfall scans run less frequently.
- Optional adaptive quality uses FPS hysteresis to reduce Minefade-only visual detail when performance remains low, and restores it after recovery.
- Active ripples and cached surfaces are cleared when leaving a world or disabling water effects.

Vanilla water blocks, fluid states and saved-world water are unchanged. This is an original Forge 1.12.2 presentation layer, not a port of the Fabric/Vulkan Realistic Water project.

## Build status

**Forge build passed:** [GitHub Actions run #40](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45701481080).

- JAR: `minefade-0.5.0.jar`
- Size: 70,999 bytes
- SHA-256: `890ea6deff74db2a791fe2d6fd9a0506543a138f7b016537513f0d2b0225d2a3`
- GitHub artifact: `minefade-mod-minecraft-1.12.2-v0.5.0` (artifact ID 15063167659)

The downloaded JAR passed CRC/integrity checks and validation of the manifest, `mcmod.info`, version, Minecraft target and required compiled classes. It has not been launched inside a real Minecraft client in this environment.

## Latest source

- [Open the complete source project](./VoxelCore-1.12.2/)
- [Realism and water system notes](./VoxelCore-1.12.2/README.md)
- [Change log](./VoxelCore-1.12.2/CHANGELOG.md)
- [Build and integrity record](./VoxelCore-1.12.2/BUILD_STATUS.txt)

## Build and install

The repository uses GitHub Actions with Temurin JDK 8, Gradle 4.10.3 and ForgeGradle 2.3. Install the compiled JAR into `.minecraft/mods`, alongside Minecraft Java 1.12.2 and Forge 14.23.5.2847. A source archive is not an installable mod.

The internal mod ID remains `voxelcore` for registry and configuration compatibility.
