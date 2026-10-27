package dev.dyslexic.cinematic.station;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
//? if >=1.21 {
import com.illusivesoulworks.polymorph.api.common.capability.IBlockEntityRecipeData;
import dev.dyslexic.cinematic.station.Sessions.Session;
//?}
import dev.dyslexic.cinematic.table.TableGrid;
//? if <1.21 {
/*import dev.dyslexic.cinematic.table.TableInput;
*///?}
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
//? if fabric && >=1.21 {
/*import net.fabricmc.loader.api.FabricLoader;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if <1.21 {
/*import net.minecraft.world.Container;
*///?}
import net.minecraft.world.entity.player.Player;
//? if >=1.21 {
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.CraftingInput;
//?}
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
//?}
import net.minecraft.world.item.crafting.RecipeType;
//? if >=1.21 {
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
//?} else if <1.21 {
/*import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SmithingMenu;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
/*import net.minecraftforge.fml.ModList;
*///?}

/**
 * Recipes that compete for the same input (Polymorph). Polymorph keeps the choice, per player for the crafting table and
 * the smithing table, per block for a furnace, and its own windows and the vanilla blocks follow it; the scenes show the
 * same choice and change it here. Without Polymorph nothing of this runs: the first recipe wins, as in vanilla.
 * Both sides number the competing recipes the same way ({@link #matching}), so a choice travels as an index.
 */
public final class RecipeChoices {
    private RecipeChoices() {}

    //? if neoforge {
    private static final boolean POLYMORPH = net.neoforged.fml.ModList.get().isLoaded("polymorph");
    //?} else if fabric && >=1.21 {
    /*private static final boolean POLYMORPH = FabricLoader.getInstance().isModLoaded("polymorph");
    *///?} else if fabric && <1.21 {
    /*private static final boolean POLYMORPH = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("polymorph");
    *///?} else if forge {
    /*private static final boolean POLYMORPH = ModList.get().isLoaded("polymorph");
    *///?}

    //? if >=1.21 {
    public static boolean available() {
        return POLYMORPH;
    }
    //?} else if <1.21 {
    /*public static boolean available() { return POLYMORPH; }
    *///?}

    //? if >=1.21 {
    /** Every recipe of {@code type} the input makes something with, ordered by id (the order both sides count in). */
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> matching(Level level, RecipeType<T> type, I input) {
        List<RecipeHolder<T>> found = new ArrayList<>(level.getRecipeManager().getRecipesFor(type, input, level));
        found.removeIf(holder -> holder.value().assemble(input, level.registryAccess()).isEmpty());
        found.sort(Comparator.comparing(holder -> holder.id().toString()));
        return found;
    //?} else if <1.21 {
    /*public static <C extends Container, T extends Recipe<C>> List<T> matching(Level level, RecipeType<T> type, C input) {
        List<T> recipes = new ArrayList<>(level.getRecipeManager().getRecipesFor(type, input, level));
        recipes.removeIf(recipe -> recipe.assemble(input, level.registryAccess()).isEmpty());
        recipes.sort(Comparator.comparing(recipe -> recipe.getId().toString()));
        return recipes;
    *///?}
    }

    /** The recipe the player chose last (Polymorph's own record), if any. */
    @Nullable
    public static ResourceLocation chosen(Player player) {
        return POLYMORPH ? Polymorph.chosen(player) : null;
    }

    //? if >=1.21 {
    /** The recipe a furnace was told to use for this input, or "" (nothing chosen, or no choice to make). */
    public static String furnaceChoice(AbstractFurnaceBlockEntity furnace, StationKind kind) {
        if (!POLYMORPH || furnace.getLevel() == null || furnace.getItem(0).isEmpty()) return "";
        var list = matching(furnace.getLevel(), cookingType(kind), new SingleRecipeInput(furnace.getItem(0)));
        if (list.size() < 2) return "";
        ResourceLocation id = Polymorph.chosen(furnace);
        return id != null && list.stream().anyMatch(holder -> holder.id().equals(id)) ? id.toString() : "";
    }

    public static RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> cookingType(StationKind kind) {
    //?} else if <1.21 {
    /*public static RecipeType<? extends AbstractCookingRecipe> cookingType(StationKind kind) {
    *///?}
        return switch (kind) {
            case SMOKER -> RecipeType.SMOKING;
            case BLAST_FURNACE -> RecipeType.BLASTING;
            default -> RecipeType.SMELTING;
        };
    }

