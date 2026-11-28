## 0.10.0 — Superficie de fluidos, renderizado y estabilidad

- Replaces the flat water overlay with a gently varying micro-surface. Uses vanilla `BlockLiquid.getLiquidHeight` for source/falling-fluid heights and softly shares corners between adjacent columns only when their heights are close; it does not bridge banks or separate pools.
- Adds small multi-directional surface relief and analytical normals for directional specular response. The effect is deliberately subtle and retains vanilla water texture/flow beneath the low-alpha tint.
- Moves the frame-wide sun vector calculation out of per-vertex work and uses analytical wave gradients rather than finite-difference sampling; simplifies the Fresnel/specular power operations to reduce repeated expensive math.
- Replaces sliding lava stripes with precomputed, stationary branching fissure geometry, dark red rims, hot orange cores and slow pulsing junctions. Uses vanilla fluid-height calculations and reuses mutable block positions during scans.
- Adds shared OpenGL state backup/restore for blend factors, texture, lighting, culling, depth, alpha test, colour material, rescale normal and current colour so Minefade overlays do not leak their render state into later renders.
- Fixes vegetation contact to inspect plant blocks above the ground support block. Limits each step to one tiny nearby contact puff.
- Reduces mineral-detail scan radius, vertical range, cache size and refresh frequency.
- Precomputes lava fissure layout when rebuilding its cache so per-frame work only animates the glow; limits ember sampling attempts.
- Refines swimming animation so paddling fades out when the player is stationary and synchronizes the headwear overlay with the head pose.
- Improves grounded item renderer cleanup while retaining vanilla baked models and item atlas.
- Keeps vanilla fluid simulation, block/item IDs, Resonite content, dropped-item rules and world generation. No images, textures or resource packs are created or modified.
- CI compilation checks Forge/API compatibility. Visual checks in a live client, GPU profiling, multiplayer and long-session testing remain separate tasks.

## 0.9.0 — Fluidos, optimización y ajustes de vídeo

- Reworks the water overlay after the prior screenshot showed an overly opaque, pale surface: alpha is reduced to a subtle tint so vanilla water texture and flow remain visible.
- Replaces the 4x4 subdivided water-cell grid with one batched quad per cached surface column; bounds the surface search/cache and shortens the vertical scan.
- Adds restrained sky-colour/specular response, small animated streak highlights and low-alpha shoreline foam without geometric waves or expanding ring geometry.
- Fixes repeated nearby-water scans on dry land by caching empty results and rechecking only after movement thresholds or a timer.
- Adds a distinct client-only lava renderer with moving molten vein accents and sparse smoke/ember particles. Lava quality, scan radius and ember frequency adapt to sustained low FPS. Vanilla lava remains the base texture and retains its flow, light, damage, collision and server state.
- Reworks player animation into a conservative additive layer that preserves vanilla crouch, jump, Elytra and fall base poses; adds measured walking weight shift, sprint follow-through, climbing, swimming and idle breathing.
- Replaces the old settings centre with an OptiFine-inspired Video Settings layout: Quality, Details, Performance, Animations, Water/Lava, Items, World and Other pages. Adds controls for new water, lava, material and animation options plus selected vanilla graphics options.
- Prevents the global GUI skin handler from restyling the new video-settings screen.
- Adds persistent config values for water surface quality/highlights/foam, lava quality/radius/embers, surface-detail density, material contacts and player animation strength.
- Reduces optional water/stone/lava render work; caches empty water lookups, uses reusable frame-time percentile storage and packed chunk coordinates.
- Keeps vanilla fluid simulation, existing item physics, Resonite content, world generation and registered IDs. No images, textures or resource packs are created or modified.
- Water reflections remain an approximation; lava details are a low-alpha overlay, not full PBR/refraction/heat distortion. In-game visual and multiplayer testing are still required.

## 0.8.0 — Realismo ambiental, materiales y animaciones

