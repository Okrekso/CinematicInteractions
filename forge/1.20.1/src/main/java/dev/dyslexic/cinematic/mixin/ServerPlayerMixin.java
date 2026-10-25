package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.table.TableActions;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /** Moves items from the physical table into any crafting window opened at that table. */
    @Inject(method = "openMenu", at = @At("RETURN"))
    private void cinematic$openedMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> result) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (result.getReturnValue().isPresent() && player.containerMenu instanceof CraftingMenu menu) {
            TableActions.intoWindow(player, menu);
        }
    }
}
