# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.11.0** · Forge 14.23.5.2864 · ForgeGradle 3.x · Gradle 5.6.4 · Java 8

Minefade improves existing Minecraft 1.12.2 rendering and gameplay systems while retaining the `voxelcore` mod ID, registered content IDs, vanilla fluid simulation and original gameplay content.

## 0.11.0 — Stability and whole-mod optimization

- **Dimension-safe diagnostics:** the chunk counter now keys chunks by both dimension and coordinates, so identical chunk positions in the Nether and Overworld are not merged.
- **World transitions:** clears water, lava and mineral-detail caches on world/dimension unload; resets performance samples and restores the user's particle setting.
- **Particle setting:** connects the minimal-particle option to client ticks, applies it only while in a world and restores/persists the original preference when disabled or leaving the world.
- **Water performance:** caches underwater depth across fog events and reuses mutable block positions in water surface, nearby-surface and waterfall scans. Surface quality now changes scan radius and refresh cadence.
- **Water consistency:** the glints and shoreline foam follow smoothed water surface heights instead of floating at a flat per-block height.
- **Lava:** uses quality-scaled scan radius, precomputed crack-strip offsets and a bounded top-of-world scan; adds a restrained warm immersion fog tint while leaving vanilla lava fog density alone.
- **Material effects:** reuses the scan position for block-neighbour checks, keeps mining debounce state correct across worlds/time resets and reduces repeated config reads per face.
- **Slime contacts:** detects the falling-to-ground transition rather than relying on post-bounce velocity; contact tracking continues even if individual effect toggles are disabled.
- **Dropped items:** runs the server pickup recovery less often and reuses a mutable position for fluid buoyancy checks.
- **Rendering stability:** all added overlays continue to restore the GL state they change.

## 0.10.0 — Realismo visual y actualización de Forge

- Water and lava surface improvements, OptiFine-inspired settings pages and additive player animations.
- Targets Forge 14.23.5.2864 using ForgeGradle 3.x (UserDev 3), Gradle 5.6.4 and Java 8.

## Render approach and limits

Water and lava remain vanilla fluids with Minefade client-side surface accents. Water uses subtle micro-height variation and lighting, but reflections remain a sky-colour approximation rather than screen-space reflection or physical refraction. Lava uses precomputed branching crack geometry with pulsing colour, not a heat-distortion shader or physically simulated molten flow. The current implementation does not add new images, textures or resource packs, and does not replace the vanilla chunk renderer.

The new menu follows OptiFine's category organization and control style; it is an independent Minefade implementation, not OptiFine code or full feature parity.

## Compatibility and verification

Target: Minecraft Java 1.12.2 · Forge 14.23.5.2864 · ForgeGradle 3.x · Gradle 5.6.4 · Java 8.

GitHub Actions compilation is used to validate Forge/API compatibility. It does not replace visual testing in a live game, multiplayer testing or device/GPU-specific performance measurement. The latest validated build should be installed as a JAR in the Forge `.minecraft/mods` directory.
