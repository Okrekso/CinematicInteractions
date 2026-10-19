package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** Local recipe reference: never places items or asks the server to unlock/craft a recipe. */
final class TableRecipeGuide {
    static final class Entry {
        @Nullable final RecipeHolder<CraftingRecipe> holder;
        final ItemStack result;
        final List<Ingredient> ingredients;
        final List<Ingredient> pattern;
        final int width, height;
        final String idText;
        final String search;
        boolean craftable;

        Entry(RecipeHolder<CraftingRecipe> holder, ItemStack result) {
            this.holder = holder;
            this.result = result;
            ingredients = holder.value().placementInfo().ingredients();
            if (holder.value() instanceof ShapedRecipe shaped) {
                width = shaped.getWidth();
                height = shaped.getHeight();
                pattern = shaped.getIngredients().stream().map(optional -> optional.orElse(null)).toList();
            } else {
                width = height = 0;
                pattern = List.of();
            }
            idText = holder.id().toString();
            search = (result.getHoverName().getString() + " " + idText).toLowerCase(Locale.ROOT);
        }

        Entry(RecipeDisplayEntry vanilla, ItemStack result, net.minecraft.util.context.ContextMap context) {
            holder = null;
            this.result = result;
            if (vanilla.display() instanceof ShapedCraftingRecipeDisplay shaped) {
                width = shaped.width();
                height = shaped.height();
                pattern = shaped.ingredients().stream().map(slot -> ingredient(slot, context)).toList();
                ingredients = pattern.stream().filter(i -> i != null).toList();
            } else if (vanilla.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
                width = height = 0;
                pattern = List.of();
                ingredients = shapeless.ingredients().stream().map(slot -> ingredient(slot, context))
                        .filter(i -> i != null).toList();
            } else throw new IllegalArgumentException("Not a crafting display");
            idText = "#" + vanilla.id().index();
            search = (result.getHoverName().getString() + " " + idText).toLowerCase(Locale.ROOT);
        }
    }

    @Nullable private static Ingredient ingredient(SlotDisplay slot, net.minecraft.util.context.ContextMap context) {
        var items = slot.resolveForStacks(context).stream().filter(stack -> !stack.isEmpty()).map(ItemStack::getItem).distinct().toList();
        return items.isEmpty() ? null : Ingredient.of(items.stream());
    }

    final List<Entry> entries = new ArrayList<>();
    @Nullable Entry selected;
    final Ingredient[] grid = new Ingredient[9];
    final ItemStack[] display = new ItemStack[9];
    final boolean[] placed = new boolean[9];
    final boolean[] missing = new boolean[9];
    int missingCount;
    int revision;
    private final TableStation station;
    private List<ItemStack> snapshot = List.of();
    private int indexed = -1;
    static boolean empty(Ingredient ingredient) { return ingredient == null; }

