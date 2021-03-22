/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.gui.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag

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

fun ItemStack.withGlint(doGlint: Boolean = true): ItemStack {
    if (doGlint) {
        this.orCreateTag.put("Enchantments", ListTag().apply {
            add(CompoundTag())
        })
    }
    return this
}

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

fun ItemStack.asHashtag(): ItemStack {
    this.applySkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ0YzRkZjdlMTdkYjNjN2U5OTZjYzY3YjE3ZThmOGE5N2Q2MmM4MWZlMzJmODUyZTFhNDc3OWE5ZmM1ODhiOCJ9fX0=", listOf(-1704412717,-1610265263,-1967963385,273102471))
    return this
}

fun ItemStack.asEmptyBook(): ItemStack {
    this.orCreateTag.put("pages", ListTag().apply {
        add(StringTag.of(""))
    })
    this.orCreateTag.put("title", StringTag.of(""))
    this.orCreateTag.put("author", StringTag.of(""))
    return this
}
