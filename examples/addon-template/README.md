# Minimal add-on scene example

`ExampleAddonRegistration` and `ExampleAddonScene` are a compile-checked illustration, not an installable mod. Adapt them in your own NeoForge 1.21.1 add-on. Register a real block at `exampleaddon:custom_crafting_table` which opens `CraftingMenu`, call `registerCommon()` during common mod setup, and call `registerClient()` during client setup. Replace the sample IDs, menu and scene geometry with your own. See [the API guide](../../docs/addon-api.md) for the dependency and scope.

The example only touches the nine input cells. A production crafting scene should also draw and click the result slot and check its block model and texture in game.
