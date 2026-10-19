# Cinematic Interactions add-on API (NeoForge 1.21.1 preview)

This preview lets a NeoForge add-on give **its own menu-backed block** a first-person Cinematic Interactions scene. Cinematic Interactions handles approach, camera, hands, inventory drawer, exit, session lifetime and the usual window fallback. Your block and menu keep authority over item slots, recipes, costs and rewards.

The API ships **inside the Cinematic Interactions mod JAR**. An add-on declares `cinematic` as a required dependency on both client and server and compiles against the same Minecraft/NeoForge version as the host. It does not bundle a second copy of Cinematic Interactions. The public entry points are `dev.dyslexic.cinematic.api.AddonStations`, `dev.dyslexic.cinematic.api.client.AddonScenes` and `AddonMenuStation`. A [small source example](../examples/addon-template/) shows registration and a nine-cell menu scene; it assumes your add-on has already registered its own block.

## Register the block on both sides

Call this during your mod's normal registration/setup, after your block has a registry ID and before a world is joined:

```java
AddonStations.register(new AddonStations.Definition(
    ResourceLocation.fromNamespaceAndPath("yourmod", "press_scene"),
    ResourceLocation.fromNamespaceAndPath("yourmod", "press"),
    YourPressMenu.class,
    state -> List.of(state.getValue(HorizontalDirectionalBlock.FACING))
));
```

The first ID names the scene; the second names your registered block. `YourPressMenu` is the **actual menu class** opened by that block's `MenuProvider`. Supply the horizontal sides from which the player may work. An empty list disables the scene for that block state. Only modded blocks may register, and one block may have one scene definition. If your block has different kinds of menus, use a common base class only when all of them genuinely support your scene's slot layout.

## Register the client scene

In your client setup, register a factory with the same scene ID:

```java
AddonScenes.register(
    ResourceLocation.fromNamespaceAndPath("yourmod", "press_scene"),
    (pos, state, player, side) -> new YourPressScene(pos, side)
);
```

Extend `AddonMenuStation<YourPressMenu>` for a menu-backed scene. Its constructor takes the block position, working side, menu class and number of leading input slots used for the optional exit animation. Pass `0` if those slots are not the first slots of your menu or if your menu keeps its inputs in the block. Implement the camera, hand rests, cursor target, interaction, drawing and hint methods inherited from `ClientStation` and `MenuStation`. Use `slotClick(index, button, shift)` for real slot actions. That sends the same menu click as a normal container screen; do not duplicate recipes or invent client-side results. `renderScene` draws persistent scene content through intro, active work and outro, while `renderWorld` is for extras during active work. The existing station classes in `client/` are useful examples.

Your block must already open its menu normally. If a scene cannot be reached, the player gets that menu. Sneaking also keeps the block's normal use. If a different menu appears, Cinematic Interactions yields to its screen. An add-on may still render its own block entity for observers; this first API does **not** automatically publish arbitrary menu slots or custom effects to other players.

## Required dependency

In your add-on's `neoforge.mods.toml`, declare Cinematic Interactions as required:

```toml
[[dependencies.yourmod]]
modId="cinematic"
type="required"
versionRange="[0.18.12,)"
ordering="AFTER"
side="BOTH"
```

Download the [0.18.12 NeoForge JAR](https://github.com/dcvke/CinematicInteractions/releases/tag/v0.18.12-neoforge-1.21.1), put it in your add-on project's `libs/` directory, and add it as a compile dependency in `build.gradle`:

```groovy
dependencies {
    compileOnly files('libs/CinematicInteraction-0.18.12-NeoForge-1.21.1.jar')
}
```

Add the same JAR to the game's `mods/` directory when testing or playing; your add-on does not include a copy of it. Update the filename in the Gradle line if you use a newer host release. The API is currently a **NeoForge 1.21.1 preview**, not a compatibility promise for Fabric or Forge ports. Add-on IDs, menu types and slot behavior should stay the same on client and server. Test in both singleplayer and on a dedicated server before distributing an add-on.

## Current scope

Supported: custom blocks with a normal menu, their own first-person scene, real menu slot interactions, inventory quick-move, and window fallback. Not yet exposed: menu-less stations, automatic spectator item rendering, custom network actions/effects, per-add-on layout editor profiles, and Fabric/Forge ports. Those are future API work, not behavior promised by this preview.
