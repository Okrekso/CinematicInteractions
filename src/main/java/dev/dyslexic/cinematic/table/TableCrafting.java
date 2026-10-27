package dev.dyslexic.cinematic.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.minecraft.world.SimpleContainer;
//?} else if forge {
/*import net.minecraft.world.SimpleContainer;
*///?}
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
//? if >=1.21 {
import net.minecraft.world.item.crafting.CraftingInput;
//?}
import net.minecraft.world.item.crafting.CraftingRecipe;
//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?} else if <1.21 {
/*import net.minecraft.world.item.crafting.Recipe;
*///?}
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
//? if neoforge {
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
//?} else if forge {
/*import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
*///?}

/** Reads the table top as a 3x3 crafting grid from the player's side and performs crafts like the vanilla result slot. */
public final class TableCrafting {
    private TableCrafting() {}

    //? if >=1.21 {
    public record Match(RecipeHolder<CraftingRecipe> recipe, CraftingInput.Positioned input, ItemStack result) {}
    //?} else if <1.21 {
    /*public record Match(CraftingRecipe recipe, TableInput input, ItemStack result) {}
    *///?}

    /** The recipe the table currently spells for a player on {@code side}; the result is a fresh copy. */
    public static Optional<Match> match(Level level, List<ItemStack> world, Direction side) {
        return match(level, world, side, null);
    }

