/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.data

import mc.defibrillator.gui.inventory.SimpleDefaultedInventory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.server.network.ServerPlayerEntity

class GuiStateComposite(val state: MenuState, playerInv: PlayerInventory, val defaultedInventory: SimpleDefaultedInventory) {
    val player = playerInv.player as ServerPlayerEntity
}
