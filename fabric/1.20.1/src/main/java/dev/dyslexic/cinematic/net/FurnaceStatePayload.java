package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Fire and cooking progress of the furnace the player crouches at (what the vanilla menu would show). */
public record FurnaceStatePayload(BlockPos pos, int litTime, int litDuration, int cookingProgress, int cookingTotalTime, String choice) implements FabricPacket {
    public static final PacketType<FurnaceStatePayload> TYPE = PacketType.create(Cinematic.id("furnace_state"), FurnaceStatePayload::read);

    @Override
    public PacketType<FurnaceStatePayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(litTime);
        buf.writeVarInt(litDuration);
        buf.writeVarInt(cookingProgress);
        buf.writeVarInt(cookingTotalTime);
        buf.writeUtf(choice);
    }

    public static FurnaceStatePayload read(FriendlyByteBuf buf) {
        return new FurnaceStatePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
    }
}
