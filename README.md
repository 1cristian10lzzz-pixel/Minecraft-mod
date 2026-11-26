# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.7.1** · Forge 14.23.5.2847

Minefade adds a shared interface theme, performance diagnostics, Resonite content, client-side water effects, natural player animation and improved dropped-item handling.

## Minefade Mod 0.7.1

- **Player animation:** subtle secondary motion is applied after Minecraft calculates its standard pose. Default and slim skins and their overlays are preserved. The model is cached per renderer to avoid allocation every frame.
- **Dropped items:** one smaller baked model represents each stack on the floor. The vanilla item atlas is explicitly bound and the original GROUND transform is retained.
- **Pickup reliability:** a server-side fallback delegates to Minecraft's own EntityItem pickup method, retaining inventory capacity and pickup-delay rules.
- **Water:** surface checks run less often; smooth ripple rings and adaptive quality remain client-side. Vanilla water blocks and simulation are unchanged.
- **Performance:** optional entity culling remains configurable; liquid buoyancy processing is throttled; the vanilla chunk renderer is untouched.
- **Assets:** all existing PNG textures and icons are preserved. No new or modified images are included.

Mo' Bends, Realistica and Realistic Finite Fluids are references only; no external mod code or resource-pack textures are bundled.

## Build status

**Forge compilation and asset validation passed:** [GitHub Actions run #53](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45838678259).

- JAR: `Minefade-Mod-0.7.1-Forge-1.12.2.jar`
- Size: 101,998 bytes
- SHA-256: `949ab73eae7c68f374f14d32a2926dc0335a2bfaf691fa48249f928493f675ed`
- Artifact: `minefade-mod-minecraft-1.12.2-v0.7.1` (ID 15126578103)

The JAR passed archive/CRC and metadata checks and contains the new classes. Actual Minecraft client and dedicated/LAN runtime tests have not been performed in this environment.

## Install

Copy `Minefade-Mod-0.7.1-Forge-1.12.2.jar` into `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains `voxelcore`.
