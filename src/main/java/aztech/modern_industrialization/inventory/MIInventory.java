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

package aztech.modern_industrialization.inventory;

import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.util.NbtHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class MIInventory implements MachineComponent {
    public static final MIInventory EMPTY;

    private @Nullable BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> outputCache;

    public final MIItemStorage itemStorage;
    public final MIFluidStorage fluidStorage;
    public final SlotPositions itemPositions;
    public final SlotPositions fluidPositions;

    /**
     * Build a new MI inventory. If you need to access the stacks, make sure to
     * reference them through this inventory and not directly!
     */
    public MIInventory(List<ConfigurableItemStack> itemStacks, List<ConfigurableFluidStack> fluidStacks, SlotPositions itemPositions,
            SlotPositions fluidPositions) {
        // Must be array lists to allow using .set() in readNbt()
        this.itemStorage = new MIItemStorage(new ArrayList<>(itemStacks));
        this.fluidStorage = new MIFluidStorage(new ArrayList<>(fluidStacks));
        this.itemPositions = itemPositions;
        this.fluidPositions = fluidPositions;
        if (itemPositions.size() != itemStacks.size()) {
            throw new IllegalArgumentException(
                    "Mismatched item slots and positions. Slot count: " + itemStacks.size() + ", position count: " + itemPositions.size());
        }
        if (fluidPositions.size() != fluidStacks.size()) {
            throw new IllegalArgumentException(
                    "Mismatched fluid slots and positions. Slot count: " + fluidStacks.size() + ", position count: " + fluidPositions.size());
        }
    }

    public void addListener(ChangeListener listener, Object token) {
        listener.listenAll(getItemStacks(), token);
        listener.listenAll(getFluidStacks(), token);
    }

    public List<ConfigurableItemStack> getItemStacks() {
        return itemStorage.stacks;
    }

    public List<ConfigurableFluidStack> getFluidStacks() {
        return fluidStorage.stacks;
    }

    // Fleet port (26.1): auto-extract/insert re-implemented on the NeoForge transfer API (upstream left them as
    // TODO stubs). Behaviour matches 1.21.x, including the full-storage early outs from upstream #1251.
    public void autoExtractItems(Level world, BlockPos pos, Direction direction) {
        // The second check is needed in case we change the output side...
        boolean updateCache = outputCache == null || outputCache.context() != direction.getOpposite();

        if (updateCache) {
            outputCache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, (ServerLevel) world, pos.relative(direction),
                    direction.getOpposite());
        }

        var target = outputCache.getCapability();
        if (target != null && !FilledItemStorage.isFull(target)) {
            try (var tx = Transaction.openRoot()) {
                ResourceHandlerUtil.moveStacking(itemStorage, target, r -> true, Integer.MAX_VALUE, tx);
                tx.commit();
            }
        }
    }

    public void autoExtractFluids(Level world, BlockPos pos, Direction direction) {
        ResourceHandler<FluidResource> target = world.getCapability(Capabilities.Fluid.BLOCK, pos.relative(direction), direction.getOpposite());

        if (target != null) {
            try (var tx = Transaction.openRoot()) {
                ResourceHandlerUtil.move(fluidStorage, target, r -> true, Integer.MAX_VALUE, tx);
                tx.commit();
            }
        }
    }

    public void autoInsertItems(Level world, BlockPos pos, Direction direction) {
        ResourceHandler<ItemResource> target = world.getCapability(Capabilities.Item.BLOCK, pos.relative(direction), direction.getOpposite());

        if (target != null && !FilledItemStorage.isFull(itemStorage)) {
            try (var tx = Transaction.openRoot()) {
                ResourceHandlerUtil.move(target, itemStorage, r -> true, Integer.MAX_VALUE, tx);
                tx.commit();
            }
        }
    }

    public void autoInsertFluids(Level world, BlockPos pos, Direction direction) {
        ResourceHandler<FluidResource> target = world.getCapability(Capabilities.Fluid.BLOCK, pos.relative(direction), direction.getOpposite());

        if (target != null) {
            try (var tx = Transaction.openRoot()) {
                ResourceHandlerUtil.move(target, fluidStorage, r -> true, Integer.MAX_VALUE, tx);
                tx.commit();
            }
        }
    }

    public void writeNbt(ValueOutput output) {
        NbtHelper.putList(output, "items", itemStorage.stacks, ConfigurableItemStack.CODEC);
        NbtHelper.putList(output, "fluids", fluidStorage.stacks, ConfigurableFluidStack.CODEC);
    }

    public void readNbt(ValueInput input, boolean isUpgradingMachine) {
        List<ConfigurableItemStack> newItemStacks = new ArrayList<>();
        List<ConfigurableFluidStack> newFluidStacks = new ArrayList<>();

        NbtHelper.getList(input, "items", newItemStacks, ConfigurableItemStack.CODEC);
        NbtHelper.getList(input, "fluids", newFluidStacks, ConfigurableFluidStack.CODEC);

        if (isUpgradingMachine) {
            // Increase fluid slot capacities if upgrading
            for (int i = 0; i < newFluidStacks.size() && i < fluidStorage.stacks.size(); ++i) {
                newFluidStacks.get(i).setCapacity(fluidStorage.stacks.get(i).getCapacity());
            }
        }

        SlotConfig.readSlotList(itemStorage.stacks, newItemStacks);
        SlotConfig.readSlotList(fluidStorage.stacks, newFluidStacks);
    }

    static {
        EMPTY = new MIInventory(Collections.emptyList(), Collections.emptyList(), SlotPositions.empty(), SlotPositions.empty());
    }
}
