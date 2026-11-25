# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.3.0** · Forge 14.23.5.2847

Minefade is a modular Minecraft 1.12.2 mod combining performance tools, a unified interface theme and original gameplay content.

## Latest source

- [Open the complete source project](./VoxelCore-1.12.2/)
- [Interface redesign and version notes](./VoxelCore-1.12.2/README.md)
- [Change log](./VoxelCore-1.12.2/CHANGELOG.md)

The historical mod ID remains voxelcore to preserve existing registry IDs and the configuration filename used by the 0.2.0 source line. The in-game name and interface branding are Minefade Mod.

## 0.3.0 interface work

- Shared Minefade styling for ordinary buttons on vanilla and compatible mod screens.
- Minefade entry points on the main menu, pause menu and Options screen.
- Responsive in-game control center with Diagnostics, Performance and Content tabs.
- Persistent global UI theme toggle.
- Specialized sliders and image controls are left intact to preserve their behavior.

## Building the installable JAR

The repository includes a GitHub Actions build that installs JDK 8 and Gradle 4.10.3, validates assets, builds Forge 1.12.2 and uploads the resulting JAR as an Actions artifact.

Open the Actions tab and select **Build Minefade Mod 1.12.2**. Install only the compiled JAR from a successful run in .minecraft/mods; source archives are not installable mods.

Forge target: Minecraft 1.12.2 · Forge 14.23.5.2847 · Java 8 · ForgeGradle 2.3.
