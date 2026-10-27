# Cinematic Interactions — Fabric, every version from one source tree

This project builds the Fabric mod for Minecraft **1.21.11, 26.1.1, 26.1.2, 26.2 and 26.3** from a single `src/`,
with [Stonecutter](https://stonecutter.kikugie.dev/) 0.9. It replaces the separate `fabric/1.21.11` and
`fabric/26.x` projects; the notes of the 26.x ones are kept in [`docs/`](docs/).

## How the versions share code

- `src/` holds the code of the **active** version. The committed state is always **26.3**; the code of other
  versions sits next to it in comments:

  ```java
  //? if >=26.3 {
  pose.rotate(Axis.YP.rotationDegrees(yaw));
  //?} else {
  /*pose.mulPose(Axis.YP.rotationDegrees(yaw));
  *///?}
  ```

- Plain renames between versions (packages, fields, methods) are not written as conditions: they are
  listed once in `stonecutter.gradle.kts` and applied to every file in both directions. They are applied
  in one pass, so a name that changes twice (like `LevelRenderer.getLightColor` →
  `LevelRenderer.getLightCoords` → `LightCoordsUtil.getLightCoords`) is split into pieces that do not
  overlap: the class in one rule, the method in another.
- 1.21.11 is obfuscated, so it is built with the remapping Loom and Mojang mappings; 26.1 and later use the
  plain Loom. The `loom-back-compat` plugin picks the right one.
- Dependency versions per Minecraft version are in `stonecutter.properties.toml`, including the mixins
  that one version does not have (`mixins.exclude`).
- Resources (textures, translations, models) still come from `../src/main/resources` and
  `../fabric/src/1.21.5/resources`, as before.

## Building

```bash
./gradlew build
```

builds every version; the JARs are in `versions/<version>/build/libs/`. One version alone:
`./gradlew :26.2:build`. Running the game: `./gradlew :26.3:runClient` (the same `-PwithShaders`,
`-PwithJei`, `-PwithSettings`, ... switches as before).

## Working on another version

To see and edit a version's code as plain Java (for the IDE), make it active:

```bash
./gradlew "Set active project to 26.1.1"
```

Edit, then **return to 26.3 before committing**:

```bash
./gradlew "Reset active project"
```

When code differs between versions, write both variants in a condition, as above. Conditions use the
Minecraft version (`>=26.1`, `<26.3`, `26.1.2`); a whole file that only some versions have is wrapped in one.
