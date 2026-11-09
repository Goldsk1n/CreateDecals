package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every recipe of ours crafts from real items in the running game, found as a player's crafting grid or Stonecutter
 * finds it. The recipes name items through shared tags ({@code c:nuggets/brass}, {@code c:plates/iron}, {@code c:dyes},
 * {@code c:ingots/zinc}): an empty tag (one the Create version in use does not fill) makes a recipe nobody can craft,
 * which the content test, checking only that a recipe exists, would not see. Run on {@code latest} and {@code floor}.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeGameTests {
    private RecipeGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyRecipeCrafts(GameTestHelper helper) {
        List<String> checked = new ArrayList<>();
        // Every recipe is tried before failing, so one run names every clash (another mod's recipe for the same items)
        List<String> failures = new ArrayList<>();
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CreateDecals.MOD_ID)) {
                continue;
            }
            Recipe<?> recipe = holder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                ItemStack[] items = ingredient.getItems();
                helper.assertTrue(items.length > 0 && !items[0].is(Items.BARRIER),
                        holder.id() + ": an ingredient matches no item (an empty tag?)");
            }
            ItemStack expected = recipe.getResultItem(helper.getLevel().registryAccess());
            Optional<? extends RecipeHolder<?>> found;
            if (recipe instanceof ShapedRecipe shaped) {
                List<ItemStack> grid = new ArrayList<>();
                for (Ingredient ingredient : shaped.getIngredients()) {
                    grid.add(ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0].copy());
                }
                found = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                        CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid), helper.getLevel());
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                List<ItemStack> items = new ArrayList<>();
                shapeless.getIngredients().forEach(ingredient -> items.add(ingredient.getItems()[0].copy()));
                int width = Math.min(items.size(), 3);
                while (items.size() % width != 0) {
                    items.add(ItemStack.EMPTY);
                }
                found = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                        CraftingInput.of(width, items.size() / width, items), helper.getLevel());
            } else if (recipe instanceof StonecutterRecipe) {
                // A Stonecutter lists every recipe for its input (zinc also makes Create's copycats): ours must be among them
                found = helper.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING,
                        new SingleRecipeInput(recipe.getIngredients().get(0).getItems()[0].copy()), helper.getLevel())
                        .stream().filter(other -> other.id().equals(holder.id())).findFirst();
            } else {
                helper.fail(holder.id() + ": a recipe type this test does not know: " + recipe.getClass().getSimpleName());
                return;
            }
            if (found.isEmpty()) {
                failures.add(holder.id() + ": its own ingredients craft nothing");
                continue;
            }
            ItemStack made = found.get().value().getResultItem(helper.getLevel().registryAccess());
            if (!ItemStack.isSameItem(made, expected) || made.getCount() != expected.getCount()) {
                failures.add(holder.id() + ": its ingredients make " + made + " (" + found.get().id() + "), not " + expected);
                continue;
            }
            checked.add(holder.id().getPath());
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.assertTrue(checked.size() >= 8, "only these recipes were found: " + checked);
        CreateDecals.LOGGER.info("[gametest] recipes craft: {}", checked);
        helper.succeed();
    }
}
