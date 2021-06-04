/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.data

import mc.defibrillator.gui.NBTScreenHandlerFactory
import mc.defibrillator.util.classes.DynamicLimitedIntProp
import net.minecraft.nbt.AbstractNbtList
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The state of the gui, should be passed around instead of copied or duplicated
 */
class NBTMenuState(var rootTag: NbtCompound, val playerUUID: UUID, val player: ServerPlayerEntity) {
    var clickMode: RightClickMode = RightClickMode.PASS
    var keyStack = mutableListOf<String>()
    var page by DynamicLimitedIntProp({ 0 }, { getAvailableKeys().size / PER_PAGE })
    var isInAddMenu = false

    var factory: NBTScreenHandlerFactory? = null
    var suppressOnClose: AtomicBoolean = AtomicBoolean(false)

    fun getActiveTag(): NbtElement {
        var out: NbtElement = rootTag
        for (key in keyStack) {
            if (out is NbtCompound) {
                out = out.get(key)!!
            } else if (out is AbstractNbtList<*>) {
                out = out[key.toInt()]!!
            }
        }
        return out
    }

    fun getAvailableKeys(): List<String> {
        return when (val active = getActiveTag()) {
            is NbtCompound -> active.keys.toList()
            is AbstractNbtList<*> -> (0 until active.size).map { it.toString() }
            else -> emptyList()
        }
    }

    companion object {
        const val PER_PAGE = 9 * 5
    }
}
