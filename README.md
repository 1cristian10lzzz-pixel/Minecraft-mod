# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.6.0** · Forge 14.23.5.2847

Minefade combines a shared interface theme, performance diagnostics, Resonite content, water effects and improved dropped-item behaviour.

## Minefade Mod 0.6.0

- **Realistic dropped items:** flat ground placement, right-click pickup, sprint toss and gentle water/lava buoyancy and drag.
- **Item renderer:** uses Minecraft's baked item models and lighting, with a separate flat ground transform and optional render-distance culling.
- **Performance:** retains batched water ripple rendering, cached surface searches and adaptive water effects. Adds optional culling of distant dropped items and retains optional distant living-entity culling.
- **Visuals:** original 32x32 Minefade icons for its Resonite tools, armour and materials, plus original leather/aged-metal interface textures and an OBJETOS settings tab.

These are original Minefade implementations inspired by the behaviour and visual direction of other projects. No Realistic Item Drops source code or third-party resource-pack textures are copied into this mod.

## Sodium compatibility and limits

Sodium's Forge 1.12.2 ports improve performance by changing the chunk renderer deeply, with techniques such as face culling, chunk multi-draw, vertex arrays and compact vertex formats. Replacing Minecraft's complete chunk renderer inside Minefade would risk conflicts with other mods. Minefade instead uses isolated optimizations that do not replace the vanilla chunk pipeline. It is not a full Sodium port.

## References used

- [Realistic Item Drops — CurseForge](https://www.curseforge.com/minecraft/mc-mods/realistic-item-drops)
- [Zorya — Forge 1.12.2 Sodium-derived renderer](https://github.com/MyNukeMC/Zorya)
- [Realstuff64 — realistic 64x resource pack for 1.12.2](https://www.curseforge.com/minecraft/texture-packs/realstuff64)

Third-party assets are references only. Minefade's included GUI and Resonite textures are original.

## Build status

**Forge build passed:** [GitHub Actions run #47](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45705530398).

- JAR: `minefade-0.6.0.jar`
- Size: 97,424 bytes
- SHA-256: `67ebad56ddcb7473b8a14a79a0f3dfb8c0224cd300cc6ba6ba2bf7ac5f194212`
- Artifact: `minefade-mod-minecraft-1.12.2-v0.6.0` (ID 15065261525)

The downloaded JAR passed ZIP/CRC checks and required class/metadata checks. It has not been launched inside a real Minecraft client in this environment.

## Install

Copy `Minefade-Mod-0.6.0-Forge-1.12.2.jar` into `.minecraft/mods` with Minecraft Java 1.12.2 and Forge 14.23.5.2847. The internal mod ID remains `voxelcore` to preserve existing registry and configuration compatibility.
