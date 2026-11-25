# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.5.0** · Forge 14.23.5.2847

Minefade is a Forge 1.12.2 mod with a shared interface theme, performance diagnostics, Resonite content and an enhanced client-side water presentation system.

## Minefade Mod 0.5.0 — improved rendering

The 0.5.0 source improves the water effects renderer and reduces unnecessary client work:

- All visible ripple rings are batched into one render buffer and one GL draw call per frame.
- Nearby water-surface searches are cached instead of repeated on every client tick.
- Rain and waterfall scans run less frequently.
- Optional adaptive quality uses FPS hysteresis to reduce Minefade-only visual detail when performance remains low, and restores it after recovery.
- Active ripples and cached surfaces are cleared when leaving a world or disabling water effects.

Vanilla water blocks, fluid states and saved-world water are unchanged. This remains an original Forge 1.12.2 presentation layer, not a port of the Fabric/Vulkan Realistic Water project.

## Build status

The 0.5.0 source has been committed. Its own Forge build and JAR validation must pass before the installable file is considered ready. The previous 0.4.0 build passed, but that result does not substitute for the new build.

## Latest source

- [Open the complete source project](./VoxelCore-1.12.2/)
- [Realism and water system notes](./VoxelCore-1.12.2/README.md)
- [Change log](./VoxelCore-1.12.2/CHANGELOG.md)
- [Build and integrity record](./VoxelCore-1.12.2/BUILD_STATUS.txt)

## Build and install

The repository uses GitHub Actions with Temurin JDK 8, Gradle 4.10.3 and ForgeGradle 2.3. Install only the compiled JAR from the successful 0.5.0 run into `.minecraft/mods`, alongside Minecraft Java 1.12.2 and Forge 14.23.5.2847. A source archive is not an installable mod.

The internal mod ID remains `voxelcore` for registry and configuration compatibility.
