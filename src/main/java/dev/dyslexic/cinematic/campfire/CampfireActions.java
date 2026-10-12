package dev.dyslexic.cinematic.campfire;

import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions.Session;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Sitting by a campfire: food is put on a chosen spot of the fire, or taken off it. Putting on follows the
 * campfire's own rules (only what it can cook, the recipe's cooking time, one piece per spot). Taking off hands
 * the piece back as it is, raw or half done; left alone it pops off cooked as in vanilla. The item in the
 * player's hand is the cursor stack of their inventory menu, as at the other stations without a menu.
 */
public final class CampfireActions {
    private CampfireActions() {}

    public static final int SPOTS = 4;

    public static void handle(ServerPlayer player, ServerLevel level, Session session, StationActionPayload action) {
        int spot = action.index();
        if (action.action() != StationActionPayload.Action.CLICK || spot < 0 || spot >= SPOTS
                || !(level.getBlockEntity(session.pos()) instanceof CampfireBlockEntity fire)) {
            return;
        }
        AbstractContainerMenu menu = player.inventoryMenu;
        ItemStack carried = menu.getCarried();
        ItemStack there = fire.getItems().get(spot);
        if (!carried.isEmpty()) {
            Optional<RecipeHolder<CampfireCookingRecipe>> recipe = fire.getCookableRecipe(carried);
            if (!there.isEmpty() || recipe.isEmpty()) {
                return;
            }
            fire.cookingTime[spot] = recipe.get().value().getCookingTime();
            fire.cookingProgress[spot] = 0;
            fire.getItems().set(spot, carried.consumeAndReturn(1, player));
            level.gameEvent(GameEvent.BLOCK_CHANGE, session.pos(), GameEvent.Context.of(player, fire.getBlockState()));
            player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
        } else {
            if (there.isEmpty()) {
                return;
            }
            fire.getItems().set(spot, ItemStack.EMPTY);
            fire.cookingProgress[spot] = 0;
            menu.setCarried(there);
        }
        fire.setChanged();
        BlockState state = fire.getBlockState();
        level.sendBlockUpdated(session.pos(), state, state, 3);
        menu.broadcastChanges();
    }
}
