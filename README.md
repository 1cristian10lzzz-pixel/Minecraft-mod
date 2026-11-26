# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.7.0** · Forge 14.23.5.2847

Minefade adds a shared interface theme, performance diagnostics, Resonite content, client-side water presentation, natural player animation and improved dropped-item behaviour.

## Minefade Mod 0.7.0

- **Player animation:** a small additive animation layer runs after vanilla calculates the base pose. Default and slim player models, armour layers and vanilla hitboxes are retained.
- **Dropped items:** one smaller baked model represents each dropped stack on the ground. The renderer binds Minecraft's item atlas and keeps the vanilla ground camera transform.
- **Pickup reliability:** a server-side proximity recovery calls the vanilla dropped-item pickup method; inventory capacity, pickup delay and server authority remain handled by Minecraft.
- **Water:** cached local surface lookups run less often, ripples use smoother rings, and optional effects continue to adapt when FPS is low. Vanilla water blocks and fluid simulation are unchanged.
- **Performance:** optional dropped-item culling remains disabled by default; fluid buoyancy checks are throttled and the vanilla chunk renderer is untouched.
- **Assets:** existing PNG textures and icons are preserved exactly. No new images or third-party resource-pack textures are included.

Mo' Bends, Realistica and Realistic Finite Fluids were used as research references only. No third-party source code or protected textures are copied into Minefade.

## Compatibility strategy

Minefade avoids replacing Minecraft's chunk renderer or fluid blocks, because doing so would increase conflicts with Forge 1.12.2 mods. Rendering changes are limited to the player model and dropped-item renderer; water effects are client-side and do not alter world state.

## Build status

The 0.7.0 build is pending CI. It must pass both resource validation and ForgeGradle compilation before its JAR is distributed. A build pass alone does not prove actual Minecraft runtime behaviour; a real client and server test is still required.

## Install

Copy the finished `Minefade-Mod-0.7.0-Forge-1.12.2.jar` into `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains `voxelcore` to preserve registry and configuration compatibility.
