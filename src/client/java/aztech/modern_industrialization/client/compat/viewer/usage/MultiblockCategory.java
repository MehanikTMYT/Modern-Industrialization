/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package aztech.modern_industrialization.client.compat.viewer.usage;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.MIText;
import aztech.modern_industrialization.client.compat.viewer.abstraction.ViewerCategory;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MultiblockCategory extends ViewerCategory<MultiblockCategory.Recipe> {
    private static final int COLUMNS = 6;
    private static final int ROWS = 2;

    protected MultiblockCategory() {
        super(Recipe.class, MI.id("multiblock_shapes"), MIText.MultiblockMaterials.text(), MIItem.WRENCH.stack(), 34 + (COLUMNS * 18),
                28 + (ROWS * 18));
    }

    @Override
    public void buildWorkstations(WorkstationConsumer consumer) {}

    @Override
    public void buildRecipes(RecipeMap recipeMap, RegistryAccess registryAccess, Consumer<Recipe> consumer) {
        for (ReiMachineRecipes.MultiblockShape entry : ReiMachineRecipes.multiblockShapes) {
            consumer.accept(new Recipe(entry.machine(), entry.shapeTemplate(), entry.alternative(), registryAccess));
        }
    }

    @Override
    public void buildLayout(Recipe recipe, LayoutBuilder builder) {
        builder.invisibleInput(Ingredient.of(recipe.controller.getItem()), 1);
        builder.outputSlot((width / 2) - 8, 5).item(recipe.controller);

        builder.scrollableSlots(COLUMNS, ROWS, recipe.materials);
    }

    @Override
    public void buildWidgets(Recipe recipe, WidgetList widgets) {
        widgets.scrollableSlots(COLUMNS, ROWS, recipe.materials);
    }

    @Override
    public Identifier getRecipeId(Recipe recipe) {
        return recipe.id;
    }

    protected static class Recipe {
        public final ItemStack controller;
        public final List<SizedIngredient> materials = new ArrayList<>();
        public final Identifier id;

        public Recipe(Identifier controller, ShapeTemplate shapeTemplate, @Nullable String alternative, RegistryAccess registries) {
            this.controller = BuiltInRegistries.ITEM.getValue(controller).getDefaultInstance();
            // Upstream #1385 counts into a HashMap; Ingredient equality is kept linear here because 26.1 Ingredient hashing
            // is not guaranteed to agree with equals for tag-backed holder sets.
            List<Ingredient> ingredients = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();

            for (var entry : shapeTemplate.simpleMembers.entrySet()) {
                var previewState = entry.getValue().getItemPreviewState(registries);
                // PORT(26.1): ingredients are never empty; members whose block has no item yield an AIR ingredient.
                if (previewState.items().anyMatch(holder -> holder.value() == Items.AIR)) {
                    continue;
                }
                int index = ingredients.indexOf(previewState);
                if (index >= 0) {
                    counts.set(index, counts.get(index) + 1);
                } else {
                    ingredients.add(previewState);
                    counts.add(1);
                }
            }

            List<SizedIngredient> materials = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                materials.add(new SizedIngredient(ingredients.get(i), counts.get(i)));
            }
            materials.sort(Comparator.comparing(SizedIngredient::count).reversed());
            this.materials.addAll(materials);
            this.id = Identifier.fromNamespaceAndPath(controller.getNamespace(),
                    "/" + controller.getPath() + "/" + materials.size() + (alternative == null ? "" : "/" + alternative));
        }
    }
}
