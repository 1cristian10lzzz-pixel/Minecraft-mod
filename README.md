# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.3.0** · Forge 14.23.5.2847

Minefade is a modular Minecraft 1.12.2 mod combining performance tools, a unified interface theme and original gameplay content.

## Download Minefade Mod 0.3.0

**Build status: successful.** GitHub Actions run [#34](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45693043458) compiled the mod with JDK 8, Gradle 4.10.3 and ForgeGradle 2.3, then uploaded the installable JAR as the artifact:

`minefade-mod-minecraft-1.12.2-v0.3.0`

Download the artifact from the run's **Artifacts** section. GitHub Actions artifacts expire after their configured retention period; this build is kept for 30 days. The JAR SHA-256 is recorded in [BUILD_STATUS.txt](./VoxelCore-1.12.2/BUILD_STATUS.txt).

The JAR passed archive-integrity and metadata checks. A launch inside a real Minecraft client has not yet been performed in the build environment.

## Latest source

- [Open the complete source project](./VoxelCore-1.12.2/)
- [Interface redesign and version notes](./VoxelCore-1.12.2/README.md)
- [Change log](./VoxelCore-1.12.2/CHANGELOG.md)

The historical mod ID remains `voxelcore` to preserve existing registry IDs and the configuration filename from the 0.2.0 source line. The in-game name and interface branding are Minefade Mod.

## 0.3.0 interface work

- Shared Minefade styling for ordinary buttons on vanilla and compatible mod screens.
- Minefade entry points on the main menu, pause menu and Options screen.
- Responsive in-game control center with Diagnostics, Performance and Content tabs.
- Persistent global UI theme toggle.
- Specialized sliders and image controls are left intact to preserve their behavior.

## Install

1. Install Minecraft Java 1.12.2 with Forge 1.12.2.
2. Download the JAR artifact from [GitHub Actions run #34](https://github.com/1cristian10lzzz-pixel/Minecraft-mod/actions/runs/45693043458).
3. Copy `minefade-0.3.0.jar` into `.minecraft/mods`.
4. Start the Forge 1.12.2 profile.

Source archives are not installable mods. Install only the compiled JAR.

## Build target

Minecraft 1.12.2 · Forge 14.23.5.2847 · Java 8 · ForgeGradle 2.3.
