# Minefade Mod 0.10.0 — Minecraft Java 1.12.2

Forge 14.23.5.2847 · Java 8 · ForgeGradle 2.3 · Gradle 4.10.3

Minefade 0.10.0 is a client-rendering and gameplay polish update. It keeps the existing `voxelcore` mod ID, registered block/item IDs, vanilla fluid simulation and world content.

## Water rendering

- The previous near-opaque white/blue overlay has been replaced by a low-alpha surface treatment. The vanilla water texture and its animated flow remain visible underneath.
- Surface tint is based on biome water colour and measured local depth; sky reflection and specular response are deliberately restrained.
- Small elongated highlights glide across the surface. They are not expanding rings and do not displace the water mesh.
- Shore foam is thin, low-alpha and limited to exposed edges.
- The custom pass uses vanilla fluid-height calculations, softly blends neighbouring water-cell corners at similar levels, and uses a tiny multi-directional micro-surface displacement with analytical lighting normals. Tessellation is limited by quality settings and adaptive FPS reduction.
- The search radius, vertical scan range, cache size and refresh interval are bounded. Null search results are cached too, avoiding repeated water searches while walking on dry land.
- Underwater tint/fog, bubbles, entry splashes, wakes, rain impacts and waterfall mist remain separate local effects.

**Rendering limit:** reflections are an approximation based on sky colour and viewing angle; the micro-surface displacement is deliberately very small, not large waves. This is not screen-space reflection, physical refraction or a full shader pipeline.

## Lava

- A client-only molten-surface pass draws precomputed branching molten fissures with a darker rim, a bright core and a slow pulsing glow.
- Sparse smoke/ember particles are emitted near nearby lava surfaces.
- Quality and radius can be configured separately from water.
- Vanilla lava texture animation, flow, light, collision, damage and server state remain authoritative.

**Rendering limit:** the molten pattern is a procedural overlay, not a replacement lava material or a heat-distortion shader.

## Animations

The player model now uses a smaller additive animation layer. It leaves vanilla's base pose in control and adds restrained body weight shift, sprint follow-through, climbing reach, swimming stroke/kicks, subtle attack counter-motion and idle breathing. The default crouch, jump, fall and Elytra poses are no longer replaced with large absolute-angle overrides. Clothing overlays are synchronized after the animation offsets.

The animation strength can be set from 0 to 100 in the new video settings pages.

## Video settings — OptiFine-inspired layout

The Minefade settings page follows the familiar video-settings structure:
- Quality
- Details
- Performance
- Animations
- Water/Lava
- Items
- World
- Other

Options use compact two-column vanilla buttons with click-to-cycle values. The page exposes existing Minefade settings plus vanilla graphics, smooth lighting, render distance, FPS limit, particles and VSync. No new GUI textures or images are used. This is an OptiFine-inspired arrangement; it does not include OptiFine code or claim full OptiFine feature parity.

## Material details and existing systems

- Procedural micro-grain accents remain available for vanilla stone families, cobblestone, gravel, netherrack, end stone, sandstone, red sandstone, obsidian, prismarine, stone bricks, hardened clay, bricks, quartz blocks and ores.
- Contact effects for vegetation, slime landings, hard-block impacts and material footstep particles now have separate config switches.
- Existing dropped-item ground rendering, sprint toss, pickup recovery, buoyancy/drag, Resonite content and world generation are retained.
- Diagnostic P95 frame-time calculation uses a reusable scratch array; chunk tracking uses compact packed chunk coordinates instead of allocated dimension/key strings.

## Compatibility and non-goals

Vanilla water/lava blocks retain their IDs, metadata, textures, collision, flow, harvesting, drops and server simulation. No registered content IDs are changed. No images, textures or resource packs are created or modified. The vanilla chunk renderer is not replaced.

The current water and lava effects are client-side overlays. They do not add physically simulated surface waves, true fluid refraction, screen-space reflections, deformable foliage meshes or a complete shader-based lighting system.

## Build and testing

Target runtime: Minecraft Java 1.12.2, Forge 14.23.5.2847, Java 8.

CI compilation confirms source/API compatibility only. A live Minecraft launch, GPU-specific visual check, multiplayer session and long-duration performance test still need to be performed in-game.
