package com.mehaniktmyt.mi_structure_viewer;

import java.util.List;
import java.util.Map;

import net.minecraft.world.level.block.state.BlockState;

/**
 * One horizontal slice (fixed Y) of a multiblock, stored as a row-major grid
 * of cells: {@code cells.get(row * cols + col)}, row = Z, col = X.
 */
public record MultiblockLayer(int yOffset, int rows, int cols, Map<Integer, MultiblockCell> cells) {

    public MultiblockCell cell(int row, int col) {
        return cells.get(row * cols + col);
    }
}
