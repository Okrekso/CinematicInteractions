# Changelog

## 0.18.28 beta — October 25, 2026

New beta ports are available for NeoForge 1.21.1, Fabric 1.21.11, Fabric 26.1.1, 26.1.2, 26.2 and 26.3, and Forge 1.20.1. Each has its own prerelease and JAR on the [releases page](https://github.com/dcvke/CinematicInteractions/releases).

- Brought current crafting controls, station interactions, arm styles and entrance options to the supported ports.
- Carried over recipe compatibility and recent campfire, enchantment and rendering improvements where applicable.
- Corrected the bright furnace overlay with shaders in the newer Fabric ports.
- Updated the Forge 1.20.1 port with current scenes, optional Polymorph support and crash prevention when a station is broken during a scene.

**Known issue:** Natural Reach arms can render incorrectly in some situations. Use Stretched or Floating Hands if needed. A fix is planned for a future update. Forge 1.20.1 also has an unconfirmed intermittent connection stall on Forge 47.4.20; please attach a log if you encounter it.

## Beta — October 19, 2026

The latest beta adds an optional client-only preview for supported workstation scenes. It can be enabled from the main menu to use those scenes on servers without Cinematic Interactions installed. Shared items, other players' poses, campfire interactions, and the remaining stations still require the mod on the server.

- **NeoForge 1.21.1 · 0.18.6:** Added the client-only preview and removed the brief hand swing before a local scene starts. Fixed campfire food occasionally staying at 0% while seated. Vanilla recipe synchronization remains unchanged.
- **Fabric 1.21.11 · 0.18.6:** Added the client-only preview and the hand-swing fix. Fixed stalled campfire cooking and disconnections while syncing custom crafting recipes, including the Simple Locks case.
- **Fabric 26.1.1 · 0.18.7:** New supported build with the current scenes, editors and client-only preview. Includes the campfire and custom-recipe fixes. Corrected shader depth for scene effects and the dark lectern book cover.
- **Fabric 26.1.2 · 0.18.7:** New supported build with the same feature set and gameplay fixes. Corrected shader effect depth, the lectern book cover and translucent sleeves in scenes.
- **Fabric 26.2 · 0.18.7:** Added the client-only preview and hand-swing fix; fixed stalled campfire cooking and custom-recipe syncing. Corrected the dark lectern book cover.
- **Fabric 26.3 · 0.18.7:** Added the client-only preview and hand-swing fix; fixed stalled campfire cooking and custom-recipe syncing. Corrected the dark lectern book cover.

All six downloads are **beta** builds. Choose the JAR matching your exact Minecraft version and loader. Previous releases remain available.

## Stable workstation update — October 17, 2026

- **NeoForge 1.21.1 · 0.18.2:** Optional Inventory Particles and Inventory Interactions support now follows visible slots and items in workstation scenes, including items held by animated hands. Developer Mode, layout profiles, campfire progress and the Punchy fix remain included.
- **Fabric 26.2 and 26.3 · 0.18.2:** The same optional inventory effects support, alongside station editors, campfire cooking progress and the Punchy fix.
- **Fabric 1.21.11 · 0.18.3:** Added Developer Mode, in-world layout editors and three local profiles per station, plus optional inventory effects support. Fixed chiseled bookshelf rendering in the editor port. The Punchy fix remains included; campfire cooking progress is not yet available in this build.

Choose the JAR for your exact Minecraft version and loader. Cinematic Interactions is required on both client and server; Fabric API is required on Fabric. Inventory Particles and Inventory Interactions are optional, separate client mods and are not bundled. Previous beta and 0.1 downloads remain available.

## Beta — October 16, 2026

- **NeoForge 1.21.1 · 0.18.1:** Developer Mode, in-world layout editors and local station profiles; campfire cooking progress; improved Punchy compatibility.
- **Fabric 26.2 and 26.3 · 0.18.1:** Full ports of the current station scenes, editors and profiles, campfire cooking progress, and Punchy compatibility fix.
- **Fabric 1.21.11 · 0.12.4:** Punchy hand-rendering and camera-state compatibility fix. This port does not include the layout editors.

These builds are beta releases. Install Cinematic Interactions on both client and server. Fabric builds require Fabric API. Report visual or compatibility issues through [GitHub Issues](https://github.com/dcvke/CinematicInteractions/issues). Earlier 0.1 releases remain available.

Compatibility with Inventory Particles and Inventory Interactions is planned for a later update and is not included here.
