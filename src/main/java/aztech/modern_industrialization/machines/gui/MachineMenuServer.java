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

package aztech.modern_industrialization.machines.gui;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MoveRecipeHandler;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.network.machines.MachineComponentSyncPacket;
import aztech.modern_industrialization.MICommonProxy;
import com.google.common.primitives.Ints;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class MachineMenuServer extends MachineMenuCommon implements MoveRecipeHandler {
    public final MachineBlockEntity blockEntity;
    protected final List<Object> trackedData;

    public MachineMenuServer(int syncId, Inventory playerInventory, MachineBlockEntity blockEntity, MachineGuiParameters guiParams) {
        super(syncId, playerInventory, blockEntity.getInventory(), guiParams, blockEntity.guiComponents);
        this.blockEntity = blockEntity;
        trackedData = new ArrayList<>();
        for (GuiComponentServer component : blockEntity.guiComponents) {
            trackedData.add(component.extractData());
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        blockEntity.guiComponents.forEachIndexed((i, component) -> {
            var newData = component.extractData();
            if (!Objects.equals(trackedData.get(i), newData)) {
                var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), blockEntity.getLevel().registryAccess());
                ((GuiComponentServer.Type<?, Object>) component.getType()).dataCodec().encode(buf, newData);
                byte[] bytes = new byte[buf.writerIndex()];
                buf.readBytes(bytes);
                new MachineComponentSyncPacket(containerId, i, bytes).sendToClient((ServerPlayer) playerInventory.player);
                trackedData.set(i, newData);
                buf.release();
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(blockEntity, player);
    }

    @Override
    public void readClientComponentSyncData(int componentIndex, RegistryFriendlyByteBuf buf) {
        throw new UnsupportedOperationException("Data can only be read on the client side!");
    }

    /**
     * Upstream #1340/#1381 (EMI "fill recipe" into a machine), ported to the 26.1 transfer API: inputs already in the
     * machine count first, then matching items and fluid containers from the player's main inventory are moved in.
     */
    @Override
    public void moveRecipe(Identifier recipeId, int fillAction, int amount) {
        var category = ReiMachineRecipes.categories.get(guiParams.blockId);
        if (category == null) {
            return;
        }
        var world = playerInventory.player.level();
        var recipeHolder = MICommonProxy.INSTANCE.getRecipeMap(world).byType(category.recipeType).stream()
                .filter(r -> r.id().identifier().equals(recipeId)).findFirst().orElse(null);
        if (recipeHolder == null) {
            return;
        }
        var recipe = recipeHolder.value();
        var player = playerInventory.player;
        var machineItemInventory = this.getMachineInventory().itemStorage;
        var machineFluidInventory = this.getMachineInventory().fluidStorage;

        // Handle item inputs
        itemLoop:
        for (var itemInput : recipe.itemInputs) {
            int remainingAmount = itemInput.amount() * amount;
            // Check contents of the machine first
            for (var configurableStack : inventory.getItemStacks()) {
                if (remainingAmount <= 0) {
                    continue itemLoop;
                }
                if (!configurableStack.canPlayerInsert() || configurableStack.getAmount() == 0) {
                    continue;
                }
                var stack = configurableStack.getResource().toStack(configurableStack.getAmount());
                if (itemInput.matches(stack)) {
                    remainingAmount -= stack.getCount();
                }
            }
            // Try to insert from player inventory
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
                if (remainingAmount <= 0) {
                    continue itemLoop;
                }
                var stack = playerInventory.getItem(slot);
                if (stack.isEmpty() || !itemInput.matches(stack)) {
                    continue;
                }
                try (var transaction = Transaction.openRoot()) {
                    int insertedAmount = machineItemInventory.insert(ItemResource.of(stack), Math.min(remainingAmount, stack.getCount()),
                            transaction, ConfigurableItemStack::canPlayerInsert, false);
                    if (insertedAmount > 0) {
                        transaction.commit();
                        stack.shrink(insertedAmount);
                        remainingAmount -= insertedAmount;
                    }
                }
            }
        }

        // Handle fluid inputs
        fluidLoop:
        for (var fluidInput : recipe.fluidInputs) {
            int remainingAmount = Ints.saturatedCast((long) fluidInput.amount() * amount);
            // Check contents of the machine first
            for (var configurableStack : inventory.getFluidStacks()) {
                if (remainingAmount <= 0) {
                    continue fluidLoop;
                }
                if (!configurableStack.canPlayerInsert() || configurableStack.getResource().isEmpty()) {
                    continue;
                }
                if (fluidInput.fluid().test(configurableStack.getResource().toStack(1))) {
                    remainingAmount -= configurableStack.getAmount();
                }
            }
            // Try to insert from fluid containers in player inventory
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
                if (remainingAmount <= 0) {
                    continue fluidLoop;
                }
                if (playerInventory.getItem(slot).isEmpty()) {
                    continue;
                }
                var fluidContainer = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Fluid.ITEM);
                if (fluidContainer == null) {
                    continue;
                }
                for (int tank = 0; tank < fluidContainer.size() && remainingAmount > 0; tank++) {
                    var fluid = fluidContainer.getResource(tank);
                    if (fluid.isEmpty() || !fluidInput.fluid().test(fluid.toStack(1))) {
                        continue;
                    }
                    int accepted;
                    try (var simulation = Transaction.openRoot()) {
                        accepted = machineFluidInventory.insert(fluid, remainingAmount, simulation, ConfigurableFluidStack::canPlayerInsert, false);
                    }
                    if (accepted <= 0) {
                        continue;
                    }
                    try (var transaction = Transaction.openRoot()) {
                        int extracted = fluidContainer.extract(tank, fluid, accepted, transaction);
                        int insertedAmount = extracted <= 0 ? 0
                                : machineFluidInventory.insert(fluid, extracted, transaction, ConfigurableFluidStack::canPlayerInsert, false);
                        if (insertedAmount > 0 && insertedAmount == extracted) {
                            transaction.commit();
                            remainingAmount -= insertedAmount;
                        }
                    }
                }
            }
        }
    }
}
