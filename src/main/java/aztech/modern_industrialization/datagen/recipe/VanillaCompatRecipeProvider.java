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

package aztech.modern_industrialization.datagen.recipe;

import aztech.modern_industrialization.MIFluids;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.recipe.MachineRecipeBuilder;
import aztech.modern_industrialization.recipe.json.ShapelessRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * Datagen for recipes that produce vanilla materials using MI machines.
 */
public class VanillaCompatRecipeProvider extends BaseRecipeProvider {
    protected VanillaCompatRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    public void buildRecipes() {
        generateCopperOxidation(Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER);
        generateCopperOxidation(Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER);
        generateCopperOxidation(Items.CUT_COPPER_SLAB, Items.EXPOSED_CUT_COPPER_SLAB, Items.WEATHERED_CUT_COPPER_SLAB,
                Items.OXIDIZED_CUT_COPPER_SLAB);
        generateCopperOxidation(Items.CUT_COPPER_STAIRS, Items.EXPOSED_CUT_COPPER_STAIRS, Items.WEATHERED_CUT_COPPER_STAIRS,
                Items.OXIDIZED_CUT_COPPER_STAIRS);
        // Upstream #1372: copper bulbs, doors, trapdoors, chiseled copper and grates.
        generateCopperOxidation(Items.COPPER_BULB, Items.EXPOSED_COPPER_BULB, Items.WEATHERED_COPPER_BULB, Items.OXIDIZED_COPPER_BULB);
        generateCopperOxidation(Items.COPPER_DOOR, Items.EXPOSED_COPPER_DOOR, Items.WEATHERED_COPPER_DOOR, Items.OXIDIZED_COPPER_DOOR);
        generateCopperOxidation(Items.COPPER_TRAPDOOR, Items.EXPOSED_COPPER_TRAPDOOR, Items.WEATHERED_COPPER_TRAPDOOR, Items.OXIDIZED_COPPER_TRAPDOOR);
        generateCopperOxidation(Items.CHISELED_COPPER, Items.EXPOSED_CHISELED_COPPER, Items.WEATHERED_CHISELED_COPPER, Items.OXIDIZED_CHISELED_COPPER);
        generateCopperOxidation(Items.COPPER_GRATE, Items.EXPOSED_COPPER_GRATE, Items.WEATHERED_COPPER_GRATE, Items.OXIDIZED_COPPER_GRATE);
        // waxed variants
        generateCopperOxidation(Items.WAXED_COPPER_BLOCK, Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER,
                Items.WAXED_OXIDIZED_COPPER);
        generateCopperOxidation(Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER,
                Items.WAXED_OXIDIZED_CUT_COPPER);
        generateCopperOxidation(Items.WAXED_CUT_COPPER_SLAB, Items.WAXED_EXPOSED_CUT_COPPER_SLAB, Items.WAXED_WEATHERED_CUT_COPPER_SLAB,
                Items.WAXED_OXIDIZED_CUT_COPPER_SLAB);
        generateCopperOxidation(Items.WAXED_CUT_COPPER_STAIRS, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        generateCopperOxidation(Items.WAXED_COPPER_BULB, Items.WAXED_EXPOSED_COPPER_BULB, Items.WAXED_WEATHERED_COPPER_BULB, Items.WAXED_OXIDIZED_COPPER_BULB);
        generateCopperOxidation(Items.WAXED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_DOOR, Items.WAXED_WEATHERED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_DOOR);
        generateCopperOxidation(Items.WAXED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR, Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
        generateCopperOxidation(Items.WAXED_CHISELED_COPPER, Items.WAXED_EXPOSED_CHISELED_COPPER, Items.WAXED_WEATHERED_CHISELED_COPPER, Items.WAXED_OXIDIZED_CHISELED_COPPER);
        generateCopperOxidation(Items.WAXED_COPPER_GRATE, Items.WAXED_EXPOSED_COPPER_GRATE, Items.WAXED_WEATHERED_COPPER_GRATE, Items.WAXED_OXIDIZED_COPPER_GRATE);

        // wax
        generateWax(Items.COPPER_BLOCK, Items.WAXED_COPPER_BLOCK);
        generateWax(Items.CUT_COPPER, Items.WAXED_CUT_COPPER);
        generateWax(Items.CUT_COPPER_SLAB, Items.WAXED_CUT_COPPER_SLAB);
        generateWax(Items.CUT_COPPER_STAIRS, Items.WAXED_CUT_COPPER_STAIRS);
        generateWax(Items.EXPOSED_COPPER, Items.WAXED_EXPOSED_COPPER);
        generateWax(Items.EXPOSED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER);
        generateWax(Items.EXPOSED_CUT_COPPER_SLAB, Items.WAXED_EXPOSED_CUT_COPPER_SLAB);
        generateWax(Items.EXPOSED_CUT_COPPER_STAIRS, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS);
        generateWax(Items.WEATHERED_COPPER, Items.WAXED_WEATHERED_COPPER);
        generateWax(Items.WEATHERED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER);
        generateWax(Items.WEATHERED_CUT_COPPER_SLAB, Items.WAXED_WEATHERED_CUT_COPPER_SLAB);
        generateWax(Items.WEATHERED_CUT_COPPER_STAIRS, Items.WAXED_WEATHERED_CUT_COPPER_STAIRS);
        generateWax(Items.OXIDIZED_COPPER, Items.WAXED_OXIDIZED_COPPER);
        generateWax(Items.OXIDIZED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER);
        generateWax(Items.OXIDIZED_CUT_COPPER_SLAB, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB);
        generateWax(Items.OXIDIZED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        generateWax(Items.COPPER_BULB, Items.WAXED_COPPER_BULB);
        generateWax(Items.COPPER_DOOR, Items.WAXED_COPPER_DOOR);
        generateWax(Items.COPPER_TRAPDOOR, Items.WAXED_COPPER_TRAPDOOR);
        generateWax(Items.CHISELED_COPPER, Items.WAXED_CHISELED_COPPER);
        generateWax(Items.COPPER_GRATE, Items.WAXED_COPPER_GRATE);
        generateWax(Items.EXPOSED_COPPER_BULB, Items.WAXED_EXPOSED_COPPER_BULB);
        generateWax(Items.EXPOSED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_DOOR);
        generateWax(Items.EXPOSED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR);
        generateWax(Items.EXPOSED_CHISELED_COPPER, Items.WAXED_EXPOSED_CHISELED_COPPER);
        generateWax(Items.EXPOSED_COPPER_GRATE, Items.WAXED_EXPOSED_COPPER_GRATE);
        generateWax(Items.WEATHERED_COPPER_BULB, Items.WAXED_WEATHERED_COPPER_BULB);
        generateWax(Items.WEATHERED_COPPER_DOOR, Items.WAXED_WEATHERED_COPPER_DOOR);
        generateWax(Items.WEATHERED_COPPER_TRAPDOOR, Items.WAXED_WEATHERED_COPPER_TRAPDOOR);
        generateWax(Items.WEATHERED_CHISELED_COPPER, Items.WAXED_WEATHERED_CHISELED_COPPER);
        generateWax(Items.WEATHERED_COPPER_GRATE, Items.WAXED_WEATHERED_COPPER_GRATE);
        generateWax(Items.OXIDIZED_COPPER_BULB, Items.WAXED_OXIDIZED_COPPER_BULB);
        generateWax(Items.OXIDIZED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_DOOR);
        generateWax(Items.OXIDIZED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
        generateWax(Items.OXIDIZED_CHISELED_COPPER, Items.WAXED_OXIDIZED_CHISELED_COPPER);
        generateWax(Items.OXIDIZED_COPPER_GRATE, Items.WAXED_OXIDIZED_COPPER_GRATE);

        // misc recipes
        machine(MIMachineRecipeTypes.MACERATOR, 2, 100)
                .itemIn(Items.STONE, 1)
                .itemOut(Items.COBBLESTONE, 1)
                .offerTo(output, "vanilla_recipes/macerator/stone_to_cobblestone");
        // Upstream #1393
        machine(MIMachineRecipeTypes.COMPRESSOR, 2, 100)
                .itemIn(Items.STRING, 4)
                .itemOut(Items.WHITE_WOOL, 1)
                .offerTo(output, "vanilla_recipes/compressor/string_to_wool");
    }

    private void generateCopperOxidation(Item unaffected, Item exposed, Item weathered, Item oxidized) {
        oxidize(unaffected, exposed);
        oxidize(exposed, weathered);
        oxidize(weathered, oxidized);
    }

    private void oxidize(Item from, Item to) {
        machine(MIMachineRecipeTypes.CHEMICAL_REACTOR, 8, 100)
                .itemIn(from, 1)
                .fluidIn(MIFluids.OXYGEN, 100)
                .itemOut(to, 1)
                .offerTo(output, "vanilla_recipes/oxidation/" + BuiltInRegistries.ITEM.getKey(from).getPath());
    }

    private void generateWax(Item from, Item to) {
        var recipe = ShapelessRecipeBuilder.shapeless(to).requires(from).requires(MIItem.WAX);
        recipe.offerTo(output, "vanilla_recipes/waxing/" + BuiltInRegistries.ITEM.getKey(from).getPath());

        MachineRecipeBuilder chemicalReactorRecipe = machine(MIMachineRecipeTypes.CHEMICAL_REACTOR, 8, 100)
                .itemIn(from, 1)
                .itemIn(MIItem.WAX, 1)
                .itemOut(to, 1);

        chemicalReactorRecipe.offerTo(output, "vanilla_recipes/chemical_reactor/waxing/" + BuiltInRegistries.ITEM.getKey(from).getPath());
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new VanillaCompatRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Vanilla Compat Recipes";
        }
    }
}