    TableRecipeGuide(TableStation station) {
        this.station = station;
        Arrays.fill(grid, null);
        Arrays.fill(display, ItemStack.EMPTY);
    }

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<RecipeDisplayEntry> vanilla = Scene.clientOnly() ? mc.player.getRecipeBook().getCollections().stream()
                .flatMap(collection -> collection.getRecipes().stream()).toList() : List.of();
        int source = Scene.clientOnly() ? vanilla.stream().mapToInt(entry -> entry.id().index()).reduce(1, (hash, id) -> 31 * hash + id)
                : ClientRecipes.revision;
        boolean reindexed = indexed != source;
        if (reindexed) {
            entries.clear();
            var context = SlotDisplayContext.fromLevel(mc.level);
            if (Scene.clientOnly()) {
                for (RecipeDisplayEntry display : vanilla) {
                    if (!(display.display() instanceof ShapedCraftingRecipeDisplay shaped
                            && shaped.width() <= 3 && shaped.height() <= 3)
                            && !(display.display() instanceof ShapelessCraftingRecipeDisplay)) continue;
                    ItemStack result = display.display().result().resolveForFirstStack(context);
                    if (!result.isEmpty()) {
                        Entry entry = new Entry(display, result.copy(), context);
                        if (!entry.ingredients.isEmpty()) entries.add(entry);
                    }
                }
            } else {
                for (var holder : ClientRecipes.crafting()) {
                    var recipe = holder.value();
                    // Dynamic special recipes have no fixed ingredient diagram; their vanilla GUI remains available.
                    if (recipe.isSpecial() || !(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)
                            || recipe instanceof ShapedRecipe shaped && (shaped.getWidth() > 3 || shaped.getHeight() > 3) || recipe.placementInfo().isImpossibleToPlace()) continue;
                    ItemStack result = recipe.display().getFirst().result().resolveForFirstStack(context);
                    if (!result.isEmpty()) entries.add(new Entry(holder, result.copy()));
                }
            }
            entries.sort(Comparator.comparing((Entry e) -> e.result.getHoverName().getString())
                    .thenComparing(e -> e.idText));
            selected = null;
            indexed = source;
        }
        List<ItemStack> next = new ArrayList<>();
        for (int i = 0; i < 36; i++) next.add(mc.player.getInventory().getItem(i).copy());
        next.add(mc.player.containerMenu.getCarried().copy());
        ClientTables.Entry table = ClientTables.get(station.pos);
        for (int i = 0; i < 9; i++) next.add(table == null ? ItemStack.EMPTY : table.items[i].copy());
        boolean changed = reindexed || next.size() != snapshot.size();
        for (int i = 0; !changed && i < next.size(); i++) changed = !ItemStack.matches(next.get(i), snapshot.get(i));
        if (!changed) return;
        snapshot = next;
        List<ItemStack> pool = units(next);
        for (Entry entry : entries) {
            int[] assignment = match(entry.ingredients, pool);
            entry.craftable = Arrays.stream(assignment).allMatch(i -> i >= 0);
        }
        refresh();
        revision++;
    }

    void select(@Nullable Entry entry) {
        selected = entry;
        Arrays.fill(grid, null);
        refresh();
        revision++;
    }

    private ItemStack actual(int logical) {
        ClientTables.Entry table = ClientTables.get(station.pos);
        return table == null ? ItemStack.EMPTY : table.items[TableGrid.worldIndex(station.side, logical / 3, logical % 3)];
    }

    private void refresh() {
        Arrays.fill(display, ItemStack.EMPTY);
        Arrays.fill(placed, false);
        Arrays.fill(missing, false);
        missingCount = 0;
        if (selected == null) return;
        layout();
        List<ItemStack> resources = new ArrayList<>();
        for (int i = 0; i < Math.min(37, snapshot.size()); i++) resources.add(snapshot.get(i).copy());
        List<Ingredient> needed = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = actual(i).copy();
            placed[i] = !TableRecipeGuide.empty(grid[i]) && grid[i].test(stack);
            if (placed[i]) stack.shrink(1);
            resources.add(stack);
            if (!TableRecipeGuide.empty(grid[i]) && !placed[i]) {
                needed.add(grid[i]);
                cells.add(i);
            }
        }
        List<ItemStack> pool = units(resources);
        int[] assignment = match(needed, pool);
        for (int n = 0; n < cells.size(); n++) {
            int cell = cells.get(n);
            missing[cell] = assignment[n] < 0;
            if (missing[cell]) missingCount++;
            // Available tag alternatives are shown literally; unavailable alternatives rotate in the renderer.
            if (!missing[cell]) display[cell] = pool.get(assignment[n]).copyWithCount(1);
        }
    }

    private void layout() {
        if (selected.width > 0) {
            int w = selected.width, h = selected.height;
            Ingredient[] best = shapedLayout(selected, w == 1 ? 1 : 0, h == 1 ? 1 : 0, false);
            int score = score(best);
            if (!gridEmpty() && score(grid) >= score) { best = grid.clone(); score = score(best); }
            for (int y = 0; y <= 3 - h; y++) for (int x = 0; x <= 3 - w; x++) for (int m = 0; m < 2; m++) {
                Ingredient[] candidate = shapedLayout(selected, x, y, m == 1);
                int value = score(candidate);
                if (value > score) { best = candidate; score = value; }
            }
            System.arraycopy(best, 0, grid, 0, 9);
        } else {
            Arrays.fill(grid, null);
            List<ItemStack> table = new ArrayList<>();
            for (int i = 0; i < 9; i++) table.add(actual(i));
            int[] assignment = match(selected.ingredients, table);
            for (int i = 0; i < assignment.length; i++) if (assignment[i] >= 0) grid[assignment[i]] = selected.ingredients.get(i);
            for (int i = 0; i < assignment.length; i++) if (assignment[i] < 0) {
                int cell = -1;
                for (int j = 0; j < 9; j++) if (TableRecipeGuide.empty(grid[j]) && actual(j).isEmpty()) { cell = j; break; }
                if (cell < 0) for (int j = 0; j < 9; j++) if (TableRecipeGuide.empty(grid[j])) { cell = j; break; }
                grid[cell] = selected.ingredients.get(i);
            }
        }
    }

    private boolean gridEmpty() {
        return Arrays.stream(grid).allMatch(TableRecipeGuide::empty);
    }

    private Ingredient[] shapedLayout(Entry recipe, int x, int y, boolean mirror) {
        Ingredient[] result = new Ingredient[9];
        Arrays.fill(result, null);
        for (int row = 0; row < recipe.height; row++) for (int col = 0; col < recipe.width; col++) {
            result[(row + y) * 3 + col + x] = recipe.pattern.get(row * recipe.width
                    + (mirror ? recipe.width - col - 1 : col));
        }
        return result;
    }

    private int score(Ingredient[] candidate) {
        int score = 0;
        for (int i = 0; i < 9; i++) if (!actual(i).isEmpty()) score += candidate[i] != null && candidate[i].test(actual(i)) ? 4 : -1;
        return score;
    }

    /**
     * The moves that lay out the selected recipe: for each empty cell of the diagram that the inventory (or the item in
     * hand) can fill, {world cell, inventory slot or -1}. Cells nothing can fill are left out (they stay see-through).
     */
    List<int[]> layoutMoves() {
        // worked out again only when the inventory, the table or the choice changes ({@link #revision}), not every frame
        if (movesRevision == revision && movesCache != null) {
            return movesCache;
        }
        movesRevision = revision;
        movesCache = workOutMoves();
        return movesCache;
    }

    @Nullable
    private List<int[]> movesCache;
    private int movesRevision = -1;

    private List<int[]> workOutMoves() {
        Minecraft mc = Minecraft.getInstance();
        List<int[]> moves = new ArrayList<>();
        if (selected == null || mc.player == null) {
            return moves;
        }
        List<ItemStack> units = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = -1; slot < 36; slot++) {
            ItemStack stack = slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot);
            for (int i = 0; i < Math.min(9, stack.getCount()); i++) {
                units.add(stack);
                slots.add(slot);
            }
        }
        List<Ingredient> needed = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int logical = 0; logical < 9; logical++) {
            if (!TableRecipeGuide.empty(grid[logical]) && !placed[logical] && actual(logical).isEmpty()) {
                needed.add(grid[logical]);
                cells.add(logical);
            }
        }
        int[] assignment = match(needed, units);
        for (int n = 0; n < cells.size(); n++) {
            if (assignment[n] >= 0) {
                int logical = cells.get(n);
                moves.add(new int[]{TableGrid.worldIndex(station.side, logical / 3, logical % 3), slots.get(assignment[n])});
            }
        }
        return moves;
    }

    ItemStack ghost(int logical) {
        if (!display[logical].isEmpty()) return display[logical];
        ItemStack[] choices = grid[logical] == null ? new ItemStack[0] : grid[logical].items().map(h -> new ItemStack(h.value())).toArray(ItemStack[]::new);
        return choices.length == 0 ? ItemStack.EMPTY : choices[(int) (Scene.clock() / 1.8) % choices.length].copyWithCount(1);
    }

    boolean wrong(int logical) {
        return !actual(logical).isEmpty() && !placed[logical];
    }

    private static List<ItemStack> units(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        // No crafting recipe consumes more than nine units, even if the source stack contains thousands.
        for (ItemStack stack : stacks) for (int i = 0; i < Math.min(9, stack.getCount()); i++) result.add(stack);
        return result;
    }

    /** Maximum matching avoids spending a flexible tag alternative on an ingredient needed elsewhere. */
    static int[] match(List<Ingredient> ingredients, List<ItemStack> units) {
        int[] owner = new int[units.size()];
        Arrays.fill(owner, -1);
        for (int i = 0; i < ingredients.size(); i++) augment(i, ingredients, units, owner, new boolean[units.size()]);
        int[] result = new int[ingredients.size()];
        Arrays.fill(result, -1);
        for (int i = 0; i < owner.length; i++) if (owner[i] >= 0) result[owner[i]] = i;
        return result;
    }

    private static boolean augment(int ingredient, List<Ingredient> ingredients, List<ItemStack> units, int[] owner, boolean[] seen) {
        for (int i = 0; i < units.size(); i++) if (!seen[i] && ingredients.get(ingredient).test(units.get(i))) {
            seen[i] = true;
            if (owner[i] < 0 || augment(owner[i], ingredients, units, owner, seen)) {
                owner[i] = ingredient;
                return true;
            }
        }
        return false;
    }
}
