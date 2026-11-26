# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.8.0** · Forge 14.23.5.2847

Minefade focuses on the legacy client rendering path, water appearance, player animation, dropped items, performance diagnostics and Resonite content.

## Minefade Mod 0.8.0

- **Realistic water surface:** the old ring-only effect is removed. Exposed water surfaces are covered by a custom procedural mesh with small continuous height changes, biome/depth colour absorption, view-angle sky reflection approximation, specular highlights and shoreline foam.
- **Underwater presentation:** deeper red/green light absorption, custom fog distance and rising air bubbles. Entry splashes, wakes and waterfall mist remain as small local effects.
- **Preserved fluid simulation:** Minecraft's water blocks, levels, flow, collision, source/flowing states and server simulation are not replaced. The mod changes the client-side appearance and fog only.
- **Performance:** surface positions are cached and rebuilt periodically; the mesh is batched; adaptive quality lowers surface detail when FPS remains low.
- **Dropped items:** fluid buoyancy is damped near the surface to reduce bobbing jitter while retaining existing server-side pickup rules.
- **Player animation:** adds alternating leg kicks while swimming to the existing additive player animation.
- **Assets and interfaces:** all existing PNGs are kept untouched. No images/textures were created, modified or added. No GUI or interface files were changed.

## Water rendering limits

The new surface uses procedural geometry and a sky-colour/specular approximation. It is a substantial change from the flat vanilla top-face appearance, but it is not full screen-space reflection, physically accurate refraction or a new finite-water simulation. A fully reflective shader path would require deeper framebuffer/chunk-renderer integration and has higher compatibility risk on Forge 1.12.2.

## Build status

The 0.8.0 asset validation and ForgeGradle build are pending. Runtime tests inside a real Minecraft client and dedicated/LAN server are separate from compilation.

## Install

Copy the finished `Minefade-Mod-0.8.0-Forge-1.12.2.jar` into `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains `voxelcore`.
