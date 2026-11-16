package com.mehaniktmyt.mi_structure_viewer.emi;

import com.mehaniktmyt.mi_structure_viewer.StructureIndex;
import com.mehaniktmyt.mi_structure_viewer.MultiblockStructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraft.resources.Identifier;

/**
 * EMI plugin. Runs again on every EMI reindex (including /reload), and always
 * rebuilds the structure snapshot from MI's current registration state, so
 * reload / re-registration is handled by construction.
 */
@EmiEntrypoint
public class StructureViewerEmiPlugin implements EmiPlugin {

    public static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath("mi_structure_viewer", "multiblock");

    @Override
    public void register(EmiRegistry registry) {
        List<MultiblockStructure> structures = new ArrayList<>(StructureIndex.snapshot());
        if (structures.isEmpty()) {
            // Nothing registered yet: no category, no recipes. Avoids a dead tab.
            return;
        }
        structures.sort(Comparator.comparing(s -> s.name().getString().toLowerCase()));

        MultiblockCategoryEmi category = new MultiblockCategoryEmi(CATEGORY_ID, structures);
        registry.addCategory(category);
        for (MultiblockStructure structure : structures) {
            registry.addRecipe(new MultiblockEmiRecipe(category, structure));
        }
    }
}
