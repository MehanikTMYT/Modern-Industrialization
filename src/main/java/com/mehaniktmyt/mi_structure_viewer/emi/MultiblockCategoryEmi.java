package com.mehaniktmyt.mi_structure_viewer.emi;

import java.util.List;

import com.mehaniktmyt.mi_structure_viewer.MultiblockStructure;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * EMI category for MI multiblock structures. The icon is the item of the first
 * known multiblock machine block (lazily resolved so it works at any registry
 * state), with a vanilla blast furnace fallback.
 */
public class MultiblockCategoryEmi extends EmiRecipeCategory {

    public MultiblockCategoryEmi(Identifier id, List<MultiblockStructure> structures) {
        super(id, icon(structures), icon(structures));
    }

    private static EmiRenderable icon(List<MultiblockStructure> structures) {
        return (draw, x, y, delta) -> iconStack(structures).render(draw, x, y, delta);
    }

    private static EmiStack iconStack(List<MultiblockStructure> structures) {
        for (MultiblockStructure structure : structures) {
            Item item = BuiltInRegistries.ITEM.getValue(structure.machine());
            if (item != Items.AIR) {
                return EmiStack.of(item);
            }
        }
        return EmiStack.of(Items.BLAST_FURNACE);
    }

    @Override
    public Component getName() {
        return Component.translatableWithFallback("emi.category." + getId().getNamespace() + "." + getId().getPath(),
                "Multiblock Structures (MI)");
    }
}
