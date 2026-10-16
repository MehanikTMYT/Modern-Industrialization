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
package aztech.modern_industrialization.util;

import aztech.modern_industrialization.MICommonProxy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/**
 * Fleet port (26.1): {@code ItemStack#getBurnTime} needs the {@link FuelValues} of a level/server. The 26.1 port stubbed
 * every call site to 0 (item-fuel boilers and generators produced nothing, the steam drill accepted no fuel).
 */
public final class MIFuel {
    private MIFuel() {
    }

    public static @Nullable FuelValues fuelValues() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.fuelValues();
        }
        var player = MICommonProxy.INSTANCE.getClientPlayer();
        return player == null ? null : player.level().fuelValues();
    }

    public static int burnTime(ItemStack stack) {
        var fuelValues = fuelValues();
        return fuelValues == null || stack.isEmpty() ? 0 : stack.getBurnTime(null, fuelValues);
    }
}
