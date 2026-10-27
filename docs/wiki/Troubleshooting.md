# 🩺 Troubleshooting & FAQ

## The game will not start

Check that the JAR matches the exact Minecraft version and loader. Remove duplicate Cinematic Interactions JARs. Fabric also needs a matching Fabric API. If the problem remains, attach the relevant crash report or `logs/latest.log` to a bug report.

## A block opens its normal screen

Check that its scene is enabled. When a workstation is too high, too low, obstructed, or cannot be approached safely, a short notice above the hotbar says why; with height logic enabled, the normal screen opens instead. Leave enough space to stand at the working side of the block. Chests and barrels intentionally keep normal screens.

## How do I sit at a campfire?

Look at the campfire and press **H** (or your rebound key). Right-click remains the ordinary Minecraft campfire interaction. The mod needs a safe place on the ground to sit.

## Can I use it on a vanilla server?

No. Both sides need the mod; Fabric also needs Fabric API on both sides. The mod synchronizes workstation contents, poses, and effects through its own packets.

## Does it support shaders?

Iris/Sodium combinations have been checked on selected versions, including 1.21.10 and 1.21.11 with Complementary Reimagined. Compatibility with every shader, resource pack, or modpack is not guaranteed. See [recorded test coverage](https://github.com/dcvke/CinematicInteractions/blob/main/docs/ports/fabric-1.21/TESTING.md).

For a rendering issue, report whether it also happens without shaders and include the exact Iris/Sodium versions, shader pack, resource packs, and a recording if possible.

## Do recipe mods have a custom interface here?

The crafting guide is built in. Custom JEI presentation inside the scene has not been implemented. Use the ordinary crafting screen for recipes the guide cannot lay out.

## What should I include in a report?

Use the [bug report form](https://github.com/dcvke/CinematicInteractions/issues/new?template=bug_report.yml). Include game, loader, mod versions, the affected block, reproduction steps, expected and actual behavior, and relevant media or logs. Remove private server addresses, chat, and other personal details before uploading logs. English and Russian reports are accepted.
