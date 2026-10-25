# ⚙️ Settings

## Open the configuration

- **NeoForge:** Mods → Cinematic Interactions → Config. Cloth Config gives the expanded screen; otherwise the NeoForge configuration screen is used.
- **Fabric:** use the config button in Mod Menu. Cloth Config is optional; without it, the mod supplies a basic screen. Without Mod Menu, edit the configuration file while the game is closed.

| Loader | File |
| --- | --- |
| NeoForge | `config/cinematic-client.toml` |
| Fabric | `config/cinematic-client.json` |

These are player preferences. Settings are not automatically transferred between the TOML and JSON formats.

## Scenes

Enable or disable individual stations. Disabled stations use their ordinary interface or vanilla interaction. Height logic is disabled by default: a short notice explains why a scene cannot begin. With height logic enabled, an inaccessible station falls back to its normal interface instead.

## Interface

Toggle control hints and the campfire invitation. Both English and Russian translations are included.

## Camera & comfort

- Entrance and exit speed: **50–200%**.
- Camera lean and scene zoom: **0–100%**.
- Quick return: a shorter entrance when you return to the same station within the configured **5–30 second** window (default **12 seconds**).
- Cinematic bars, idle camera motion, camera shake, and flashes can be toggled individually.
- Orbit view supports hold or toggle behavior, with a rebindable key.

If the camera motion feels too strong, reduce lean and zoom and disable idle motion, shake, and flashes. These preferences apply to the local player's presentation.
