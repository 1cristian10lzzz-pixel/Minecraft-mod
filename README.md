# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.7.1** · Forge 14.23.5.2847

Minefade adds a shared interface theme, performance diagnostics, Resonite content, client-side water effects, natural player animation and improved dropped-item handling.

## Minefade Mod 0.7.1

- **Player animation:** subtle secondary motion is applied after Minecraft calculates its standard pose. Default and slim skins and their overlays are preserved.
- **Dropped items:** one smaller baked model represents each stack on the floor. The item atlas is explicitly bound before rendering.
- **Pickup reliability:** a server-side fallback delegates to Minecraft's own EntityItem pickup method, retaining inventory capacity and pickup-delay rules.
- **Water:** surface checks run less often; smooth ripple rings and adaptive quality remain client-side. Vanilla water blocks and simulation are unchanged.
- **Performance:** optional entity culling remains configurable; liquid buoyancy processing is throttled; the vanilla chunk renderer is untouched.
- **Assets:** all existing PNG textures and icons are preserved. No new or modified images are included.

Mo' Bends, Realistica and Realistic Finite Fluids are references only; no external mod code or resource-pack textures are bundled.

## Build status

The 0.7.1 build is pending GitHub Actions resource validation and ForgeGradle compilation. The JAR will only be distributed after those checks pass. Runtime tests inside a real Minecraft client/server remain separate from compile validation.

## Install

Copy the finished `Minefade-Mod-0.7.1-Forge-1.12.2.jar` into `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains `voxelcore`.
