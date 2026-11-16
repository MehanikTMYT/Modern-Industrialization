package com.mehaniktmyt.mi_structure_viewer;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Aggregated block requirement for one structure: the preview block state of a
 * member type and how many positions of the template it covers.
 */
public record MultiblockBlockRequirement(BlockState previewState, long count) {}
