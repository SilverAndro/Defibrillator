/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.AbstractNbtList
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement

/**
 * Gets the value stored at `key`, or null if not present or tag does not support retrieval
 */
fun NbtElement.retrieve(key: String): NbtElement? {
    return when (this) {
        is NbtCompound -> this.get(key)
        is AbstractNbtList<*> -> this[key.toInt()]
        else -> null
    }
}

/**
 * Deletes `key` from the containing tag
 */
fun NbtElement.delete(key: String) {
    when (this) {
        is NbtCompound -> this.remove(key)
        is AbstractNbtList<*> -> this.removeAt(key.toInt())
    }
}

/**
 * @param nbtType An int representing the type of tag
 * @see NbtType
 * @return If the listTag would accept a tag of that type
 */
fun AbstractNbtList<*>.wouldAccept(nbtType: Int): Boolean {
    return if (heldType == NbtType.END.toByte()) {
        true
    } else {
        heldType == nbtType.toByte()
    }
}

fun Entity.toNBT(): NbtCompound {
    return writeNbt(NbtCompound())
}

fun BlockEntity.toNBT(): NbtCompound {
    return writeNbt(NbtCompound())
}

fun ItemStack.toNBT(): NbtCompound {
    return writeNbt(NbtCompound())
}
