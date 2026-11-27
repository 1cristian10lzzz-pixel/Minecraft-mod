# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.8.0** · Forge 14.23.5.2847

Minefade improves the legacy client presentation while retaining vanilla block identities, collision and server simulation.

## Minefade Mod 0.8.0

- **Stone and mineral surfaces:** deterministic procedural micro-grain highlights on exposed faces of stone, cobblestone, gravel, netherrack, end stone and common ores. Uses a small cached vertex-colour pass; no texture replacement or block-state changes.
- **Player animation:** more natural weight transfer, torso counter-rotation, sprint arm pumping, crouch posture, ladder climbing, swimming paddles, elytra pose, falling posture and subtle idle breathing. Vanilla calculates the base pose and Minefade adds motion afterwards.
- **Water:** calm custom surface mesh, biome/depth colour, sky-reflection approximation, Fresnel response, daylight highlights and shoreline foam. No geometric waves or expanding ring geometry.
- **Vegetation contact:** subtle client-side block-fragment particles when walking through tall grass, ferns, vines, dead bushes and leaves. Effects are throttled and restricted to a tiny area around the player.
- **Slime contact:** a small particle burst on landing from a vanilla slime-block bounce. Vanilla bounce physics are not changed.
- **Underwater:** biome-aware colour absorption, depth fog, bubbles, entry splashes, wakes and waterfall mist.
- **Movement and items:** additive swimming leg kicks and gentler dropped-item buoyancy near fluid surfaces.
- **Performance:** cached nearby water surfaces and stone detail candidates, batched water and stone-detail drawing, bounded scan radius and adaptive water mesh detail.

## Preserved behaviour and scope

Minecraft 1.12.2 blocks retain their original IDs, metadata, textures, collision, drops, harvesting and server simulation. The stone detail layer is a subtle procedural surface overlay; it does not replace vanilla textures or create new block variants. Player animations are additive to the vanilla pose. GUI/interface files are unchanged. No images, textures or resource packs were created or modified.

## Rendering limits

The water reflection is a sky-colour/specular approximation, not full screen-space reflection or physically accurate refraction. Stone grains are small vertex-colour details, not a full PBR material or a replacement high-resolution stone texture. Vegetation contact is represented by particles; foliage meshes do not yet physically bend. A full shader/framebuffer and foliage vertex-animation system requires deeper render-pipeline integration and compatibility testing on Forge 1.12.2.

## Build and runtime status

The source changes are submitted to the existing pull request and the new Forge CI build is running. Compilation success does not replace in-game visual, multiplayer and compatibility tests.

## Install

Use the installable Minefade 0.8.0 JAR with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod id remains "voxelcore".
