# Changelog

## 0.8.0 — Agua realista sin ondas geométricas

- Replaces the old expanding-ring-only water effect with a batched procedural surface mesh over exposed Minecraft water blocks.
- Smooths shared corner heights between neighboring water columns so the surface transitions more naturally between source and flowing water.
- Adds biome-aware colour, depth-based light absorption, view-angle sky reflection, daylight-dependent specular highlights and shoreline foam.
- Keeps the water geometry calm: there is no vertical wave displacement and no expanding circular ripple mesh. Reflected light and foam brightness can change without moving the surface.
- Adds underwater colour absorption and depth fog, plus rising bubbles, entry splashes, wakes and waterfall mist.
- Smooths dropped-item buoyancy near the water/lava surface and adds alternating leg kicks to the existing swimming animation.
- Keeps vanilla water blocks, flow, collision, source/flowing levels, buckets and server simulation intact.
- Makes no interface changes and creates, edits or adds no images or textures.

## 0.7.1 — Animaciones, recogida y rendimiento visual

- Installs an additive player model before vanilla calculates its pose; preserves default/slim skin geometry and armour overlays. Models are cached per renderer to avoid per-frame allocation.
- Binds the vanilla block texture atlas explicitly and displays each dropped stack as one smaller baked model on the ground.
- Adds a server-side pickup recovery that delegates to vanilla EntityItem pickup logic and retains inventory-capacity rules.
- Reduces nearby water-surface search frequency/radius and smooths ripple rings while retaining adaptive quality and batched drawing.
- Throttles optional liquid buoyancy work to reduce world-tick scanning.
- Keeps existing PNG files untouched; does not generate, edit or add images.
- Preserves vanilla water blocks, vanilla chunk rendering, all registered item/block IDs and the mod ID "voxelcore".

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
