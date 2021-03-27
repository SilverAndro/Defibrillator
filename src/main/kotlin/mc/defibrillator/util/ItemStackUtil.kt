/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag

/**
 * Adds each entry in loreLines as plain text to the tooltip
 *
 * Automatically adds an extra line break at the begining
 */
fun ItemStack.withLore(loreLines: List<String>): ItemStack {
    val display = (this.orCreateTag.get("display") as CompoundTag?) ?: CompoundTag()
    display.put("Lore", ListTag().apply {
        add(StringTag.of("{\"text\":\"\"}"))
        for (line in loreLines) {
            add(StringTag.of("{\"text\":\"$line\",\"color\":\"white\",\"italic\":false}"))
        }
    })
    this.orCreateTag.put("display", display)
    return this
}

/**
 * Puts an empty Enchantments tag on the itemstack (replacing all enchantments)
 */
fun ItemStack.withGlint(doGlint: Boolean = true): ItemStack {
    if (doGlint) {
        this.orCreateTag.put("Enchantments", ListTag().apply {
            add(CompoundTag())
        })
    }
    return this
}

/**
 * Applies the skull data to the ItemStack
 */
fun ItemStack.applySkull(data: String, uuid: List<Int>): ItemStack {
    orCreateTag.put("SkullOwner", CompoundTag().apply {
        put("Id", IntArrayTag(uuid))
        put("Properties", CompoundTag().apply {
            put("textures", ListTag().apply {
                add(CompoundTag().apply {
                    putString("Value", data)
                })
            })
        })
    })
    return this
}

/**
 * applySkull but hardcoded to the hashtag
 */
fun ItemStack.asHashtag(): ItemStack {
    this.applySkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ0YzRkZjdlMTdkYjNjN2U5OTZjYzY3YjE3ZThmOGE5N2Q2MmM4MWZlMzJmODUyZTFhNDc3OWE5ZmM1ODhiOCJ9fX0=", listOf(-1704412717,-1610265263,-1967963385,273102471))
    return this
}
