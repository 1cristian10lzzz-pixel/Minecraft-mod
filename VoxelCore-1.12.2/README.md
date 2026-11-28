# Minefade Mod 0.11.0 — Minecraft Java 1.12.2

Forge 14.23.5.2864 · Java 8 · ForgeGradle 3.x (UserDev 3) · Gradle 5.6.4

Minefade 0.11.0 is a whole-mod stability and performance update. It keeps the existing `voxelcore` mod ID, registered block/item IDs, vanilla fluid simulation and world content.

## Whole-mod stability and performance

- Chunk diagnostics now include the dimension ID so identical chunk coordinates in different dimensions are counted separately.
- World/dimension unload clears water, lava and mineral-detail caches, frame-time samples and restores the original Minecraft particle setting.
- The minimal-particle option is now connected to the client tick lifecycle. It only applies while in a world and restores and saves the user's previous setting when released.
- Underwater depth is cached for four world ticks and shared by fog colour/density callbacks; water, waterfall and neighbour scans reuse mutable positions.
- Surface quality changes water/lava scan radius and refresh interval, lowering work when the configured quality or adaptive FPS mode is reduced.
- Glints and shoreline foam now follow the smoothed water surface height.
- Lava crack segment offsets are precomputed, top-of-world scans are bounded and lava immersion adds a mild warm fog tint without replacing vanilla fog density.
- Mineral detail checks reuse the scan position; mining-contact cooldown data resets on world changes and time rollback.
- Slime contact uses a falling-to-ground transition and keeps tracking state current even when its visual toggles are disabled.
- Server item-pickup recovery runs less frequently, and fluid buoyancy reuses a mutable block position.

## Water rendering

- The previous near-opaque white/blue overlay has been replaced by a low-alpha surface treatment. The vanilla water texture and its animated flow remain visible underneath.
- Surface tint is based on biome water colour and measured local depth; sky reflection and specular response are deliberately restrained.
- Small elongated highlights glide across the surface. They are not expanding rings.
- Shore foam is thin, low-alpha and follows the interpolated surface height along exposed edges.
- The custom pass uses vanilla fluid-height calculations, softly blends neighbouring water-cell corners at similar levels, and uses a tiny multi-directional micro-surface displacement with analytical lighting normals.
- The search radius, vertical scan range, cache size and refresh interval are bounded. Null search results are cached too.
- Underwater tint/fog, bubbles, entry splashes, wakes, rain impacts and waterfall mist remain separate local effects.

**Rendering limit:** reflections are an approximation based on sky colour and viewing angle; the micro-surface displacement is deliberately very small, not large waves. This is not screen-space reflection, physical refraction or a full shader pipeline.

## Lava

- A client-only molten-surface pass draws precomputed branching molten fissures with a darker rim, a bright core and a slow pulsing glow.
- Sparse smoke/ember particles are emitted near nearby lava surfaces.
- Quality and radius can be configured separately from water.
- Warm lava-immersion fog is added as a subtle colour blend; vanilla fog density remains in control.
- Vanilla lava texture animation, flow, light, collision, damage and server state remain authoritative.

**Rendering limit:** the molten pattern is a procedural overlay, not a replacement lava material or a heat-distortion shader.

## Animations

The player model uses a conservative additive animation layer. It leaves vanilla's base pose in control and adds restrained body weight shift, sprint follow-through, climbing reach, swimming stroke/kicks, subtle attack counter-motion and idle breathing. Crouch, jump, fall and Elytra base poses remain under vanilla control. Clothing overlays are synchronized after the animation offsets.

## Video settings

The Minefade settings page follows an OptiFine-inspired layout: Quality, Details, Performance, Animations, Water/Lava, Items, World and Other. It uses compact two-column vanilla buttons, click-to-cycle values and per-option tooltips. It is an independent Minefade screen, not OptiFine code or full feature parity.

## Existing content

- Procedural material accents remain available for vanilla stone families, cobblestone, gravel, netherrack, end stone, sandstone, red sandstone, obsidian, prismarine, stone bricks, hardened clay, bricks, quartz blocks and ores.
- Contact effects include vegetation, slime landings, block impacts and material footstep particles.
- Existing dropped-item rendering, sprint toss, pickup recovery, buoyancy/drag, Resonite content and world generation are retained.

## Compatibility and non-goals

Vanilla water/lava blocks retain their IDs, metadata, textures, collision, flow, harvesting, drops and server simulation. No registered content IDs are changed. No images, textures or resource packs are created or modified. The vanilla chunk renderer is not replaced.

Water reflections remain an approximation; no physical refraction, screen-space reflection, true foliage deformation or heat-distortion shader is included.

## Build and testing

Target runtime: Minecraft Java 1.12.2, Forge 14.23.5.2864, Java 8. Build toolchain: ForgeGradle 3.x (UserDev 3) and Gradle 5.6.4.

CI compilation confirms source/API compatibility only. A live Minecraft launch, GPU-specific visual check, multiplayer session and long-duration performance test still need to be performed in-game.
