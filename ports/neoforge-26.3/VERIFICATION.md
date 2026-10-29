# Build and test notes

Minecraft 26.3; NeoForge 26.3.0.95; Java 25. Tested locally on Windows on 2026-10-29.

The tests launched the packaged mod JAR in separate temporary game folders, using the installed Minecraft libraries and an offline development identity. They did not use an authenticated account or existing player saves.

| Walkthrough | Passed | Failed | Test configuration |
|---|---:|---:|---|
| Crafting | 81 | 0 | Port alone; before HUD correction |
| Furnace | 26 | 0 | Port alone; before HUD correction |
| Campfire and soul campfire | 77 | 0 | Final JAR with all 12 installed mods |
| Brewing stand | 33 | 0 | Final JAR with all 12 installed mods |
| Chiseled bookshelf | 49 | 0 | Final JAR with five installed rendering/animation mods |
| Loom | 37 | 0 | Final JAR with five installed rendering/animation mods |

Total: 303 passed, 0 failed. These are the upstream scripted interaction checks, with visual inspection of generated screenshots. The early crafting/furnace runs preceded the isolated HUD correction. The campfire runs used the final production code before a test-only brewing assertion was updated to inspect the mod’s synchronized bottle cache, which is what the renderer consumes. The remaining runs used the final packaged JAR.

The final HUD correction was visually verified with the normal hotbar and health indicators hidden during a scene. All 32 configured mixin classes are present. All 321 compiled mod classes target Java 25 and contain no Fabric API or Mod Menu references. Archive integrity and required NeoForge metadata, access rules and license files passed inspection.

The installed-mod runs included AmbientSounds, CreativeCore, Distant Horizons, Enchanted Fishing Line, Iris, Lithostitched, Punchy, Smooth Player Animations, Smooth Swapping, Sodium, Sound Physics Remastered and Tectonic. The rendering-focused runs used Iris, Sodium, Punchy, Smooth Player Animations and Smooth Swapping. Shader packs were disabled.

Not verified: shader packs, dedicated servers, a real multiplayer connection, every workstation or game setting, and optional JEI/Polymorph/inventory-effect integrations. This remains an unofficial experimental port. An initial restricted-environment launch and a saved-world experimental-settings prompt were resolved by permitting local test networking and using fresh temporary worlds; neither was an interaction-test failure.

Playable JAR SHA-256: `02771e0b5562424610a9da9696ddf8c32a367ccdbddd8ce806146e452bc97b4d`
