package dev.dyslexic.cinematic.station;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableInput;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

/** The 1.20.1 Polymorph API keeps a player's selected recipe as a Recipe, not a RecipeHolder. */
public final class RecipeChoices {
    private RecipeChoices() {}

    private static final boolean POLYMORPH = ModList.get().isLoaded("polymorph");

    public static boolean available() { return POLYMORPH; }

    public static <C extends Container, T extends Recipe<C>> List<T> matching(Level level, RecipeType<T> type, C input) {
        List<T> recipes = new ArrayList<>(level.getRecipeManager().getRecipesFor(type, input, level));
        recipes.removeIf(recipe -> recipe.assemble(input, level.registryAccess()).isEmpty());
        recipes.sort(Comparator.comparing(recipe -> recipe.getId().toString()));
        return recipes;
    }

    @Nullable
    public static ResourceLocation chosen(Player player) {
        return POLYMORPH ? Polymorph.chosen(player) : null;
    }

    public static RecipeType<? extends AbstractCookingRecipe> cookingType(StationKind kind) {
        return switch (kind) {
            case SMOKER -> RecipeType.SMOKING;
            case BLAST_FURNACE -> RecipeType.BLASTING;
            default -> RecipeType.SMELTING;
        };
    }

    public static String furnaceChoice(AbstractFurnaceBlockEntity furnace, StationKind kind) {
        if (!POLYMORPH || furnace.getLevel() == null || furnace.getItem(0).isEmpty()) return "";
        var input = new SimpleContainer(furnace.getItem(0));
        var recipes = matching(furnace.getLevel(), cookingType(kind), input);
        if (recipes.size() < 2) return "";
        ResourceLocation selected = Polymorph.chosen(furnace);
        return selected != null && recipes.stream().anyMatch(recipe -> recipe.getId().equals(selected)) ? selected.toString() : "";
    }

    static void choose(ServerPlayer player, ServerLevel level, Sessions.Session session, int index) {
        if (!POLYMORPH || index < 0) return;
        if (session.kind().isFurnace()) {
            if (!(level.getBlockEntity(session.pos()) instanceof AbstractFurnaceBlockEntity furnace) || furnace.getItem(0).isEmpty()) return;
            var recipes = matching(level, cookingType(session.kind()), new SimpleContainer(furnace.getItem(0)));
            if (index < recipes.size()) {
                Polymorph.choose(furnace, recipes.get(index));
                furnace.setChanged();
                dev.dyslexic.cinematic.furnace.FurnaceActions.sendState(player, session);
            }
            return;
        }
        if (session.kind() == StationKind.SMITHING) {
            if (!(player.containerMenu instanceof SmithingMenu menu)) return;
            var input = new SimpleContainer(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
            var recipes = matching(level, RecipeType.SMITHING, input);
            if (index < recipes.size()) {
                Polymorph.choose(player, recipes.get(index));
                menu.slotsChanged(menu.getSlot(0).container);
                menu.broadcastChanges();
            }
            return;
        }
        if (session.kind() != StationKind.TABLE) return;
        TableStore.Table table = TableStore.get(level).get(session.pos());
        if (table == null) return;
        TableInput input = new TableInput(TableGrid.oriented(table.items, session.side()));
        List<net.minecraft.world.item.crafting.CraftingRecipe> recipes = matching(level, RecipeType.CRAFTING, input);
        if (index < recipes.size()) Polymorph.choose(player, recipes.get(index));
    }

    /** Isolated so a client without Polymorph never resolves its classes. */
    private static final class Polymorph {
        @Nullable static ResourceLocation chosen(Player player) {
            return PolymorphApi.common().getRecipeData(player)
                    .flatMap(data -> data.getSelectedRecipe()).map(Recipe::getId).orElse(null);
        }

        static void choose(Player player, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(player).ifPresent(data -> data.selectRecipe(recipe));
        }

        @Nullable static ResourceLocation chosen(BlockEntity blockEntity) {
            return PolymorphApi.common().getRecipeData(blockEntity)
                    .flatMap(data -> data.getSelectedRecipe()).map(Recipe::getId).orElse(null);
        }

        static void choose(BlockEntity blockEntity, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(blockEntity).ifPresent(data -> data.selectRecipe(recipe));
        }
    }
}
