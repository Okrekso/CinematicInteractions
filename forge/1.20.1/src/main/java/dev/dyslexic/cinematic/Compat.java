package dev.dyslexic.cinematic;

import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small stand-ins for helpers Minecraft 1.20.1 does not have yet (items still carry plain NBT, not components). */
public final class Compat {
    private Compat() {}

    /** The item and its tag, as {@code ItemStack.hashItemAndComponents} would hash them in later versions (no count). */
    public static int hashItem(ItemStack stack) {
        return stack.isEmpty() ? 0 : 31 * Item.getId(stack.getItem()) + Objects.hashCode(stack.getTag());
    }
}
