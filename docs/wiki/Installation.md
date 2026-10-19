# 📦 Installation

## Choose a release

Download one JAR from [GitHub Releases](https://github.com/dcvke/CinematicInteractions/releases) that matches **both** your Minecraft version and mod loader. The latest beta supports NeoForge 1.21.1 and Fabric 1.21.11, 26.1.1, 26.1.2, 26.2 and 26.3. Older Fabric 1.21–1.21.10 downloads remain available as archived builds. A JAR for one game version is not a universal build.

## Fabric

1. Install Fabric Loader for your game version.
2. Install Fabric API for that same game version.
3. Put the matching Cinematic Interactions JAR in `mods/`.
4. Optionally add Mod Menu for the settings button and Cloth Config for the expanded settings screen.

| Minecraft | Minimum Fabric API | Minimum Fabric Loader |
| --- | --- | --- |
| 1.21 | 0.102.0+1.21 | 0.16.14+ |
| 1.21.1 | 0.116.17+1.21.1 | 0.16.14+ |
| 1.21.2 | 0.106.1+1.21.2 | 0.16.14+ |
| 1.21.3 | 0.114.1+1.21.3 | 0.16.14+ |
| 1.21.4 | 0.119.4+1.21.4 | 0.16.14+ |
| 1.21.5 | 0.128.2+1.21.5 | 0.16.14+ |
| 1.21.6 | 0.128.2+1.21.6 | 0.16.14+ |
| 1.21.7 | 0.129.0+1.21.7 | 0.16.14+ |
| 1.21.8 | 0.136.1+1.21.8 | 0.16.14+ |
| 1.21.9 | 0.134.1+1.21.9 | 0.17.3+ |
| 1.21.10 | 0.138.4+1.21.10 | 0.17.3+ |
| 1.21.11 | 0.141.6+1.21.11 | 0.17.3+ |

## NeoForge

Use Minecraft **1.21.1**. The reference development and verification environment uses **NeoForge 21.1.255**; the released JAR declares **21.1.200+** as its minimum. Cloth Config 15.x is optional. Fabric API and Mod Menu are not used by this build.

## Multiplayer

For the full experience, install the mod on **both the client and the server**. Install matching game, loader, and Cinematic Interactions builds on each side, plus Fabric API on Fabric. Other players with the mod can see synchronized items, workstation poses, and supported action effects.

The latest beta also offers an optional client-only preview for selected stations on an unmodified server. Enable **Client Mode** in the mod settings from Minecraft's main menu before joining. These local scenes use vanilla menus and do not synchronize displayed items or poses with other players; unsupported stations use their vanilla interfaces. Singleplayer needs only the normal client installation: Minecraft runs the integrated server automatically.

## Release names

The initial public downloads use filenames such as `CinematicInteraction-0.1-Fabric-1.21.11.jar`. The public filename label is **0.1**; the existing binaries retain their development versions internally: NeoForge **0.11.2**, Fabric 1.21–1.21.9 **0.12.2**, and Fabric 1.21.10/1.21.11 **0.12.3**. Include the version shown in the mod list when reporting a problem.

Install only one Cinematic Interactions JAR at a time. Optional rendering mods and shader packs must also match your Minecraft version and loader.
