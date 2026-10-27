//? if <1.21 {
/*package dev.dyslexic.cinematic;

import net.minecraft.resources.ResourceLocation;

/^*
 * A recipe with its id. In 1.20.1 a recipe carries its own id ({@code getId()}); later versions hand recipes out in a
 * holder of the two, and the scenes keep that shape so they read the same in every version of the mod.
 ^/
public record RecipeRef<T>(ResourceLocation id, T value) {
    public static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeRef<T> of(T recipe) {
        return new RecipeRef<>(recipe.getId(), recipe);
    }
}
*///?}
