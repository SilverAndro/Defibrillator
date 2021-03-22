/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.data

import mc.defibrillator.gui.MainScreenHandler
import mc.defibrillator.gui.ScreenHandlerFactory
import mc.defibrillator.util.DynamicLimitedIntProp
import net.minecraft.nbt.AbstractListTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import java.util.concurrent.atomic.AtomicBoolean

class MenuState(val rootTag: CompoundTag) {
    var clickMode: RightClickMode = RightClickMode.PASS
    var keyStack = mutableListOf<String>()
    var page by DynamicLimitedIntProp({ 0 }, { getAvailableKeys().size / PER_PAGE })

    var handler: MainScreenHandler? = null
    var factory: ScreenHandlerFactory? = null
    var suppressOnClose: AtomicBoolean = AtomicBoolean(false)

    fun getActiveTag(): Tag {
        var out: Tag = rootTag
        for (key in keyStack) {
            if (out is CompoundTag) {
                out = out.get(key)!!
            } else if (out is AbstractListTag<*>) {
                out = out[key.toInt()]!!
            }
        }
        return out
    }

    fun getAvailableKeys(): List<String> {
        return when (val active = getActiveTag()) {
            is CompoundTag -> active.keys.toList()
            is AbstractListTag<*> -> (0 until active.size).map { it.toString() }
            else -> emptyList()
        }
    }

    companion object {
        const val PER_PAGE = 9 * 5
    }
}
