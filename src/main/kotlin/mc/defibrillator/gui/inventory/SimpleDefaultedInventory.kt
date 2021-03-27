/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.inventory

import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

/**
 * A simple inventory that fills all slots with the provided ItemStack
 */
class SimpleDefaultedInventory(size: Int, default: ItemStack): SimpleInventory(size) {
    init {
        for (slot in 0 until size) {
            setStack(slot, default)
        }
    }
}
