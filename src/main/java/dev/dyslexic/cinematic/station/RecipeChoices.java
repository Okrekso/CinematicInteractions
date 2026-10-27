package dev.dyslexic.cinematic.station;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.capability.IBlockEntityRecipeData;
import dev.dyslexic.cinematic.station.Sessions.Session;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Recipes that compete for the same input (Polymorph). Polymorph keeps the choice, per player for the crafting table and
 * the smithing table, per block for a furnace, and its own windows and the vanilla blocks follow it; the scenes show the
 * same choice and change it here. Without Polymorph nothing of this runs: the first recipe wins, as in vanilla.
 * Both sides number the competing recipes the same way ({@link #matching}), so a choice travels as an index.
 */
public final class RecipeChoices {
    private RecipeChoices() {}

    private static final boolean POLYMORPH = net.neoforged.fml.ModList.get().isLoaded("polymorph");

    public static boolean available() {
        return POLYMORPH;
    }

    /** Every recipe of {@code type} the input makes something with, ordered by id (the order both sides count in). */
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> matching(Level level, RecipeType<T> type, I input) {
        List<RecipeHolder<T>> found = new ArrayList<>(level.getRecipeManager().getRecipesFor(type, input, level));
        found.removeIf(holder -> holder.value().assemble(input, level.registryAccess()).isEmpty());
        found.sort(Comparator.comparing(holder -> holder.id().toString()));
        return found;
    }

    /** The recipe the player chose last (Polymorph's own record), if any. */
    @Nullable
    public static ResourceLocation chosen(Player player) {
        return POLYMORPH ? Polymorph.chosen(player) : null;
    }

    /** The recipe a furnace was told to use for this input, or "" (nothing chosen, or no choice to make). */
    public static String furnaceChoice(AbstractFurnaceBlockEntity furnace, StationKind kind) {
        if (!POLYMORPH || furnace.getLevel() == null || furnace.getItem(0).isEmpty()) return "";
        var list = matching(furnace.getLevel(), cookingType(kind), new SingleRecipeInput(furnace.getItem(0)));
        if (list.size() < 2) return "";
        ResourceLocation id = Polymorph.chosen(furnace);
        return id != null && list.stream().anyMatch(holder -> holder.id().equals(id)) ? id.toString() : "";
    }

    public static RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> cookingType(StationKind kind) {
        return switch (kind) {
            case SMOKER -> RecipeType.SMOKING;
            case BLAST_FURNACE -> RecipeType.BLASTING;
            default -> RecipeType.SMELTING;
        };
    }

    /** CHOOSE: the {@code index}-th of the recipes competing for what lies in the station right now. */
    static void choose(ServerPlayer player, ServerLevel level, Session session, int index) {
        if (!POLYMORPH || index < 0) {
            return;
        }
        switch (session.kind()) {
            case TABLE -> {
                TableStore.Table table = TableStore.get(level).get(session.pos());
                if (table == null) return;
                CraftingInput input = CraftingInput.ofPositioned(3, 3, TableGrid.oriented(table.items, session.side())).input();
                var list = matching(level, RecipeType.CRAFTING, input);
                if (index < list.size()) Polymorph.choose(player, list.get(index));
            }
            case SMITHING -> {
                if (!(player.containerMenu instanceof SmithingMenu menu)) return;
                var input = new SmithingRecipeInput(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
                var list = matching(level, RecipeType.SMITHING, input);
                if (index >= list.size()) return;
                Polymorph.choose(player, list.get(index));
                // the result slot follows at once, as in Polymorph's own window
                menu.createResult();
                menu.broadcastChanges();
            }
            case FURNACE, SMOKER, BLAST_FURNACE -> {
                if (!(level.getBlockEntity(session.pos()) instanceof AbstractFurnaceBlockEntity furnace) || furnace.getItem(0).isEmpty()) return;
                var list = matching(level, cookingType(session.kind()), new SingleRecipeInput(furnace.getItem(0)));
                if (index < list.size()) Polymorph.choose(furnace, list.get(index));
                furnace.setChanged();
                dev.dyslexic.cinematic.furnace.FurnaceActions.sendState(player, session);
            }
            default -> {}
        }
    }

    /** Polymorph's API, loaded only with Polymorph present. */
    private static final class Polymorph {
        @Nullable
        static ResourceLocation chosen(Player player) {
            RecipeHolder<?> holder = PolymorphApi.getInstance().getPlayerRecipeData(player).getSelectedRecipe();
            return holder == null ? null : holder.id();
        }

        @Nullable
        static ResourceLocation chosen(BlockEntity blockEntity) {
            IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(blockEntity);
            RecipeHolder<?> holder = data == null ? null : data.getSelectedRecipe();
            return holder == null ? null : holder.id();
        }

        static void choose(Player player, RecipeHolder<?> holder) {
            PolymorphApi.getInstance().getPlayerRecipeData(player).selectRecipe(holder);
        }

        static void choose(BlockEntity blockEntity, RecipeHolder<?> holder) {
            IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(blockEntity);
            if (data != null) data.selectRecipe(holder);
        }
    }
}
