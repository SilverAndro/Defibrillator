/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui

import mc.defibrillator.gui.data.GuiStateComposite
import mc.defibrillator.gui.data.MenuState
import mc.defibrillator.gui.inventory.ItemActionMap
import mc.defibrillator.gui.inventory.SimpleDefaultedInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.collection.DefaultedList

class MainScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val inv: SimpleDefaultedInventory,
    var actions: ItemActionMap,
    private val state: MenuState,
    private val onClose: (MenuState) -> Unit
) :
    GenericContainerScreenHandler(
        ScreenHandlerType.GENERIC_9X6,
        syncId,
        playerInventory,
        inv,
        6
    ) {

    init {
        state.handler = this

        addListener(object : ScreenHandlerListener {
            override fun onHandlerRegistered(handlerx: ScreenHandler, stacks: DefaultedList<ItemStack>) {
                sendContentUpdates()
            }

            override fun onSlotUpdate(handlerx: ScreenHandler, slotId: Int, stack: ItemStack) {
                sendContentUpdates()
            }

            override fun onPropertyUpdate(handlerx: ScreenHandler, property: Int, value: Int) {
                sendContentUpdates()
            }
        })
    }

    override fun canInsertIntoSlot(slot: Slot?): Boolean {
        forceSync()
        return false
    }

    override fun insertItem(stack: ItemStack?, startIndex: Int, endIndex: Int, fromLast: Boolean): Boolean {
        forceSync()
        return false
    }

    override fun canInsertIntoSlot(stack: ItemStack?, slot: Slot?): Boolean {
        forceSync()
        return false
    }

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        forceSync()
    }

    override fun onContentChanged(inventory: Inventory) {
        forceSync()
        super.onContentChanged(inventory)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack {
        forceSync()
        return ItemStack.EMPTY
    }

    override fun updateSlotStacks(stacks: MutableList<ItemStack>) {
        forceSync()
    }

    override fun isNotRestricted(player: PlayerEntity?): Boolean {
        forceSync()
        return true
    }

    override fun onSlotClick(slot: Int, data: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        if (actionType == SlotActionType.PICKUP) {
            actions.runActionAt(slot, data, GuiStateComposite(state, playerInventory, inv))
        }

        forceSync()
        return ItemStack.EMPTY
    }

    override fun close(player: PlayerEntity) {
        forceSync()
        if (state.suppressOnClose.get().not()) {
            onClose(state)
        }
    }

    private fun forceSync() {
        sendContentUpdates()
        playerInventory.cursorStack = ItemStack.EMPTY
        playerInventory.updateItems()
        playerInventory.player.playerScreenHandler.onContentChanged(playerInventory)
        (playerInventory.player as ServerPlayerEntity).updateCursorStack()
        playerInventory.remove(
            { stack -> stack.orCreateTag.contains("DELETE") },
            Int.MAX_VALUE,
            playerInventory.player.playerScreenHandler.method_29281()
        )
    }
}
