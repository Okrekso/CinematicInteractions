# 🤝 Contributing

Bug reports, small fixes, translations, and carefully scoped station improvements are welcome. Reports can be written in English or Russian.

## Report a problem

Use the [issue forms](https://github.com/dcvke/CinematicInteractions/issues/new/choose). Include the exact Minecraft version, loader and loader version, Cinematic Interactions version shown in the mod list, and steps to reproduce the problem. For visual issues, include a screenshot or short recording and the names of any rendering mods, shaders, or resource packs. Remove private details from logs before sharing them.

## Propose a feature

Describe the player's action and the resulting behavior. For new stations or large changes, open a feature request before investing in an implementation so the intended scope can be discussed.

## Make a code change

1. Fork the repository and create a branch for one change.
2. Use **JDK 21** and the included Gradle wrapper. See [Development](docs/wiki/Development.md).
3. Keep the original NeoForge behavior as the reference. If shared behavior changes, update each affected Fabric source layer as well.
4. Keep in-game strings in both `ru_ru.json` and `en_us.json`.
5. Build the affected targets. For animation, camera, picking, or rendering changes, run the relevant Showcase act and inspect its screenshots; a successful compilation cannot verify the appearance.
6. Explain what changed, why, and how it was checked in the pull request. Mention which loader and Minecraft versions were tested.

Do not commit build output, game worlds, local options, credentials, downloaded mods, shader packs, or decompiled Minecraft sources. Do not publish JARs in the source tree; they belong in GitHub Releases.

New work must be compatible with the project's MIT license. Keep third-party attribution and license terms.

## Build an add-on

The [add-on API guide](docs/addon-api.md) explains how to attach a first-person scene to a custom menu-backed block on NeoForge 1.21.1. The [source example](examples/addon-template/) shows the registration hooks and a minimal scene. Add-ons are separate mods and declare Cinematic Interactions as a required dependency.
