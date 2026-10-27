# 🛠️ Developer guide

## Requirements

Install **JDK 25** (Fabric 26.x targets it; the other targets compile for Java 17 or 21 through Gradle toolchains) and set `JAVA_HOME` to it. Use the checked-in Gradle wrapper; a global Gradle installation is not required. Dependencies, optional integrations included, come from Maven repositories (Modrinth Maven among them), so no local JARs are needed.

## Build

The mod is built by two [Stonecutter](https://stonecutter.kikugie.dev/) projects, split where Minecraft's rendering API changed:

| Project | Targets | Code written out in `src/` |
| --- | --- | --- |
| repository root | NeoForge 1.21.1, Forge 1.20.1, Fabric 1.20.1 and 1.21.1 | `1.21.1-neoforge` |
| [`multiversion/`](../../multiversion/README.md) | Fabric 1.21.11, 26.1.1, 26.1.2, 26.2, 26.3 | `26.3` |

```sh
# repository root: JARs in versions/<target>/build/libs/
./gradlew build
./gradlew :1.20.1-forge:runClient

# multiversion/
./gradlew build
./gradlew :26.3:runClient
```

Each `src/` holds the code of its active target; the other targets' code sits next to it in `//? if` comments. To edit another target as plain Java, run `./gradlew "Set active project to <target>"`, and `./gradlew "Reset active project"` before committing.

Fabric 1.21–1.21.10 (0.12.x) are no longer built.

## Source map

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/dyslexic/cinematic/` | Root code: NeoForge 1.21.1 written out, the other root targets in comments |
| `src/main/resources/` | Shared translations, textures, icon, and hammer model (both projects use them) |
| `src/<loader>/`, `src/1.20.1/` | Metadata and resources of one loader or version |
| `stonecutter.gradle.kts`, `stonecutter.properties.toml` | Renames between root targets, and each target's dependency versions |
| `multiversion/src/main/` | Fabric 1.21.11–26.3 code (26.3 written out) and its Fabric metadata |
| `multiversion/stonecutter.gradle.kts` | Renames between 1.21.11 and the 26.x versions |
| `tools/` | Showcase, inspection, and build helpers |
| `docs/storyboards/` | Recorded development frames |

Core packages cover station sessions, vanilla menu actions, crafting tables, furnaces, brewing, network packets, client scenes, and mixins. The client scene coordinates approach, active work, and exit; each station supplies its layout, camera pose, controls, and timeline.

The two projects hold separate copies of the mod code, so a fix that both need is made in both. Check the exact Minecraft API rather than assuming it is the same between minor releases.

## Visual verification

The development-only Showcase creates a separate test world, runs station actions, checks state, and saves screenshots. It is not active in ordinary play.

```sh
# NeoForge, repository root (Bash and Python required)
bash tools/showcase.sh
ONLY=true FROM=a bash tools/showcase.sh

# Fabric, from multiversion/
./gradlew :26.3:runShowcase -Pfrom=a -Ponly=true
```

Shaders require compatible Iris/Sodium libraries, a locally installed shader pack, and enabled shader configuration; in `multiversion/`, add `-PwithShaders`.

Do not run two clients of the same target simultaneously, or rebuild/start its server while its Showcase client is running: they share build outputs. Inspect the saved frames for camera jumps, item visibility, hand alignment, and missing effects. The Actions build matrix checks compilation and packaging only; it does not perform in-game visual verification.

Historical verification limits of the Fabric 1.21.x builds are documented in [docs/ports/fabric-1.21/TESTING.md](https://github.com/dcvke/CinematicInteractions/blob/main/docs/ports/fabric-1.21/TESTING.md). Dedicated-server startup and an observer simulation are not equivalent to testing two real players on a server.

See [CONTRIBUTING.md](https://github.com/dcvke/CinematicInteractions/blob/main/CONTRIBUTING.md) before opening a pull request.
