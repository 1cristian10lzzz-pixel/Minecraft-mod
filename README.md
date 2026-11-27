# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.8.0** · Forge 14.23.5.2847

Minefade focuses on the legacy client rendering path, water appearance, player animation, dropped items, performance diagnostics and Resonite content.

## Minefade Mod 0.8.0

- **Custom realistic water surface:** the old ring-only effect is removed. A batched procedural mesh covers exposed Minecraft water with smoothly shared corner heights, biome-aware colour, depth absorption, sky reflection, view-angle Fresnel response, sunlight highlights and shoreline foam.
- **No geometric waves:** the water surface does not rise and fall, and there are no expanding circular rings. Reflections and highlights respond to the sky, camera angle and daylight; small splash/bubble particles remain local contact effects.
- **Underwater presentation:** deeper red/green light absorption, custom fog density and rising air bubbles. Entry splashes, wakes and waterfall mist remain.
- **Preserved fluid simulation:** Minecraft's existing water blocks, levels, flow, collision, source/flowing states, buckets and server simulation are not replaced. Minefade changes the client-side appearance and underwater presentation only.
- **Performance:** nearby surface positions are cached, rendered in one batch, and use adaptive mesh detail when FPS remains low.
- **Dropped items:** fluid buoyancy is damped near the surface to reduce bobbing jitter while retaining existing server-side pickup rules.
- **Player animation:** adds alternating leg kicks while swimming to the existing additive player animation.
- **Assets and interfaces:** all existing PNGs are kept untouched. No images/textures were created, modified or added. No GUI or interface files were changed.

## Water rendering limits

The new appearance uses a procedural surface, biome/depth colour, and a sky-colour/specular approximation. It is not full screen-space reflection, physically accurate refraction or a new finite-water simulation. A full shader/framebuffer path would require deeper rendering integration and carry greater compatibility risk on Forge 1.12.2.

## Build status

Resource validation and Forge 1.12.2 compilation passed in GitHub Actions run #61: https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45954678406

The workflow uploaded artifact minefade-mod-minecraft-1.12.2-v0.8.0 (artifact ID 15178541272). Its installable JAR reports version 0.8.0, mod ID voxelcore and Minecraft 1.12.2. Runtime tests inside a real Minecraft client and dedicated/LAN server have not been performed.

## Install

Copy the finished "Minefade-Mod-0.8.0-Forge-1.12.2.jar" into .minecraft/mods with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains "voxelcore".
