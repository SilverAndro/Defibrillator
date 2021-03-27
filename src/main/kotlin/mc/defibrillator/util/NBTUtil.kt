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

/**
 * Gets the value stored at `key`, or null if not present or tag does not support retrieval
 */
fun Tag.retrieve(key: String): Tag? {
    return when (this) {
        is CompoundTag -> this.get(key)
        is AbstractListTag<*> -> this[key.toInt()]
        else -> null
    }
}

/**
 * Deletes `key` from the containing tag
 */
fun Tag.delete(key: String) {
    when (this) {
        is CompoundTag -> this.remove(key)
        is AbstractListTag<*> -> this.removeAt(key.toInt())
    }
}

/**
 * @param nbtType An int representing the type of tag
 * @see NbtType
 * @return If the listTag would accept a tag of that type
 */
fun AbstractListTag<*>.wouldAccept(nbtType: Int): Boolean {
    return if (this.elementType == NbtType.END.toByte()) {
        true
    } else {
        this.elementType == nbtType.toByte()
    }
}
