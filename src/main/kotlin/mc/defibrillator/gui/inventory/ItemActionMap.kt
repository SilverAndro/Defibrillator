/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.inventory

import mc.defibrillator.gui.data.GuiStateComposite
import mc.defibrillator.gui.util.GuiAction
import mc.defibrillator.util.classes.DualHashMap
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

/**
 * A simple wrapper that indexes `GuiAction`s and ItemStacks into slot numbers + some utility functions
 */
class ItemActionMap(method: ItemActionMap.()->Unit) {
    private val backingMap: DualHashMap<Int, GuiAction, ItemStack> = DualHashMap()

    init {
        this.method()
    }

    fun runActionAt(slotId: Int, data: Int, composite: GuiStateComposite) {
        if (backingMap.contains(slotId)) {
            backingMap[slotId].first.invoke(data, composite)
        }
    }

    fun addEntry(slot: Int, stack: ItemStack, action: GuiAction) {
        backingMap.set(slot, action, stack)
    }

    fun copyIntoInventory(inv: Inventory) {
        backingMap.forEach { slot, _, item -> inv.setStack(slot, item) }
    }
}
