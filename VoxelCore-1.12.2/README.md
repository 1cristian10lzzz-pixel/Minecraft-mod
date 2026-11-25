# Minefade Mod 0.6.0 — Minecraft Java 1.12.2

Forge 14.23.5.2847 · Java 8 · ForgeGradle 2.3 · Gradle 4.10.3

## Dropped item physics

Minefade 0.6.0 adds original Forge 1.12.2 behaviour inspired by Realistic Item Drops:

- Dropped items lie horizontally on the ground instead of hovering and spinning.
- Right-click pickup replaces automatic walk-over collection when enabled.
- Sprinting when dropping an item adds a forward throw impulse.
- Airborne items can be caught with the same right-click interaction.
- Dropped items receive gentle buoyancy and drag in water and lava.
- The OBJETOS tab exposes separate controls and throw strength.

The server handles pickup, toss and liquid motion so multiplayer state remains authoritative. Minefade uses its own implementation and does not copy the reference mod's code.

## Rendering and performance

The custom EntityItem renderer keeps Minecraft's baked item models, item texture atlas and standard lighting. It adds a ground transform for resting drops and optional item-distance culling.

Minefade also retains the 0.5.0 water rendering improvements: ripple geometry is batched, nearby water surfaces are cached, and optional water effects reduce their detail when FPS remains low.

Sodium-derived Forge renderers such as Zorya rewrite more of the chunk rendering path and use techniques including face culling, chunk multi-draw, vertex arrays and compact vertex formats. Those are engine-level changes and can conflict with other renderer mods. Minefade therefore applies narrower optimizations without replacing the vanilla chunk renderer. It is not a full Sodium implementation.

## Textures and interface

Minefade now includes original 32x32 item/block icons for Resonite tools, armour and materials, plus original leather and aged-metal panel/button textures. A new OBJETOS tab controls dropped-item physics.

Realstuff64 (64x, Minecraft 1.12.2) served as a visual reference for realistic texture density and palette. Its files are not bundled or copied. This update changes Minefade's own assets, not every vanilla item texture.

## Water and atmosphere

The 0.5.0 water effects remain available: surface rings, wakes, entry splashes, rain ripples, waterfall mist and biome-aware underwater tint. These are client-side visual effects. Vanilla fluid blocks and server water states are unchanged.

## Controls and settings

- **O:** open the Minefade control centre.
- **K:** toggle Minefade water effects.
- **F8:** toggle the performance monitor.
- **F7:** toggle distant-living-entity culling.
- **F6:** toggle minimal particles.

The settings centre includes INFO, REND., AGUA, MUNDO and OBJETOS.

## Build, verify and install

**Forge build passed:** [GitHub Actions run #47](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45705530398).

- JAR: `minefade-0.6.0.jar`
- Size: 97,424 bytes
- SHA-256: `67ebad56ddcb7473b8a14a79a0f3dfb8c0224cd300cc6ba6ba2bf7ac5f194212`

The downloaded JAR passed archive-integrity checks and includes the item physics handler, custom dropped-item renderer, OBJETOS settings screen, original textures and 0.6.0 metadata.

Install the compiled JAR in `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847.

## Compatibility notes

The internal mod ID remains `voxelcore`. Optional item-distance culling is disabled by default. Vanilla item models and fluid blocks are retained. A live Minecraft client launch and multiplayer runtime test have not been performed in this build environment.
