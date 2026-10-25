# ⚙️ Settings

## Open the configuration

- **NeoForge:** Mods → Cinematic Interactions → Config. The mod has its own settings screen: a search box over four tabs (General, Stations, Camera, Advanced). Point at an option to see what it does in the box under the list. A ↺ button resets a changed option; **Reset all** resets every option. Changes apply with **Done** or Esc; **Cancel** discards them.
- **Fabric:** use the config button in Mod Menu. Cloth Config is optional; without it, the mod supplies a basic screen. Without Mod Menu, edit the configuration file while the game is closed.

| Loader | File |
| --- | --- |
| NeoForge | `config/cinematic-client.toml` |
| Fabric | `config/cinematic-client.json` |

These are player preferences. Settings are not automatically transferred between the TOML and JSON formats.

## General

- **When a station can't be reached:** **Show why** (default) displays a short notice above the hotbar, such as "Too high". **Open usual window** opens the block's normal interface instead. In the files this is `heightLogic` (`false` shows why).
- Control hints during scenes and the campfire **[H]** invitation can be shown or hidden.
- **Orbit view key:** hold or toggle, with a rebindable key.

## Stations

Turn the scene of each station on or off. The stations are grouped as crafting, smelting and cooking, smithing, magic, and other, with **All on** and **All off** buttons. A station turned off opens its usual window.

## Camera

- **Preset:** Cinematic, Balanced (default), Fast, or Minimal sets pace, lean, zoom, and effects at once. Changing one of them by hand shows **Custom**.
- **Reduce motion:** turns off the letterbox bars, the breathing camera, shake, and flashes at once, whatever each is set to.
- Approach and leaving speed: **50–200%**.
- Quick return: a shorter entrance when you return to the same station within the configured **5–30 second** window (default **12 seconds**).
- Camera lean and zoom: **0–100%**.
- Letterbox bars, breathing camera, camera shake, and flashes can be toggled individually.

If the camera motion feels too strong, turn on **Reduce motion** or pick the **Minimal** preset. These preferences apply to the local player's presentation.

## Advanced

- **Client-only mode:** local scenes over vanilla menus on servers without the mod. Change it from the main menu; it applies on the next connection.
- **Developer mode:** the layout editors and their **G** shortcut. With it on, the layout profile of every station can be chosen here.

Both English and Russian translations are included.
