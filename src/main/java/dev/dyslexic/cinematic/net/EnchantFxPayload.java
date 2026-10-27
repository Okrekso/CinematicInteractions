package dev.dyslexic.cinematic.net;

//? if >=1.21 {
import dev.dyslexic.cinematic.Cinematic;
//?} else if fabric && <1.21 {
/*import dev.dyslexic.cinematic.Cinematic;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** An enchantment just happened at a table: onlookers get the burst of light and runes. */
//? if >=1.21 {
public record EnchantFxPayload(BlockPos pos, int entityId) implements CustomPacketPayload {
    public static final Type<EnchantFxPayload> TYPE = new Type<>(Cinematic.id("enchant_fx"));
    public static final StreamCodec<FriendlyByteBuf, EnchantFxPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EnchantFxPayload::pos,
            ByteBufCodecs.VAR_INT, EnchantFxPayload::entityId,
            EnchantFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
//?} else if fabric && <1.21 {
/*public record EnchantFxPayload(BlockPos pos, int entityId) implements FabricPacket {
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
*///?} else if forge {
/*public record EnchantFxPayload(BlockPos pos, int entityId) implements Payload {
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(entityId);
    }

    public static EnchantFxPayload read(FriendlyByteBuf buf) {
        return new EnchantFxPayload(buf.readBlockPos(), buf.readVarInt());
*///?}
    }
}
