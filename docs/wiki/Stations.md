# 🧩 Station guide

| Station | Interaction |
| --- | --- |
| Crafting table | Visible 3×3 tabletop, recipe guide, ingredient previews, animated recipe layout, and a floating result. Placed items stay on the table and drop if it breaks. Items blocking a targeted cell fade locally to make placement easier. |
| Furnace | Load input and fuel into the front openings and collect output; fuel and cooking progress appear on the block. |
| Smoker | Place food in the opening and fuel behind the lower grille. |
| Blast furnace | Load ore through the top, fuel through the front, and collect ingots from the output area. |
| Crafter | Work with its facing grid and toggle disabled cells; crafting still needs redstone. |
| Enchanting table | Place an item above the book and lapis in the visible blue slot, choose an offered enchantment, and trigger the ritual. |
| Anvil | Combine, repair, enchant, or rename equipment through a three-strike hammer animation. |
| Grindstone | Press equipment against the rotating wheel to repair or remove eligible enchantments. |
| Smithing table | Place a template, equipment, and material, then apply the upgrade or trim. |
| Stonecutter | Choose a shape from the arc around the blade and push the block through the saw. |
| Loom | Preview patterns on the banner and weave the chosen design. |
| Cartography table | Enlarge, lock, or copy the map laid on the tabletop. |
| Brewing stand | Place potions on the feet, add an ingredient and blaze powder, and watch the brew. It continues brewing after you leave. |
| Campfire / soul campfire | Press H to sit down; put food on free cooking spots or take it back. Normal right-click use is preserved. |
| Lectern | Read the real book with animated page turns; its stored page and comparator behavior still update. |
| Chiseled bookshelf | Pull books by their spines, return them to empty slots, and read written books beside the shelf. |
| Jukebox | Insert a music disc into the top slot or remove the playing disc. |
| Beacon | Choose effects around the beam and activate them with a payment item. |

## Shared behavior

Scenes use the vanilla station rules and recipes, with synchronized server actions. The camera approaches from the player's actual position. When a workstation cannot be reached safely or its work area is obstructed, the height-logic setting determines whether the normal interface opens or a short notice explains the obstacle.

Other modded players can see workstation contents, poses, and effects. Full observer movements are implemented for the anvil, grindstone, stonecutter, and bookshelf; not every station reproduces every first-person gesture for observers.

Chests, barrels, and shulker boxes use their normal screens. The recipe guide is built into the mod; a custom JEI interface is not currently implemented. Unusual crafting recipes without a fixed preview can be handled through the normal crafting screen.
