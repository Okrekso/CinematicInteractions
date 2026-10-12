# 🛠️ Developer guide

## Requirements

Install **JDK 21** and set `JAVA_HOME` to your own JDK installation. Use the checked-in Gradle wrapper; a global Gradle installation is not required. NeoForge uses ModDevGradle 2.0.148 and Gradle 9.2.1; Fabric uses Loom 1.16.3 and Gradle 9.4.1.

## Build NeoForge

From the repository root:

```sh
./gradlew build
./gradlew runClient
```

Windows: `./gradlew.bat build`. The target is Minecraft 1.21.1 / NeoForge 21.1.255. Game JARs are written to `build/libs/`.

## Build Fabric

From `fabric/`:

```sh
./gradlew -Pmc_version=1.21.11 build
./gradlew -Pmc_version=1.21.11 runClient
```

Windows: `./gradlew.bat -Pmc_version=1.21.11 build`. Choose any supported exact version from 1.21 to 1.21.11. Output goes to `fabric/build/<Minecraft>/libs/`. `fabric/tools/build-all.ps1` builds the supported versions sequentially and exports local release files.

The current Fabric development version is 0.12.3. To build the earlier public 1.21–1.21.9 releases with their original version label, pass `-Pmod_version=0.12.2`. Public filenames use 0.1 while the JAR metadata preserves the development labels; see [Installation](Installation.md).

## Source map

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/dyslexic/cinematic/` | NeoForge reference implementation |
| `src/main/resources/` | Shared translations, textures, icon, and hammer model |
| `src/main/templates/` | NeoForge metadata template |
| `fabric/src/main/` | Base Fabric implementation |
| `fabric/src/1.21.x/` | Replacements for changed Minecraft APIs |
| `fabric/build.gradle` | Version targets, layer selection, and small source substitutions |
| `tools/`, `fabric/tools/` | Showcase, inspection, and build helpers |
| `docs/storyboards/` | Recorded development frames |

Core packages cover station sessions, vanilla menu actions, crafting tables, furnaces, brewing, network packets, client scenes, and mixins. The client scene coordinates approach, active work, and exit; each station supplies its layout, camera pose, controls, and timeline.

Fabric uses the latest applicable file from each source layer. Do not edit generated files under `build/<Minecraft>/generated/java/`. Shared changes may require updating several overlays. Check the exact Minecraft API rather than assuming it is the same between minor releases.

## Visual verification

The development-only Showcase creates a separate test world, runs station actions, checks state, and saves screenshots. It is not active in ordinary play.

```sh
# NeoForge, repository root (Bash and Python required)
bash tools/showcase.sh
ONLY=true FROM=a bash tools/showcase.sh
```

```powershell
# Fabric, from fabric/
.\tools\showcase.ps1 -Version '1.21.11' -From 'a' -Only
.\tools\showcase.ps1 -Version '1.21.11' -WithSettings
```

Shaders require compatible Iris/Sodium libraries, a locally installed shader pack, and enabled shader configuration; see [the Fabric guide](https://github.com/dcvke/CinematicInteractions/blob/main/fabric/README.md).

Do not run two clients of the same target simultaneously, or rebuild/start its server while its Showcase client is running: they share build outputs. Inspect the saved frames for camera jumps, item visibility, hand alignment, and missing effects. The Actions build matrix checks compilation and packaging only; it does not perform in-game visual verification.

Current and historical verification limits are documented in [fabric/TESTING.md](https://github.com/dcvke/CinematicInteractions/blob/main/fabric/TESTING.md). Dedicated-server startup and an observer simulation are not equivalent to testing two real players on a server.

See [CONTRIBUTING.md](https://github.com/dcvke/CinematicInteractions/blob/main/CONTRIBUTING.md) before opening a pull request.
