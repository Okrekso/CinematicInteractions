# 🧵 Fabric ports

One build per Minecraft version: **1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11**.

[Installation & dependencies](../docs/wiki/Installation.md) · [Developer guide](../docs/wiki/Development.md) · [Recorded test coverage](TESTING.md)

## Build

Use **JDK 21**, then from this directory:

```sh
./gradlew -Pmc_version=1.21.11 build
```

Use `gradlew.bat` on Windows. Output: `build/1.21.11/libs/`. To build all versions in PowerShell:

```powershell
.\tools\build-all.ps1
```

The helper copies game JARs to a local `../release/fabric/` directory and writes checksums. That directory is git-ignored; downloadable binaries are published in [GitHub Releases](https://github.com/dcvke/CinematicInteractions/releases).

Current source version: **0.12.3**. The published 1.21–1.21.9 binaries retain **0.12.2**; use `-Pmod_version=0.12.2` to build with that version label. The 1.21.10/1.21.11 binaries use **0.12.3**. Initial public filenames carry **0.1**.

## Source layers

`src/main/` supplies the base. Overlays in `src/1.21.2/`, `1.21.4/`, `1.21.5/`, `1.21.6/`, `1.21.9/`, `1.21.10/`, and `1.21.11/` replace files whose Minecraft APIs changed. The last applicable layer wins. Small API substitutions are maintained in `sourcePatches` in `build.gradle`.

Resources are shared with `../src/main/resources/`; the Fabric project must remain within the complete repository. Generated Java under `build/<version>/generated/java/` is not the source of truth.

## Showcase

```powershell
.\tools\showcase.ps1 -Version '1.21.11' -From 'a' -Only
.\tools\showcase.ps1 -Version '1.21.11' -WithSettings
```

The walkthrough creates a separate local world and records checks and screenshots. With shaders, place matching Iris/Sodium and required nested libraries in `test-mods/<version>/` where needed; place your shader pack in `run/<version>/showcase-iris/shaderpacks/` and enable it in that instance's Iris configuration. The pinned 1.21.10/1.21.11 Iris and Sodium builds are resolved through Maven, but the helper still checks local JARs for the nested library extraction.

Do not rebuild, start another Showcase, or start a server for the same version while its client is running. Different cache directories do not isolate its compiled output. See [TESTING.md](TESTING.md) for coverage, supported shader combinations, and known verification limits.
