package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** An enchantment just happened at a table: onlookers get the burst of light and runes. */
public record EnchantFxPayload(BlockPos pos, int entityId) implements FabricPacket {
    public static final PacketType<EnchantFxPayload> TYPE = PacketType.create(Cinematic.id("enchant_fx"), EnchantFxPayload::read);

    @Override
    public PacketType<EnchantFxPayload> getType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(entityId);
    }

    public static EnchantFxPayload read(FriendlyByteBuf buf) {
        return new EnchantFxPayload(buf.readBlockPos(), buf.readVarInt());
    }
}
