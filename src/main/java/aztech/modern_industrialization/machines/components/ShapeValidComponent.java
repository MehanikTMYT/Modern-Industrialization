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

package aztech.modern_industrialization.machines.components;

import aztech.modern_industrialization.machines.MachineComponent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Syncing whether the multiblock shape is currently valid with the clients, to
 * decide if the BER should display something or not.
 */
public class ShapeValidComponent implements MachineComponent.ClientOnly {
    private boolean lastShapeValid = false;
    public boolean shapeValid = false;

    // Used to communicate what block entities are mismatching to the client, since it does not have full block entity awareness like the server does
    private Set<BlockPos> lastMismatchingBlockEntities = new HashSet<>();
    private Set<BlockPos> mismatchingBlockEntities = new HashSet<>();

    public boolean isBlockEntityMatchingAt(BlockPos pos) {
        return !mismatchingBlockEntities.contains(pos);
    }

    public void clearMismatchingBlockEntities() {
        mismatchingBlockEntities.clear();
    }

    public void addMismatchingBlockEntity(BlockPos pos) {
        mismatchingBlockEntities.add(pos);
    }

    /**
     * Return true if this component should be synced with the client.
     */
    public boolean update() {
        if (lastShapeValid != shapeValid || !lastMismatchingBlockEntities.equals(mismatchingBlockEntities)) {
            lastShapeValid = shapeValid;
            lastMismatchingBlockEntities = Set.copyOf(mismatchingBlockEntities);
            return true;
        }
        return false;
    }

    @Override
    public void writeClientNbt(ValueOutput output) {
        output.putBoolean("shapeValid", shapeValid);
        // PORT(26.1): same list as upstream's ListTag of {pos}, written with the BlockPos codec.
        var list = output.list("mismatchingBlockEntities", BlockPos.CODEC);
        for (BlockPos pos : mismatchingBlockEntities) {
            list.add(pos);
        }
    }

    @Override
    public void readClientNbt(ValueInput input) {
        shapeValid = input.getBooleanOr("shapeValid", false);

        mismatchingBlockEntities.clear();
        for (BlockPos pos : input.listOrEmpty("mismatchingBlockEntities", BlockPos.CODEC)) {
            mismatchingBlockEntities.add(pos);
        }
    }
}
