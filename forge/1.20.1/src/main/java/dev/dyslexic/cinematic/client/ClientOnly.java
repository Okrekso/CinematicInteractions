package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;

/**
 * A server without Cinematic Interactions owns the real menu and all its contents. Only the local presentation of
 * selected vanilla menus is replaced; no mod payload, remote item display or player movement is used.
 */
public final class ClientOnly {
    private ClientOnly() {}

    @Nullable private static BlockPos pendingPos;
    @Nullable private static StationKind pendingKind;
    private static long pendingAt;
    private static boolean enabledForConnection;

    /** A settings change takes effect when the player joins again, never halfway through an open menu. */
    public static void onLogin() {
        enabledForConnection = Settings.clientOnlyMode();
        pendingPos = null;
        pendingKind = null;
    }

    public static boolean serverHasMod() {
        return ClientNet.serverHasChannel();
    }

    /** Called before vanilla sends its block-use packet. True leaves that use untouched. */
    public static boolean onVanillaUse(BlockPos pos, StationKind kind) {
        if (serverHasMod()) return false;
        pendingPos = null;
        pendingKind = null;
        if (enabledForConnection && Settings.scene(kind) && supported(kind)) {
            pendingPos = pos.immutable();
            pendingKind = kind;
            pendingAt = System.nanoTime();
        }
        return true;
    }

    /** The menu opens a few ticks later; keep the vanilla use, but skip its premature hand swing. */
    public static boolean suppressUseSwing(BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabledForConnection || serverHasMod() || mc.player == null || mc.level == null
                || mc.player.isSecondaryUseActive() || mc.player.isSpectator()) return false;
        var state = mc.level.getBlockState(hit.getBlockPos());
        StationKind kind = StationKind.of(state);
        return kind != null && kind.startsOnUse() && supported(kind) && Settings.scene(kind)
                && kind.usable(mc.level, hit.getBlockPos(), state) && kind.usedOn(state, hit.getDirection());
    }

    /** Called after the server opened a real menu, while Minecraft is about to show its normal screen. */
    public static boolean onScreenOpening(Screen next) {
        BlockPos pos = pendingPos;
        StationKind kind = pendingKind;
        if (pos == null || kind == null || next == null) return false;
        if (!(next instanceof MenuAccess<?> window) || !matches(kind, window.getMenu())) {
            pendingPos = null;
            pendingKind = null;
            return false;
        }
        pendingPos = null;
        pendingKind = null;
        Minecraft mc = Minecraft.getInstance();
        return enabledForConnection && System.nanoTime() - pendingAt < 4_000_000_000L && !serverHasMod() && mc.player != null && mc.level != null
                && kind.matches(mc.level.getBlockState(pos)) && mc.player.distanceToSqr(pos.getCenter()) <= 25
                && Scene.beginClientOnly(pos, kind);
    }

    public static void clear() {
        enabledForConnection = false;
        pendingPos = null;
        pendingKind = null;
    }

    private static boolean supported(StationKind kind) {
        return switch (kind) {
            case TABLE, FURNACE, SMOKER, BLAST_FURNACE, ENCHANTING, ANVIL, GRINDSTONE, SMITHING, STONECUTTER, LOOM, CARTOGRAPHY -> true;
            default -> false;
        };
    }

    private static boolean matches(StationKind kind, Object menu) {
        return switch (kind) {
            case TABLE -> menu instanceof CraftingMenu;
            case FURNACE -> menu instanceof FurnaceMenu;
            case SMOKER -> menu instanceof SmokerMenu;
            case BLAST_FURNACE -> menu instanceof BlastFurnaceMenu;
            case ENCHANTING -> menu instanceof EnchantmentMenu;
            case ANVIL -> menu instanceof AnvilMenu;
            case GRINDSTONE -> menu instanceof GrindstoneMenu;
            case SMITHING -> menu instanceof SmithingMenu;
            case STONECUTTER -> menu instanceof StonecutterMenu;
            case LOOM -> menu instanceof LoomMenu;
            case CARTOGRAPHY -> menu instanceof CartographyTableMenu;
            default -> false;
        };
    }
}
