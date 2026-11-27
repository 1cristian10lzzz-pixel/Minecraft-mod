# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.8.0** · Forge 14.23.5.2847

Minefade adds a shared interface theme, performance diagnostics, Resonite content, a custom water-surface renderer, natural player animation and improved dropped-item handling.

## Minefade Mod 0.8.0 — agua y renderizado

- **Agua:** Minefade now replaces the visible top-surface pass inside Minecraft's existing fluid renderer. The mesh is smoothly subdivided, uses the biome water colour and a bounded depth estimate, and adds broad, irregular light glints without moving the surface geometry.
- **Sin anillos:** expanding circular ripple geometry has been removed. Swimming, boats and rain use small contact-splash particles instead; waterfall mist and underwater colour remain.
- **Compatibilidad:** vanilla water blocks, block IDs, level metadata, collision, flow, buckets and server-side fluid simulation are preserved. Other fluids continue through the original renderer.
- **Rendimiento:** adaptive quality falls back from a 3-by-3 top mesh to a single quad while FPS remains low. Water surface scans and ambient particle scans remain throttled.
- **Interfaz y recursos:** no GUI or interface code was changed. Existing PNG assets are preserved; no images or resource-pack textures were created or modified.

The new surface is a renderer-level visual improvement, not a full physically based shader: it does not claim screen-space reflections, refraction, or a new fluid simulation.

## Previous version

The 0.7.1 build passed Forge compilation and asset validation in [GitHub Actions run #53](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45838678259). That check applies to 0.7.1 only; the 0.8.0 branch must pass its own build before its JAR is considered verified.

## Build and install

Target: Minecraft Java 1.12.2 with Forge 14.23.5.2847 and Java 8.

The CI workflow builds the installable JAR as "minefade-0.8.0.jar" and uploads it as "minefade-mod-minecraft-1.12.2-v0.8.0". Runtime testing inside a Minecraft client, dedicated server and LAN world remains necessary after compilation.

The internal mod ID remains "voxelcore".
