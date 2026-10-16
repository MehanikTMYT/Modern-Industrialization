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

package aztech.modern_industrialization.items;

import aztech.modern_industrialization.MIComponents;
import aztech.modern_industrialization.blocks.storage.ResourceStorage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public interface ItemContainingItemHelper extends ContainerItem<ItemResource> {
    default boolean handleStackedOnOther(ItemStack stackBarrel, Slot slot, ClickAction clickType, Player player) {
        if (clickType == ClickAction.SECONDARY && slot.allowModification(player)) {
            Mutable<ItemStack> ref = new MutableObject<>(slot.getItem());
            boolean result = handleClick(player, stackBarrel, ref);
            slot.set(ref.getValue());
            return result;
        } else {
            return false;
        }
    }

    default boolean handleOtherStackedOnMe(ItemStack stackBarrel, ItemStack itemStack, Slot slot, ClickAction clickType, Player player,
            SlotAccess cursorStackReference) {
        if (clickType == ClickAction.SECONDARY && slot.allowModification(player)) {
            Mutable<ItemStack> ref = new MutableObject<>(itemStack);
            boolean result = handleClick(player, stackBarrel, ref);
            cursorStackReference.set(ref.getValue());
            slot.set(stackBarrel);
            return result;
        } else {
            return false;
        }
    }

    default boolean handleClick(Player player, ItemStack barrelLike, Mutable<ItemStack> otherStack) {
        if (!(barrelLike.getItem() instanceof ItemContainingItemHelper)) {
            throw new AssertionError("This method should only be called on a ItemContainingItemHelper.");
        }
        if (barrelLike.getCount() != 1) {
            return false;
        }


        // Fleet port (26.1): the 1.21 IItemHandler view of the barrel item is gone (it was left as a null handler, so
        // right-clicking items onto a barrel item crashed). Same semantics, applied directly to the storage component.
        var behaviour = getBehaviour();
        var current = getResource(barrelLike);
        long amount = getAmount(barrelLike);

        // Try to fill barrel with otherStack
        var other = otherStack.getValue();
        if (!other.isEmpty()) {
            var resource = ItemResource.of(other);
            boolean sameOrBlank = current.isEmpty() ? isUnlocked(barrelLike) : current.equals(resource);
            if (sameOrBlank && behaviour.canInsert(resource)) {
                int inserted;
                if (behaviour.isCreative()) {
                    // Creative containers accept and void everything; a blank one takes the resource as its filter.
                    if (current.isEmpty()) {
                        setResource(barrelLike, resource);
                    }
                    inserted = other.getCount();
                } else {
                    long space = behaviour.getCapacityForResource(resource) - (current.isEmpty() ? 0 : amount);
                    inserted = (int) Math.max(0, Math.min(space, other.getCount()));
                    if (inserted > 0) {
                        setResourceNoClean(barrelLike, resource);
                        setAmount(barrelLike, (current.isEmpty() ? 0 : amount) + inserted);
                    }
                }
                if (inserted > 0) {
                    otherStack.setValue(other.copyWithCount(other.getCount() - inserted));
                    return true;
                }
            }
        }

        // Try to empty barrel into otherStack
        current = getResource(barrelLike);
        amount = getAmount(barrelLike);
        if (!current.isEmpty() && amount > 0 && behaviour.canExtract(current)) {
            var held = otherStack.getValue();
            if (held.isEmpty() || current.matches(held)) {
                int extracted = (int) Math.min(amount, current.getMaxStackSize() - held.getCount());
                if (extracted > 0) {
                    if (!behaviour.isCreative()) {
                        setAmount(barrelLike, amount - extracted);
                    }
                    otherStack.setValue(current.toStack(held.getCount() + extracted));
                    return true;
                }
            }
        }

        return !isEmpty(barrelLike) || !otherStack.getValue().isEmpty();
    }

    @Override
    default DataComponentType<ResourceStorage<ItemResource>> getComponentType() {
        return MIComponents.ITEM_STORAGE.get();
    }

    @Override
    default ResourceStorage<ItemResource> getDefaultComponent() {
        return ResourceStorage.ITEM_EMPTY;
    }
}
