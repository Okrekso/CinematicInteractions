//? if <1.21 {
/*package dev.dyslexic.cinematic.table;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/^*
 * The table top read as a 3x3 crafting grid, in recipe order (row 0 the far edge, from the crafting player's side).
 * Minecraft 1.20.1 matches crafting recipes against a container rather than a positioned input, so this is a plain one
 * over the same stack instances: matching and remainders work on the whole grid, index = col + row * 3.
 ^/
public final class TableInput implements CraftingContainer {
    private final NonNullList<ItemStack> items;

    public TableInput(List<ItemStack> recipeOrder) {
        items = NonNullList.withSize(TableGrid.CELLS, ItemStack.EMPTY);
        for (int i = 0; i < TableGrid.CELLS && i < recipeOrder.size(); i++) {
            items.set(i, recipeOrder.get(i));
        }
    }

    @Override public int getWidth() { return 3; }
    @Override public int getHeight() { return 3; }
    @Override public List<ItemStack> getItems() { return List.copyOf(items); }
    @Override public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { items.clear(); }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack stack : items) {
            contents.accountSimpleStack(stack);
        }
    }
}
*///?}
