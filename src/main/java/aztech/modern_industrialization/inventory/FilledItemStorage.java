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


public interface FilledItemStorage {
    // Fleet port (26.1): accepts any handler object (ResourceHandler instead of IItemHandler).
    // Upstream #1403: also detect a completely full plain handler by checking its slots.
    @SuppressWarnings("unchecked")
    static boolean isFull(Object handler) {
        if (handler instanceof FilledItemStorage filled) {
            return filled.isFull();
        }
        if (handler instanceof net.neoforged.neoforge.transfer.ResourceHandler<?> rh) {
            var items = (net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>) rh;
            for (int index = 0; index < items.size(); index++) {
                var resource = items.getResource(index);
                // an empty slot is never full (capacity for the EMPTY resource may be reported as 0)
                if (resource.isEmpty() || items.getAmountAsLong(index) < items.getCapacityAsLong(index, resource)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    boolean isFull();
}
