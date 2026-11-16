package com.mehaniktmyt.mi_structure_viewer;

import java.util.List;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A single position inside a multiblock layer.
 */
public record MultiblockCell(Kind kind, BlockState previewState, List<String> hatchDescriptions) {

    public enum Kind {
        /** A concrete block position (filled by MI with the preview state of the member). */
        BLOCK,
        /** A position where any matching hatch may be placed. */
        HATCH
    }
}
