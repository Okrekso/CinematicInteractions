package dev.dyslexic.cinematic.client;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.ForgeHooks;

/**
 * Without the mod on the server a shift-click can only be the vanilla one, and where a vanilla menu finds no place for
 * the stack in the block it moves it between hotbar and pack instead. In the scene that would only shuffle the drawer, so
 * the click is sent only when the menu's own quick move would put the stack into the block: these are its conditions,
 * read from each menu's quickMoveStack.
 */
final class QuickMoves {
    private QuickMoves() {}

    static boolean entersStation(AbstractContainerMenu menu, int index, Level level) {
        if (index < 0 || index >= menu.slots.size()) return false;
        ItemStack stack = menu.getSlot(index).getItem();
        if (stack.isEmpty()) return false;
        if (menu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu) {
            RecipeType<? extends AbstractCookingRecipe> type = menu instanceof SmokerMenu ? RecipeType.SMOKING
                    : menu instanceof BlastFurnaceMenu ? RecipeType.BLASTING : RecipeType.SMELTING;
            if (smelts(menu, stack, level)) return fits(menu, 0, stack);
            return ForgeHooks.getBurnTime(stack, type) > 0 && fits(menu, 1, stack);
        }
        if (menu instanceof StonecutterMenu) {
            return level.getRecipeManager().getRecipeFor(RecipeType.STONECUTTING, new SimpleContainer(stack), level).isPresent()
                    && fits(menu, 0, stack);
        }
        if (menu instanceof LoomMenu) {
            return stack.getItem() instanceof BannerItem ? fits(menu, 0, stack)
                    : stack.getItem() instanceof DyeItem ? fits(menu, 1, stack)
                    : stack.getItem() instanceof BannerPatternItem && fits(menu, 2, stack);
        }
        if (menu instanceof CartographyTableMenu) {
            return stack.is(Items.FILLED_MAP) ? fits(menu, 0, stack)
                    : (stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE)) && fits(menu, 1, stack);
        }
        if (menu instanceof GrindstoneMenu) {
            return !(menu.getSlot(0).hasItem() && menu.getSlot(1).hasItem()) && (fits(menu, 0, stack) || fits(menu, 1, stack));
        }
        if (menu instanceof EnchantmentMenu) {
            return stack.is(Items.LAPIS_LAZULI) ? fits(menu, 1, stack) : !menu.getSlot(0).hasItem() && menu.getSlot(0).mayPlace(stack);
        }
        // the anvil and the smithing table take whatever their input slots accept
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).container != net.minecraft.client.Minecraft.getInstance().player.getInventory() && fits(menu, i, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Sends the vanilla clicks for a shift-click without the mod on the server, or nothing. */
    static void sendVanilla(net.minecraft.client.Minecraft mc, AbstractContainerMenu menu, int index) {
        if (index < 0 || index >= menu.slots.size() || mc.gameMode == null || mc.player == null) return;
        // (Forge 1.20.1 knows fuels on the client: they are code, not a data map the server would have to send, so the
        // furnace's own quick move is predicted like the other menus' and needs no pick-up-and-put-back as on NeoForge)
        if (entersStation(menu, index, mc.level)) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
        }
    }

    private static boolean smelts(AbstractContainerMenu menu, ItemStack stack, Level level) {
        RecipeType<? extends AbstractCookingRecipe> type = menu instanceof SmokerMenu ? RecipeType.SMOKING
                : menu instanceof BlastFurnaceMenu ? RecipeType.BLASTING : RecipeType.SMELTING;
        @SuppressWarnings("unchecked")
        RecipeType<AbstractCookingRecipe> cooking = (RecipeType<AbstractCookingRecipe>) type;
        return level.getRecipeManager().getRecipeFor(cooking, (Container) new SimpleContainer(stack), level).isPresent();
    }

    private static boolean fits(AbstractContainerMenu menu, int index, ItemStack stack) {
        Slot slot = menu.getSlot(index);
        if (!slot.mayPlace(stack)) return false;
        ItemStack there = slot.getItem();
        return there.isEmpty() || ItemStack.isSameItemSameTags(there, stack) && there.getCount() < slot.getMaxStackSize(stack);
    }
}
