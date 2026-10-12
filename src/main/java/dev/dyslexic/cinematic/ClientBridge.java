package dev.dyslexic.cinematic;

import dev.dyslexic.cinematic.station.StationKind;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Hooks the client mod fills in, so common code never loads client classes on a dedicated server. */
public final class ClientBridge {
    private ClientBridge() {}

    /** The local player used a station; start its scene. */
    public static BiConsumer<BlockPos, StationKind> useStation = (pos, kind) -> {};
    /** Where on the block the last use touched (a chiseled bookshelf: which book). */
    public static Vec3 useAt = Vec3.ZERO;
}