    //? if >=1.21 {
    /** CHOOSE: the {@code index}-th of the recipes competing for what lies in the station right now. */
    static void choose(ServerPlayer player, ServerLevel level, Session session, int index) {
        if (!POLYMORPH || index < 0) {
    //?} else if <1.21 {
    /*public static String furnaceChoice(AbstractFurnaceBlockEntity furnace, StationKind kind) {
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
    *///?}
            return;
        }
        //? if >=1.21 {
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
        //?} else if <1.21 {
        /*if (session.kind() == StationKind.SMITHING) {
            if (!(player.containerMenu instanceof SmithingMenu menu)) return;
            var input = new SimpleContainer(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
            var recipes = matching(level, RecipeType.SMITHING, input);
            if (index < recipes.size()) {
                Polymorph.choose(player, recipes.get(index));
                menu.slotsChanged(menu.getSlot(0).container);
        *///?}
                menu.broadcastChanges();
            }
            //? if >=1.21 {
            case FURNACE, SMOKER, BLAST_FURNACE -> {
                if (!(level.getBlockEntity(session.pos()) instanceof AbstractFurnaceBlockEntity furnace) || furnace.getItem(0).isEmpty()) return;
                var list = matching(level, cookingType(session.kind()), new SingleRecipeInput(furnace.getItem(0)));
                if (index < list.size()) Polymorph.choose(furnace, list.get(index));
                furnace.setChanged();
                dev.dyslexic.cinematic.furnace.FurnaceActions.sendState(player, session);
            }
            default -> {}
            //?} else if <1.21 {
            /*return;
            *///?}
        }
        //? if <1.21 {
        /*if (session.kind() != StationKind.TABLE) return;
        TableStore.Table table = TableStore.get(level).get(session.pos());
        if (table == null) return;
        TableInput input = new TableInput(TableGrid.oriented(table.items, session.side()));
        List<net.minecraft.world.item.crafting.CraftingRecipe> recipes = matching(level, RecipeType.CRAFTING, input);
        if (index < recipes.size()) Polymorph.choose(player, recipes.get(index));
        *///?}
    }

    /** Polymorph's API, loaded only with Polymorph present. */
    private static final class Polymorph {
        //? if >=1.21 {
        @Nullable
        static ResourceLocation chosen(Player player) {
            RecipeHolder<?> holder = PolymorphApi.getInstance().getPlayerRecipeData(player).getSelectedRecipe();
            return holder == null ? null : holder.id();
        //?} else if <1.21 {
        /*@Nullable static ResourceLocation chosen(Player player) {
            return PolymorphApi.common().getRecipeData(player)
                    .flatMap(data -> data.getSelectedRecipe()).map(Recipe::getId).orElse(null);
        *///?}
        }

        //? if >=1.21 {
        @Nullable
        static ResourceLocation chosen(BlockEntity blockEntity) {
            IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(blockEntity);
            RecipeHolder<?> holder = data == null ? null : data.getSelectedRecipe();
            return holder == null ? null : holder.id();
        //?} else if <1.21 {
        /*static void choose(Player player, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(player).ifPresent(data -> data.selectRecipe(recipe));
        *///?}
        }

        //? if >=1.21 {
        static void choose(Player player, RecipeHolder<?> holder) {
            PolymorphApi.getInstance().getPlayerRecipeData(player).selectRecipe(holder);
        //?} else if <1.21 {
        /*@Nullable static ResourceLocation chosen(BlockEntity blockEntity) {
            return PolymorphApi.common().getRecipeData(blockEntity)
                    .flatMap(data -> data.getSelectedRecipe()).map(Recipe::getId).orElse(null);
        *///?}
        }

        //? if >=1.21 {
        static void choose(BlockEntity blockEntity, RecipeHolder<?> holder) {
            IBlockEntityRecipeData data = PolymorphApi.getInstance().getBlockEntityRecipeData(blockEntity);
            if (data != null) data.selectRecipe(holder);
        //?} else if <1.21 {
        /*static void choose(BlockEntity blockEntity, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(blockEntity).ifPresent(data -> data.selectRecipe(recipe));
        *///?}
        }
    }
}
