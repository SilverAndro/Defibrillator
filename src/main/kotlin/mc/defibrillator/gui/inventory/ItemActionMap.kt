/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.inventory

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import mc.defibrillator.gui.util.GuiAction
import mc.defibrillator.gui.data.GuiStateComposite
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

class ItemActionMap(method: ItemActionMap.()->Unit) {
    private val backingActionMap: Int2ObjectArrayMap<GuiAction> = Int2ObjectArrayMap()
    private val backingStackMap: Int2ObjectArrayMap<ItemStack> = Int2ObjectArrayMap()

    init {
        this.method()
    }

    fun runActionAt(slotId: Int, data: Int, composite: GuiStateComposite) {
        if (backingActionMap.containsKey(slotId)) {
            backingActionMap[slotId].invoke(data, composite)
        }
    }

    fun addEntry(slot: Int, stack: ItemStack, action: GuiAction) {
        backingActionMap[slot] = action
        backingStackMap[slot] = stack
    }

    fun copyIntoInventory(inv: Inventory) {
        backingStackMap.forEach { (slot, item) -> inv.setStack(slot, item) }
    }
}
