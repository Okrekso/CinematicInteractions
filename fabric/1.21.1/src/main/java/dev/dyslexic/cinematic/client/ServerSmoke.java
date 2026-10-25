package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Manual opt-in check against a dedicated server with the mod (the joinServer run): unlike singleplayer, every
 * packet is encoded. Shift-clicks from the drawer onto a table, into a furnace and a brewing stand, and what the
 * server sends back (table, furnace and brewing stand contents); then the inventory opened while stepping away from the
 * stand, after which its scene must play again.
 */
public final class ServerSmoke {
    private ServerSmoke() {}

    private static final boolean ENABLED = Boolean.getBoolean("cinematic.serverSmoke");
    /** -Dcinematic.serverSmoke.reload=true: the same server restarted after a run, what was put in must be there still. */
    private static final boolean RELOAD = Boolean.getBoolean("cinematic.serverSmoke.reload");
    private static final BlockPos TABLE = new BlockPos(0, -60, 0);
    private static final BlockPos FURNACE = new BlockPos(2, -60, 0);
    private static final BlockPos BREWING = new BlockPos(-2, -60, 0);
    private static int stage = RELOAD ? 50 : 1;
    private static int ticks;
    private static int inventoryRounds;

    public static void tick() {
        if (!ENABLED || stage == 99 || stage == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (++ticks > 900) {
            fail("timed out at stage " + stage + ", phase " + Scene.phase);
            return;
        }
        switch (stage) {
            case 50 -> {
                // the table's own store, the furnace and the stand reach a client that joins the restarted server
                ClientTables.Entry table = ClientTables.get(TABLE);
                boolean planks = table != null && java.util.Arrays.stream(table.items).anyMatch(s -> s.is(Items.OAK_PLANKS) && s.getCount() == 16);
                boolean ore = ClientFurnaces.item(FURNACE, 0).is(Items.RAW_IRON) && ClientFurnaces.item(FURNACE, 0).getCount() == 5;
                boolean potion = mc.level.getBlockEntity(BREWING) instanceof BrewingStandBlockEntity stand
                        && java.util.stream.IntStream.range(0, 3).anyMatch(i -> ClientBrewing.item(stand, i).is(Items.POTION));
                if (!planks || !ore || !potion || ticks < 40) return;
                Cinematic.LOGGER.info("[ServerSmoke] PASS: after a restart the table still holds 16 planks, the furnace 5 raw iron, the stand its bottle");
                stage = 99;
                mc.stop();
            }
            case 1 -> {
                // the test script places the blocks and gives the items through RCON
                if (!StationKind.TABLE.matches(mc.level.getBlockState(TABLE)) || !StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))
                        || !(mc.level.getBlockEntity(BREWING) instanceof BrewingStandBlockEntity)
                        || !mc.player.getInventory().getItem(12).is(Items.POTION) || mc.player.position().z < 2) return;
                if (!ClientOnly.serverHasMod()) {
                    fail("the server should have the mod");
                    return;
                }
                use(mc, TABLE, new Vec3(0.5, 1, 0.5), Direction.UP);
                next();
            }
            case 2 -> {
                if (!active(StationKind.TABLE) || ticks < 25) return;
                click(mc, 9);
                next();
            }
            case 3 -> {
                ClientTables.Entry entry = ClientTables.get(TABLE);
                boolean laid = entry != null && java.util.Arrays.stream(entry.items).anyMatch(s -> s.is(Items.OAK_PLANKS) && s.getCount() == 16);
                if (!laid || !mc.player.getInventory().getItem(9).isEmpty()) return;
                Cinematic.LOGGER.info("[ServerSmoke] shift-click laid 16 planks on the table, the server sent them back");
                Scene.leave();
                next();
            }
            case 4 -> {
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) return;
                use(mc, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                next();
            }
            case 5 -> {
                if (!active(StationKind.FURNACE) || ticks < 25) return;
                click(mc, 10);
                click(mc, 11);
                next();
            }
            case 6 -> {
                boolean ore = ClientFurnaces.item(FURNACE, 0).is(Items.RAW_IRON) && ClientFurnaces.item(FURNACE, 0).getCount() == 5;
                boolean dirt = mc.player.getInventory().getItem(11).is(Items.DIRT) && mc.player.getInventory().getItem(11).getCount() == 3;
                if (!ore || !mc.player.getInventory().getItem(10).isEmpty()) return;
                if (!dirt) {
                    fail("the dirt left its slot");
                    return;
                }
                Cinematic.LOGGER.info("[ServerSmoke] furnace: raw iron in at the top, dirt left where it was");
                Scene.leave();
                next();
            }
            case 7 -> {
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) return;
                use(mc, BREWING, new Vec3(0.5, 1, 0.5), Direction.UP);
                next();
            }
            case 8 -> {
                if (!active(StationKind.BREWING) || ticks < 25) return;
                click(mc, 12);
                next();
            }
            case 9 -> {
                if (!(mc.level.getBlockEntity(BREWING) instanceof BrewingStandBlockEntity stand)) return;
                boolean shown = false;
                for (int i = 0; i < 3; i++) shown |= ClientBrewing.item(stand, i).is(Items.POTION);
                if (!shown || !mc.player.getInventory().getItem(12).isEmpty()) return;
                Cinematic.LOGGER.info("[ServerSmoke] brewing stand: the bottle went onto a foot and its contents packet arrived");
                Scene.leave();
                next();
            }
            case 10 -> {
                // stepping away and opening the inventory at once, three times over: the stand must play its scene again
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) return;
                if (inventoryRounds == 3) {
                    Cinematic.LOGGER.info("[ServerSmoke] PASS: dedicated server with the mod, shift-clicks into table, furnace and brewing stand,"
                            + " the inventory opened while stepping away three times and the scene played again each time");
                    stage = 99;
                    mc.stop();
                    return;
                }
                use(mc, BREWING, new Vec3(0.5, 1, 0.5), Direction.UP);
                next();
            }
            case 11 -> {
                if (ticks == 60 && !active(StationKind.BREWING)) {
                    fail("round " + inventoryRounds + ": the stand did not play its scene (screen "
                            + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()) + ")");
                    return;
                }
                if (!active(StationKind.BREWING) || ticks < 25) return;
                Scene.leave();
                next();
            }
            case 12 -> {
                // the work's screen has gone after 0.22 s: the game's own key opens the inventory, still stepping away
                if (ticks == 8 + inventoryRounds * 3) {
                    net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(mc.options.keyInventory));
                }
                if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
                Cinematic.LOGGER.info("[ServerSmoke] round {}: inventory open while {}", inventoryRounds + 1, Scene.phase);
                next();
            }
            case 13 -> {
                if (Scene.phase != Scene.Phase.NONE || ticks < 10) return;
                mc.setScreen(null);
                inventoryRounds++;
                stage = 10;
                ticks = 0;
            }
            default -> {}
        }
    }

    private static boolean active(StationKind kind) {
        return Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && Scene.station.kind() == kind && !Scene.clientOnly()
                && Minecraft.getInstance().screen instanceof SceneScreen;
    }

    private static void use(Minecraft mc, BlockPos pos, Vec3 local, Direction face) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos).add(local), face, pos, false));
    }

    private static void click(Minecraft mc, int inventorySlot) {
        if (mc.screen instanceof SceneScreen screen) screen.panelClick(inventorySlot < 9 ? 36 + inventorySlot : inventorySlot, 0, true);
    }

    private static void next() {
        stage++;
        ticks = 0;
    }

    private static void fail(String reason) {
        Cinematic.LOGGER.error("[ServerSmoke] FAIL: {}", reason);
        stage = -1;
        Minecraft.getInstance().stop();
    }
}
