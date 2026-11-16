package com.mehaniktmyt.mi_structure_viewer;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Immutable snapshot of one registered multiblock shape, converted from MI's
 * {@code ReiMachineRecipes.MultiblockShape} into plain MC types so the client
 * EMI layer never has to touch live template objects.
 */
public record MultiblockStructure(
        Identifier machine,
        String alternative,
        Component name,
        Component variantName,
        Component casingName,
        int sizeX,
        int sizeY,
        int sizeZ,
        List<MultiblockLayer> layers,
        List<MultiblockBlockRequirement> blocks,
        List<Component> hatchTypes) {

    public boolean isEmpty() {
        return layers.isEmpty() || blocks.isEmpty() && hatchTypes.isEmpty();
    }
}
