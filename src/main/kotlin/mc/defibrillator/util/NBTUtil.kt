/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.nbt.AbstractListTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag

fun Tag.retrieve(key: String): Tag? {
    return when (this) {
        is CompoundTag -> this.get(key)
        is AbstractListTag<*> -> this[key.toInt()]
        else -> null
    }
}

fun Tag.delete(key: String) {
    when (this) {
        is CompoundTag -> this.remove(key)
        is AbstractListTag<*> -> this.removeAt(key.toInt())
    }
}

fun AbstractListTag<*>.wouldAccept(nbtType: Int): Boolean {
    return if (this.elementType == NbtType.END.toByte()) {
        true
    } else {
        this.elementType == nbtType.toByte()
    }
}
