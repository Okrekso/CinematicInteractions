package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Manual opt-in integration check against a separate, unmodded Minecraft server. */
public final class ClientOnlySmoke {
    private ClientOnlySmoke() {}

    private static final boolean ENABLED = Boolean.getBoolean("cinematic.clientOnlySmoke");
    private static final BlockPos TABLE = new BlockPos(0, 64, 1);
    private static final BlockPos FURNACE = new BlockPos(1, 64, 1);
    private static final BlockPos ANVIL = new BlockPos(1, 64, 3);
    private static int stage;
    private static int ticks;
    private static Vec3 original;

    public static void onScreen(Screen screen) {
        if (!ENABLED || stage != 0 || !(screen instanceof TitleScreen)) return;
        stage = 1;
        Settings.CLIENT_ONLY_MODE.set(true);
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> ConnectScreen.startConnecting(screen, mc, ServerAddress.parseString("127.0.0.1:25586"),
                new ServerData("Client-only smoke", "127.0.0.1:25586", ServerData.Type.OTHER), false, null));
    }

    public static void tick() {
        if (!ENABLED || stage == 0 || stage == 99 || stage == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 600) {
            fail("timed out at stage " + stage);
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (ClientOnly.serverHasMod()) {
            fail("the test server unexpectedly has Cinematic Interactions");
            return;
        }
        switch (stage) {
            case 1 -> {
                if (!StationKind.TABLE.matches(mc.level.getBlockState(TABLE)) || mc.player.distanceToSqr(TABLE.getCenter()) > 9
                        || Math.abs(mc.player.getX() - 0.9) > 0.05
                        || mc.player.getInventory().getItem(0).getItem() != Items.OAK_LOG) return;
                original = mc.player.position();
                var hit = new BlockHitResult(new Vec3(0.5, 65, 1.5), Direction.UP, TABLE, false);
                clickAsPlayer(mc, hit);
                if (mc.player.swinging) {
                    fail("vanilla hand swung before the client-only scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] used table at {} without an initial swing", original);
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                if (ticks == 80) Cinematic.LOGGER.info("[ClientOnlySmoke] waiting: screen={}, menu={}, scene={}, refused={}",
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(),
                        mc.player.containerMenu.getClass().getSimpleName(), Scene.phase, Scene.refused);
                if (!(mc.player.containerMenu instanceof CraftingMenu) || !Scene.clientOnly()) return;
                if (Scene.phase != Scene.Phase.ACTIVE) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved during the scene");
                    return;
                }
                CraftingMenu menu = (CraftingMenu) mc.player.containerMenu;
                mc.gameMode.handleInventoryMouseClick(menu.containerId, 37, 0, ClickType.PICKUP, mc.player);
                TableStation table = Scene.station(TableStation.class);
                table.hoverCell = 4;
                table.click(1, false);
                mc.gameMode.handleInventoryMouseClick(menu.containerId, 37, 0, ClickType.PICKUP, mc.player);
                stage = 3;
                ticks = 0;
            }
            case 3 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) {
                    fail("the vanilla crafting menu closed before the recipe synced");
                    return;
                }
                if (menu.getSlot(0).getItem().getItem() != Items.OAK_PLANKS) return;
                Scene.station(TableStation.class).craft(false);
                stage = 4;
                ticks = 0;
            }
            case 4 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) {
                    fail("the vanilla crafting menu closed before taking the result");
                    return;
                }
                if (menu.getCarried().getItem() != Items.OAK_PLANKS) return;
                Scene.leave();
                stage = 5;
                ticks = 0;
            }
            case 5 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved on exit");
                    return;
                }
                if (!StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))
                        || !StationKind.ANVIL.matches(mc.level.getBlockState(ANVIL))) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(1.5, 64.5, 2.0), Direction.SOUTH, FURNACE, false));
                stage = 6;
                ticks = 0;
            }
            case 6 -> {
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE
                        || Scene.station.kind() != StationKind.FURNACE || !(mc.player.containerMenu instanceof AbstractFurnaceMenu)) return;
                Scene.leave();
                stage = 7;
                ticks = 0;
            }
            case 7 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(1, 64.5, 3.5), Direction.WEST, ANVIL, false));
                stage = 8;
                ticks = 0;
            }
            case 8 -> {
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE
                        || Scene.station.kind() != StationKind.ANVIL || !(mc.player.containerMenu instanceof AnvilMenu)) return;
                Scene.leave();
                stage = 9;
                ticks = 0;
            }
            case 9 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved during a furnace or anvil scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] PASS: vanilla server, crafting result, furnace and anvil scenes, menu close, no player movement");
                stage = 99;
                mc.stop();
            }
            default -> {}
        }
    }

    /** Exercise Minecraft's actual use-key path, including its predicted hand animation. */
    private static void clickAsPlayer(Minecraft mc, BlockHitResult hit) {
        mc.hitResult = hit;
        try {
            var use = Minecraft.class.getDeclaredMethod("startUseItem");
            use.setAccessible(true);
            use.invoke(mc);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot simulate the use key", e);
        }
    }

    private static void fail(String reason) {
        Cinematic.LOGGER.error("[ClientOnlySmoke] FAIL: {}", reason);
        stage = -1;
        Minecraft.getInstance().stop();
    }
}
