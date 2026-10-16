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

import aztech.modern_industrialization.blocks.storage.ResourceStorage;
import aztech.modern_industrialization.blocks.storage.StorageBehaviour;
import com.google.common.primitives.Ints;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Interface use for Tanks, Barrels, SteamMiningDrill and other items that can contain a resource.
 */
public interface ContainerItem<T extends Resource> {
    default T getResource(ItemStack stack) {
        return stack.getOrDefault(getComponentType(), getDefaultComponent()).resource();
    }

    default void setResourceNoClean(ItemStack stack, T resource) {
        stack.update(getComponentType(), getDefaultComponent(), resource, ResourceStorage::withResource);
        onChange(stack);
    }

    default void setResource(ItemStack stack, T resource) {
        setResourceNoClean(stack, resource);
        clean(stack);
    }

    default boolean isUnlocked(ItemStack stack) {
        if (getBehaviour().isLockable()) {
            return !stack.getOrDefault(getComponentType(), getDefaultComponent()).locked();
        } else {
            return true;
        }
    }

    default void setLockedNoClean(ItemStack stack, boolean locked) {
        if (getBehaviour().isLockable()) {
            stack.update(getComponentType(), getDefaultComponent(), locked, ResourceStorage::withLocked);
        }
    }

    default void setLocked(ItemStack stack, boolean locked) {
        setLockedNoClean(stack, locked);
        clean(stack);
    }

    default long getAmount(ItemStack stack) {
        var component = stack.getOrDefault(getComponentType(), getDefaultComponent());
        if (component.resource().isEmpty()) {
            return 0;
        }
        if (getBehaviour().isCreative()) {
            return Long.MAX_VALUE;
        } else {
            return component.amount();
        }
    }

    default void setAmountNoClean(ItemStack stack, long amount) {
        if (!getBehaviour().isCreative()) {
            stack.update(getComponentType(), getDefaultComponent(), amount, ResourceStorage::withAmount);
            onChange(stack);
        }
    }

    default void setAmount(ItemStack stack, long amount) {
        setAmountNoClean(stack, amount);
        clean(stack);
    }

    default void clean(ItemStack stack) {
        var component = stack.get(getComponentType());
        if (component != null && !component.locked() && (component.resource().isEmpty() || component.amount() == 0)) {
            stack.remove(getComponentType());
        }
    }

    default boolean isEmpty(ItemStack stack) {
        return stack.getOrDefault(getComponentType(), getDefaultComponent()).amount() == 0;
    }

    default void onChange(ItemStack stack) {}

    StorageBehaviour<T> getBehaviour();

    DataComponentType<ResourceStorage<T>> getComponentType();

    ResourceStorage<T> getDefaultComponent();

    class BaseHandler<T extends Resource> {
        protected final ItemStack stack;
        protected final ContainerItem<T> containerItem;

        public BaseHandler(ItemStack stack, ContainerItem<T> containerItem) {
            this.stack = stack;
            this.containerItem = containerItem;
        }

        public boolean isResourceBlank() {
            return getResource().isEmpty();
        }

        public T getResource() {
            return containerItem.getResource(stack);
        }

        public long getAmount() {
            if (isResourceBlank()) {
                return 0;
            }
            if (!containerItem.getBehaviour().isCreative()) {
                return containerItem.getAmount(stack);
            } else {
                return Long.MAX_VALUE;
            }
        }

        protected void update(T newResource, long newAmount) {
            containerItem.setResourceNoClean(stack, newResource);
            containerItem.setAmountNoClean(stack, newAmount);

            containerItem.clean(stack);
        }
    }

    /**
     * Fleet port (26.1): item capability for container items (barrels, tanks), replacing the 1.21 IItemHandler /
     * IFluidHandlerItem handlers. Same rules: a stack of exactly one container, filter/lock/capacity from the
     * behaviour, creative containers never change. Changes are applied by exchanging the accessed stack for a copy
     * carrying the new storage component, inside the caller's transaction.
     */
    class AccessHandler<T extends Resource> implements ResourceHandler<T> {
        private final ItemAccess access;
        private final ContainerItem<T> containerItem;

        public AccessHandler(ItemAccess access, ContainerItem<T> containerItem) {
            this.access = access;
            this.containerItem = containerItem;
        }

        private ItemStack current() {
            return access.getResource().toStack();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public T getResource(int index) {
            return containerItem.getResource(current());
        }

        @Override
        public long getAmountAsLong(int index) {
            var stack = current();
            return containerItem.getResource(stack).isEmpty() ? 0 : containerItem.getAmount(stack);
        }

        @Override
        public long getCapacityAsLong(int index, T resource) {
            return containerItem.getBehaviour().getCapacityForResource(resource);
        }

        @Override
        public boolean isValid(int index, T resource) {
            return containerItem.getBehaviour().canInsert(resource);
        }

        private boolean commit(ItemStack updated, TransactionContext transaction) {
            return access.exchange(ItemResource.of(updated), 1, transaction) == 1;
        }

        @Override
        public int insert(int index, T resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (index != 0 || access.getAmount() != 1 || containerItem.getBehaviour().isCreative()) {
                return 0;
            }
            var stack = current();
            if (!containerItem.getBehaviour().canInsert(resource)) {
                return 0;
            }
            T stored = containerItem.getResource(stack);
            if (!((stored.isEmpty() && containerItem.isUnlocked(stack)) || stored.equals(resource))) {
                return 0;
            }
            long storedAmount = stored.isEmpty() ? 0 : containerItem.getAmount(stack);
            int inserted = (int) Math.min(amount, containerItem.getBehaviour().getCapacityForResource(resource) - storedAmount);
            if (inserted <= 0) {
                return 0;
            }
            var updated = stack.copy();
            containerItem.setResourceNoClean(updated, resource);
            containerItem.setAmountNoClean(updated, storedAmount + inserted);
            containerItem.clean(updated);
            return commit(updated, transaction) ? inserted : 0;
        }

        @Override
        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (index != 0 || access.getAmount() != 1) {
                return 0;
            }
            var stack = current();
            T stored = containerItem.getResource(stack);
            if (stored.isEmpty() || !stored.equals(resource) || !containerItem.getBehaviour().canExtract(resource)) {
                return 0;
            }
            if (containerItem.getBehaviour().isCreative()) {
                return amount;
            }
            long storedAmount = containerItem.getAmount(stack);
            int extracted = (int) Math.min(amount, storedAmount);
            if (extracted <= 0) {
                return 0;
            }
            var updated = stack.copy();
            containerItem.setAmountNoClean(updated, storedAmount - extracted);
            containerItem.clean(updated);
            return commit(updated, transaction) ? extracted : 0;
        }
    }
}
