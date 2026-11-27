# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.8.0** · Forge 14.23.5.2847

Minefade improves the legacy client presentation while retaining vanilla block identities, collision and server simulation.

## Minefade Mod 0.8.0

- **Water:** calm custom surface mesh, biome/depth colour, sky-reflection approximation, Fresnel response, daylight highlights and shoreline foam. No geometric waves or expanding ring geometry.
- **Vegetation contact:** subtle client-side block-fragment particles when walking through tall grass, ferns, vines, dead bushes and leaves. Effects are throttled and restricted to a tiny area around the player.
- **Slime contact:** a small particle burst on landing from a vanilla slime-block bounce. Vanilla bounce physics are not changed.
- **Underwater:** biome-aware colour absorption, depth fog, bubbles, entry splashes, wakes and waterfall mist.
- **Movement and items:** additive swimming leg kicks and gentler dropped-item buoyancy near fluid surfaces.
- **Performance:** cached nearby water surfaces, batched water drawing and adaptive mesh detail.

## Preserved behaviour and scope

Minecraft 1.12.2 water, grass, leaves, vines, slime blocks, collision, block drops, harvesting and server simulation remain vanilla. The new vegetation/slime contact layer is visual feedback, not full vertex bending or a replacement foliage renderer. No GUI/interface files changed. No images, textures or resource packs were created or modified.

## Rendering limits

The water reflection is a sky-colour/specular approximation, not full screen-space reflection or physically accurate refraction. Vegetation contact is represented by particles; foliage meshes do not yet physically bend. A full shader/framebuffer and foliage vertex-animation system requires deeper render-pipeline integration and compatibility testing on Forge 1.12.2.

## Build status

The previous 0.8.0 source build passed Forge compilation and resource validation. The latest vegetation/slime changes require a new CI build and in-game runtime tests before they can be called verified.

## Install

Use the installable Minefade 0.8.0 JAR with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod id remains "voxelcore".
