//? if forge {
/*package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/^* Opt-in two-client regression: one player works at the furnace while the other breaks it. ^/
public final class FurnaceBreakSmoke {
    private FurnaceBreakSmoke() {}

    private static final String ROLE = System.getProperty("cinematic.furnaceBreakRole", "");
    private static final BlockPos FURNACE = new BlockPos(2, -60, 0);
    private static int ticks;
    private static boolean started;
    private static boolean broken;

    static void tick() {
        if (ROLE.isEmpty() || broken && ROLE.equals("breaker")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        ticks++;
        if (ticks > 2400) {
            Cinematic.LOGGER.error("[FurnaceBreakSmoke] FAIL: timeout role={} phase={}", ROLE, Scene.phase);
            mc.stop();
            return;
        }
        if (ROLE.equals("worker")) {
            if (ticks % 100 == 0 && !started) Cinematic.LOGGER.info("[FurnaceBreakSmoke] waiting at={} block={}",
                    mc.player.position(), mc.level.getBlockState(FURNACE).getBlock());
            if (!started && StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE)) && mc.player.position().z > 2
                    && mc.player.onGround() && mc.screen == null) {
                Scene.begin(FURNACE, StationKind.FURNACE);
                started = true;
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker used furnace phase={} grounded={} screen={} refused={}",
                        Scene.phase, mc.player.onGround(), mc.screen, Scene.refused);
            }
            if (started && ticks % 100 == 0 && Scene.phase != Scene.Phase.ACTIVE)
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker phase={} block={}", Scene.phase, mc.level.getBlockState(FURNACE).getBlock());
            if (started && Scene.phase == Scene.Phase.ACTIVE && StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))) {
                if (ticks % 100 == 0) Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker ready");
            }
            if (started && !StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))) {
                if (Scene.phase == Scene.Phase.NONE || Scene.phase == Scene.Phase.OUTRO) {
                    Cinematic.LOGGER.info("[FurnaceBreakSmoke] PASS: worker scene closed after furnace was broken");
                    broken = true;
                    mc.stop();
                }
            }
        } else if (ROLE.equals("breaker") && started && !StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))) {
            Cinematic.LOGGER.info("[FurnaceBreakSmoke] breaker removed furnace");
            broken = true;
            mc.stop();
        } else if (ROLE.equals("breaker") && !started && mc.player.position().x > 1.5
                && mc.player.position().x < 3.5 && mc.player.position().z > 2
                && StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))) {
            if (mc.gameMode.startDestroyBlock(FURNACE, Direction.SOUTH)) {
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] breaker attacked furnace");
                started = true;
            }
        }
    }
}
*///?}
