# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.9.0** · Forge 14.23.5.2847 · Java 8

Minefade improves existing Minecraft 1.12.2 rendering and gameplay systems while retaining the `voxelcore` mod ID, registered content IDs, vanilla fluid simulation and original gameplay content.

## 0.9.0 update

- **Water:** fixed the over-opaque white/blue surface overlay seen in the submitted screenshot. The water pass now uses low alpha so the vanilla texture and animated flow remain visible. Added restrained sky-colour/specular accents, small animated glints and thin shoreline foam.
- **Water performance:** one batched quad per visible water column instead of sixteen sub-quads; smaller bounded scan windows; cached empty lookups so dry land no longer triggers a full nearby-water search every tick.
- **Lava:** new molten vein highlights and sparse ember/smoke effects over vanilla lava. Vanilla flow, lighting, damage and collision remain intact.
- **Animations:** smaller additive player motion. Vanilla crouch, jump, Elytra and falling poses stay in control; Minefade adds restrained walking, sprint, climbing, swimming and idle motion.
- **Settings:** an OptiFine-inspired video settings layout with Quality, Details, Performance, Animations, Water/Lava, Items, World and Other pages. Includes new Minefade options and selected vanilla video settings.
- **Existing content:** preserves Resonite blocks/items/world generation, dropped-item rendering/physics, stone details, vegetation contact, slime bounce feedback and existing registered IDs.
- **Optimization:** bounded cached fluid/material scans, reduced overlays and fewer recurring allocations in diagnostics/chunk tracking.

## Render approach and limits

Water and lava remain vanilla fluids with Minefade client-side surface accents. Water reflection is an approximation, not screen-space reflection or physical refraction. Lava streaks are not a shader or a physically simulated molten surface. The current implementation does not add new images, textures or resource packs, and does not replace the vanilla chunk renderer.

The new menu follows OptiFine's category organization and control style; it is an independent Minefade implementation, not OptiFine code or full feature parity.

## Compatibility and verification

Target: Minecraft Java 1.12.2 · Forge 14.23.5.2847 · Java 8.

GitHub Actions compilation is used to validate Forge/API compatibility. It does not replace visual testing in a live game, multiplayer testing or device/GPU-specific performance measurement. The last validated build should be installed as a JAR in the Forge `.minecraft/mods` directory.