    //? if >=1.21 {
    /**
     * The same, with {@code chosen} winning when it is one of several recipes the cells spell (Polymorph's choice): the
     * scene shows and the server crafts the same one.
     */
    public static Optional<Match> match(Level level, List<ItemStack> world, Direction side, @javax.annotation.Nullable net.minecraft.resources.ResourceLocation chosen) {
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, TableGrid.oriented(world, side));
        CraftingInput input = positioned.input();
    //?} else if <1.21 {
    /*public static Optional<Match> match(Level level, List<ItemStack> world, Direction side,
            @javax.annotation.Nullable net.minecraft.resources.ResourceLocation chosen) {
        TableInput input = new TableInput(TableGrid.oriented(world, side));
    *///?}
        if (input.isEmpty()) {
            return Optional.empty();
        }
        //? if >=1.21 {
        Optional<RecipeHolder<CraftingRecipe>> found = Optional.empty();
        //?} else if <1.21 {
        /*Optional<CraftingRecipe> found = Optional.empty();
        *///?}
        if (chosen != null) {
            //? if >=1.21 {
            found = dev.dyslexic.cinematic.station.RecipeChoices.matching(level, RecipeType.CRAFTING, input).stream()
                    .filter(holder -> holder.id().equals(chosen)).findFirst();
            //?} else if <1.21 {
            /*found = dev.dyslexic.cinematic.station.RecipeChoices.matching(level, RecipeType.CRAFTING, input)
                    .stream().filter(recipe -> recipe.getId().equals(chosen)).findFirst();
            *///?}
        }
        if (found.isEmpty()) found = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        //? if >=1.21 {
        ItemStack result = found.get().value().assemble(input, level.registryAccess());
        //?} else if <1.21 {
        /*ItemStack result = found.get().assemble(input, level.registryAccess());
        *///?}
        if (result.isEmpty() || !result.isItemEnabled(level.enabledFeatures())) {
            return Optional.empty();
        }
        //? if >=1.21 {
        return Optional.of(new Match(found.get(), positioned, result));
        //?} else if <1.21 {
        /*return Optional.of(new Match(found.get(), input, result));
        *///?}
    }

    /**
     * Crafts once. The result goes into the cursor stack, or into the inventory when {@code toInventory}.
     * Returns the consumed ingredients (for particles), or null when nothing was crafted.
     */
    public static List<ItemStack> craftOnce(ServerPlayer player, ServerLevel level, TableStore.Table table, Direction side,
            boolean toInventory) {
        //? if !forge {
        Optional<Match> found = match(level, table.items, side, dev.dyslexic.cinematic.station.RecipeChoices.chosen(player));
        //?} else if forge {
        /*Optional<Match> found = match(level, table.items, side,
                dev.dyslexic.cinematic.station.RecipeChoices.chosen(player));
        *///?}
        if (found.isEmpty()) {
            return null;
        }
        Match match = found.get();
        //? if >=1.21 {
        RecipeHolder<CraftingRecipe> recipe = match.recipe();
        if (!recipe.value().isSpecial() && level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
        //?} else if <1.21 {
        /*CraftingRecipe recipe = match.recipe();
        if (!recipe.isSpecial() && level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
        *///?}
                && !player.getRecipeBook().contains(recipe)) {
            return null;
        }
        ItemStack result = match.result();
        ItemStack carried = player.containerMenu.getCarried();
        if (toInventory) {
            if (!canFit(player.getInventory(), result)) {
                return null;
            }
        } else if (!carried.isEmpty() && !(ItemStack.isSameItemSameComponents(carried, result)
                && carried.getCount() + result.getCount() <= carried.getMaxStackSize())) {
            return null;
        }

        List<ItemStack> grid = TableGrid.oriented(table.items, side);
        List<ItemStack> snapshot = new ArrayList<>(grid.size());
        for (ItemStack stack : grid) {
            snapshot.add(stack.copy());
        }
        result.onCraftedBy(level, player, result.getCount());
        // (NeoForge fires its ItemCraftedEvent here; Fabric API has no such event, vanilla's own hooks above remain)
        //? if neoforge {
        EventHooks.firePlayerCraftingEvent(player, result, new SimpleContainer(snapshot.toArray(ItemStack[]::new)));
        //?} else if forge {
        /*ForgeEventFactory.firePlayerCraftingEvent(player, result, new SimpleContainer(snapshot.toArray(ItemStack[]::new)));
        *///?}
        player.triggerRecipeCrafted(recipe, snapshot);
        //? if >=1.21 {
        if (!recipe.value().isSpecial()) {
            player.awardRecipes(Collections.singleton(recipe));
        //?} else if <1.21 {
        /*if (!recipe.isSpecial()) {
            player.awardRecipes(Collections.<Recipe<?>>singleton(recipe));
        *///?}
        }

        //? if neoforge {
        CraftingInput input = match.input().input();
        CommonHooks.setCraftingPlayer(player);
        //?} else if fabric && >=1.21 {
        /*CraftingInput input = match.input().input();
        *///?} else if fabric && <1.21 {
        /*TableInput input = match.input();
        *///?} else if forge {
        /*TableInput input = match.input();
        ForgeHooks.setCraftingPlayer(player);
        *///?}
        NonNullList<ItemStack> remaining = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, level);
        //? if neoforge {
        CommonHooks.setCraftingPlayer(null);
        //?} else if forge {
        /*ForgeHooks.setCraftingPlayer(null);
        *///?}

        List<ItemStack> consumed = new ArrayList<>();
        //? if >=1.21 {
        for (int row = 0; row < input.height(); row++) {
            for (int col = 0; col < input.width(); col++) {
                int world = TableGrid.worldIndex(side, row + match.input().top(), col + match.input().left());
        //?} else if <1.21 {
        /*for (int row = 0; row < input.getHeight(); row++) {
            for (int col = 0; col < input.getWidth(); col++) {
                int world = TableGrid.worldIndex(side, row, col);
        *///?}
                ItemStack stack = table.items.get(world);
                //? if >=1.21 {
                ItemStack leftover = remaining.get(col + row * input.width());
                //?} else if <1.21 {
                /*ItemStack leftover = remaining.get(col + row * input.getWidth());
                *///?}
                if (!stack.isEmpty()) {
                    consumed.add(stack.copyWithCount(1));
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        table.items.set(world, ItemStack.EMPTY);
                    }
                    stack = table.items.get(world);
                }
                if (!leftover.isEmpty()) {
                    if (stack.isEmpty()) {
                        table.items.set(world, leftover);
                    } else if (ItemStack.isSameItemSameComponents(stack, leftover)) {
                        leftover.grow(stack.getCount());
                        table.items.set(world, leftover);
                    } else if (!player.getInventory().add(leftover)) {
                        player.drop(leftover, false);
                    }
                }
            }
        }

        if (toInventory) {
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        } else if (carried.isEmpty()) {
            player.containerMenu.setCarried(result);
        } else {
            carried.grow(result.getCount());
        }
        return consumed;
    }

    private static boolean canFit(Inventory inventory, ItemStack stack) {
        return inventory.getSlotWithRemainingSpace(stack) != -1 || inventory.getFreeSlot() != -1;
    }
}
