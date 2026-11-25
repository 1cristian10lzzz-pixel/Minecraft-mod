# Changelog

## 0.5.0 — Optimización del renderizado de agua

- Batched all active ripple rings into one render buffer and one draw call per frame, instead of issuing a draw per ring.
- Cached nearby water-surface lookups and reduced waterfall scans to lower client-tick block queries.
- Added optional automatic water quality with hysteresis; sustained low FPS reduces scan frequency, particle count and ring detail, then restores quality after FPS recovers.
- Reduced visual work at low particle settings and capped concurrent ripples.
- Cleared cached surfaces and active ripples when leaving a world or disabling water effects.
- Preserved Forge 1.12.2 compatibility, vanilla fluid blocks and the existing `voxelcore` registry namespace.


## 0.4.0 — Realismo ambiental y agua

- Added an original client-side water presentation layer for Forge 1.12.2.
- Added expanding surface rings around moving swimmers and boats.
- Added water-entry splashes and wake particles.
- Added rain rings on nearby exposed water.
- Added light mist particles near nearby waterfall impacts.
- Added biome-aware underwater fog colouring.
- Added an in-game Agua tab with independent toggles, ripple strength and effect radius.
- Added K as a quick toggle for all Minefade water effects; avoids the vanilla F5 camera key.
- Kept vanilla water blocks, source conversion and saved-world fluid states unchanged.
- Expanded the performance overlay with water-effect state and radius.
- Updated the Forge CI workflow and language keys.

## 0.3.0 — Minefade UI and settings overhaul

- Added a shared Minefade GUI theme for vanilla screens and compatible mod screens.
- Re-skinned ordinary buttons while preserving their original IDs, dimensions and action handling.
- Kept specialized sliders, image buttons and custom controls intact to avoid breaking behavior.
- Added Minefade access buttons to the main menu, pause menu and Options screen.
- Added a responsive in-game settings center with diagnostics, performance and content tabs.
- Added a persistent global UI-theme toggle.
- Updated branding and language strings for Minefade.
- Added a root GitHub Actions workflow for Forge 1.12.2 builds and JAR artifacts.

## 0.2.0

- Added chunk/entity/memory diagnostics and configurable optional visual performance controls.
- Added Resonite gameplay content, tools, armor, recipes and world generation.