- Adds deterministic procedural mineral-grain highlights to exposed faces of stone (including granite, diorite and andesite variants), cobblestone, gravel, netherrack, end stone, sandstone, red sandstone, obsidian, prismarine, stone bricks, hardened clay, bricks, quartz blocks and common ores.
- Extends the additive player animation layer with improved weight transfer, torso counter-rotation, sprint arm pumping, crouch posture, ladder climbing, swimming paddles, elytra posture, falling pose and subtle idle breathing.
- Adds lightweight vegetation contact particles when the player walks through tall grass, ferns, vines, dead bushes and foliage.
- Adds small block-coloured chips when striking hard surfaces and restrained material particles when walking over stone, dirt, grass, sand, clay and snow.
- Adds a small block-fragment contact burst when the player lands and receives the vanilla bounce from a slime block; vanilla bounce force and collision are unchanged.
- Adds a calm custom water surface with biome/depth colour, sky-reflection approximation, view-angle Fresnel response, daylight highlights and shoreline foam; no geometric wave displacement or expanding ring geometry.
- Adds underwater colour absorption and depth fog, rising bubbles, local splashes/wakes and waterfall mist.
- Smooths dropped-item buoyancy near fluid surfaces and adds alternating swimming leg kicks.
- Keeps vanilla blocks, fluid flow, collision, slime bounce, harvesting, drops and server simulation intact.
- No GUI/interface files changed. No images, textures or resource packs created or modified.
- Stone detail and water surface scans are bounded and cached; render work is batched.

## 0.7.1 — Animaciones, recogida y rendimiento visual

- Installs an additive player model before vanilla calculates its pose; preserves default/slim skin geometry and armour overlays. Models are cached per renderer to avoid per-frame allocation.
- Binds the vanilla block texture atlas explicitly and displays each dropped stack as one smaller baked model on the ground.
- Adds a server-side pickup recovery that delegates to vanilla EntityItem pickup logic and retains inventory-capacity rules.
- Reduces nearby water-surface search frequency/radius and smooths ripple rings while retaining adaptive quality and batched drawing.
- Throttles optional liquid buoyancy work to reduce world-tick scanning.
- Keeps existing PNG files untouched; does not generate, edit or add images.
- Preserves vanilla water blocks, vanilla chunk rendering, all registered item/block IDs and the mod id "voxelcore".

## 0.6.0 — Física de objetos, render y estilo visual

- Added server-authoritative dropped-item handling: right-click pickup, sprint toss, airborne catch interaction and gentle liquid buoyancy/drag.
- Added a custom EntityItem renderer that places resting item models horizontally while preserving vanilla baked models and lighting.
- Added optional dropped-item distance culling, disabled by default.
- Added the OBJETOS settings tab and item-physics controls.
- Added original 32x32 Resonite item/block icons and leather/aged-metal GUI textures.
- Kept the 0.5.0 batched water ripple renderer, surface cache and adaptive water quality.
- Applied selective Sodium-inspired optimization without replacing the vanilla chunk renderer.
- Preserved the internal mod id "voxelcore", Minecraft 1.12.2 and Forge 14.23.5.2847.

## 0.5.0 — Optimización del renderizado de agua

- Batched all active ripple rings into one render buffer and one draw call per frame.
- Cached nearby water-surface lookups and reduced waterfall scans.
- Added adaptive water quality with FPS hysteresis.
- Capped active ripples and reduced optional visual work at low FPS.
- Cleared cached surfaces and ripples on world changes and when effects are disabled.

## 0.4.0 — Realismo ambiental y agua

- Added client-side surface rings, wakes, water-entry splashes, rain ripples, waterfall mist and biome-aware underwater fog colour.
- Added the Agua settings tab and the K water-effects key.
- Preserved vanilla fluid blocks and server-side water state.

## 0.3.0 — Minefade UI and settings overhaul

- Added shared GUI styling and a settings centre with diagnostics, performance and content tabs.

## 0.2.0

- Added performance diagnostics and original Resonite content, tools, armour and world generation.
