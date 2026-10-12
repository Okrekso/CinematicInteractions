package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.brewing.BrewingWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells clients what stands in a brewing stand, so its bottles can be drawn as the real potions. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends BaseContainerBlockEntity implements BrewingWatch.Fingerprinted {
    @Unique
    private int cinematic$fingerprint;

    protected BrewingStandBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int cinematic$fingerprint() {
        return cinematic$fingerprint;
    }

    @Override
    public void cinematic$setFingerprint(int fingerprint) {
        cinematic$fingerprint = fingerprint;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return BrewingWatch.updateTag((BrewingStandBlockEntity) (Object) this, registries);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void cinematic$watch(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand, CallbackInfo ci) {
        BrewingWatch.afterTick(level, pos, state, stand);
    }
}
