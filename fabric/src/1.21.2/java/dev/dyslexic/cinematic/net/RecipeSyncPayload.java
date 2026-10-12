package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.*;

/** Vanilla 1.21.2 syncs displays, while the table reference also needs the full ingredient pattern. */
public record RecipeSyncPayload(boolean reset, List<RecipeHolder<?>> recipes) implements CustomPacketPayload {
    public static final Type<RecipeSyncPayload> TYPE = new Type<>(Cinematic.id("crafting_reference"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RecipeSyncPayload::reset,
            RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeSyncPayload::recipes, RecipeSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerPlayer player) {
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, TYPE)) return;
        var recipes = player.serverLevel().recipeAccess().getRecipes().stream()
                .filter(h -> h.value() instanceof CraftingRecipe).toList();
        PacketDistributor.sendToPlayer(player, new RecipeSyncPayload(true, List.of()));
        // Bound each packet even for large modpacks; a fresh batch replaces the cache after /reload.
        for (int first = 0; first < recipes.size(); first += 32)
            PacketDistributor.sendToPlayer(player, new RecipeSyncPayload(false, recipes.subList(first, Math.min(first + 32, recipes.size()))));
    }
}
